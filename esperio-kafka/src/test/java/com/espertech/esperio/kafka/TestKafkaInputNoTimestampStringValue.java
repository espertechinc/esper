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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import junit.framework.TestCase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TestKafkaInputNoTimestampStringValue extends TestCase {

    private static final String TOPIC = SupportConstants.DEV_INPUT_TOPIC_SUPPORTBEAN_STRING;

    public void testInput() {
        Properties pluginProperties = SupportConstants.getInputPluginProps(TOPIC, SupportBeanFromStringDeserializer.class.getName(), null);

        EPServiceProvider epService = SupportConstants.getEngineWKafkaInput(this.getClass().getSimpleName(), pluginProperties);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean");
        SupportListener listener = new SupportListener();
        stmt.addListener(listener);

        Properties producerProperties = SupportConstants.getProducerProps(org.apache.kafka.common.serialization.StringSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);

        // send 10 messages
        for (int i = 0; i < 10; i++) {
            String generatedUUID = UUID.randomUUID().toString();
            producer.send(new ProducerRecord<>(TOPIC, generatedUUID));

            SupportAwaitUtil.awaitOrFail(10, TimeUnit.SECONDS, "failed to receive expected event", (Supplier<Object>) () -> {
                for (EventBean[] events : listener.getEvents()) {
                    for (EventBean event : events) {
                        SupportBean bean = (SupportBean) event.getUnderlying();
                        if (bean.getStringProp().equals(generatedUUID)) {
                            return true;
                        }
                    }
                }
                return null;
            });
        }

        producer.close();
        epService.destroy();
    }
}
