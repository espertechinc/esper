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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import junit.framework.TestCase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.espertech.esperio.kafka.SupportCompileUtil.compileDeploy;

public class TestKafkaInputJson extends TestCase {

    private static final String TOPIC = SupportConstants.DEV_INPUT_TOPIC_JSON;

    public void testInput() {
        Properties pluginProperties = SupportConstants.getInputPluginProps(TOPIC, StringDeserializer.class.getName(), null);
        pluginProperties.setProperty(EsperIOKafkaConfig.INPUT_PROCESSOR_CONFIG, EsperIOKafkaInputProcessorJson.class.getName());
        pluginProperties.setProperty(EsperIOKafkaConfig.INPUT_EVENTTYPENAME, "MyEvent");

        EPRuntime runtime = SupportConstants.getEngineWKafkaInput(this.getClass().getSimpleName(), pluginProperties);

        compileDeploy(runtime, "@buseventtype @public create json schema MyEvent(p0 string)");
        EPStatement stmt = compileDeploy(runtime, "select * from MyEvent").getStatements()[0];
        SupportListener listener = new SupportListener();
        stmt.addListener(listener);

        Properties producerProperties = SupportConstants.getProducerProps(org.apache.kafka.common.serialization.StringSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);

        // send 10 messages
        for (int i = 0; i < 10; i++) {
            String generatedUUID = UUID.randomUUID().toString();
            JsonObject json = new JsonObject().add("p0", generatedUUID);
            producer.send(new ProducerRecord<>(TOPIC, json.toString()));

            SupportAwaitUtil.awaitOrFail(10, TimeUnit.SECONDS, "failed to receive expected event", (Supplier<Object>) () -> {
                for (EventBean[] events : listener.getEvents()) {
                    for (EventBean event : events) {
                        if (event.get("p0").equals(generatedUUID)) {
                            return true;
                        }
                    }
                }
                return null;
            });
        }

        producer.close();
        runtime.destroy();
    }
}
