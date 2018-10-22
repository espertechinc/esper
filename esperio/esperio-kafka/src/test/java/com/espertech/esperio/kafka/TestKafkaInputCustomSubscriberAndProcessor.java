/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esperio.kafka;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import junit.framework.TestCase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.espertech.esperio.kafka.SupportCompileUtil.compileDeploy;
import static com.espertech.esperio.kafka.SupportConstants.DEV_INPUT_TOPIC_BYTES;

public class TestKafkaInputCustomSubscriberAndProcessor extends TestCase {

    private static final String TOPIC = DEV_INPUT_TOPIC_BYTES;

    public void testInput() {

        Properties pluginProperties = SupportConstants.getInputPluginProps(TOPIC, ByteArrayDeserializer.class.getName(), null);
        pluginProperties.put(EsperIOKafkaConfig.INPUT_PROCESSOR_CONFIG, SupportInputCustomProcessor.class.getName());

        EPRuntime runtime = SupportConstants.getEngineWKafkaInput(this.getClass().getSimpleName(), pluginProperties);

        EPStatement stmt = compileDeploy(runtime, "select * from SupportBean").getStatements()[0];
        SupportListener listener = new SupportListener();
        stmt.addListener(listener);

        Properties producerProperties = SupportConstants.getProducerProps(org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProperties);
        int randomNumber = (int) (Math.random() * 100000000);
        byte[] bytes = ByteBuffer.allocate(4).putInt(randomNumber).array();
        producer.send(new ProducerRecord<>(TOPIC, bytes));

        SupportAwaitUtil.awaitOrFail(10, TimeUnit.SECONDS, "failed to receive expected event", (Supplier<Object>) () -> {
            for (EventBean[] events : listener.getEvents()) {
                for (EventBean event : events) {
                    SupportBean bean = (SupportBean) event.getUnderlying();
                    if (bean.getIntProp() == randomNumber) {
                        return true;
                    }
                }
            }
            return null;
        });

        producer.close();
        runtime.destroy();

        assertTrue(SupportInputCustomProcessor.isClosed());
        assertNotNull(SupportInputCustomProcessor.getContext().getAdapter());
        assertNotNull(SupportInputCustomProcessor.getContext().getRuntime());
        assertNotNull(SupportInputCustomProcessor.getContext().getProperties());
        assertNotNull(SupportInputCustomProcessor.getContext().getConsumer());
    }
}
