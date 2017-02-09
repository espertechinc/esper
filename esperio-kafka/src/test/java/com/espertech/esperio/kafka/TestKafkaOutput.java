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
import junit.framework.TestCase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.espertech.esperio.kafka.SupportConstants.DEV_BOOTSTRAP_SERVER;
import static com.espertech.esperio.kafka.SupportConstants.DEV_OUTPUT_TOPIC_JSON;

public class TestKafkaOutput extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TestKafkaOutput.class);

    public void testOutput() {
        Properties adapterProps = SupportConstants.getOutputPluginProps();

        adapterProps.put(EsperIOKafkaConfig.TOPICS_CONFIG, DEV_OUTPUT_TOPIC_JSON);
        adapterProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        adapterProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        adapterProps.put(EsperIOKafkaConfig.OUTPUT_FLOWCONTROLLER_CONFIG, EsperIOKafkaOutputFlowControllerByAnnotatedStmt.class.getName());
        EPServiceProvider epService = SupportConstants.getEngineWKafkaOutput(this.getClass().getSimpleName(), adapterProps);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // create statement
        EPStatement statement = epService.getEPAdministrator().createEPL("@name('first') @KafkaOutputDefault select * from SupportBean");

        // get consumer for asserting message
        KafkaConsumer<String, String> consumer = initConsumer();
        Collection<TopicPartition> topicPartitions = Collections.singletonList(new TopicPartition(DEV_OUTPUT_TOPIC_JSON, 0));
        consumer.assign(topicPartitions);
        int numMessages = countMessages(topicPartitions, consumer);
        consumer.seek(topicPartitions.iterator().next(), 0);

        // send-and-await two events
        sendAndAwait(epService, consumer, "E1");
        sendAndAwait(epService, consumer, "E2");

        // destroy and check nothing received
        int numMessagesAfter = countMessages(topicPartitions, consumer);
        statement.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean("XXX", -1));
        assertEquals(2, numMessagesAfter - numMessages);

        // create second statement
        epService.getEPAdministrator().createEPL("@name('second') @KafkaOutputDefault select stringProp from SupportBean");

        // send-and-await another event
        sendAndAwait(epService, consumer, "E3");

        // destroy engine
        epService.destroy();
    }

    private int countMessages(Collection<TopicPartition> topicPartitions, KafkaConsumer<String, String> consumer) {
        consumer.seek(topicPartitions.iterator().next(), 0);
        int count = 0;
        boolean more = true;
        while (more) {
            ConsumerRecords<String, String> rows = consumer.poll(1000);
            count += rows.count();
            more = !rows.isEmpty();
        }
        return count;
    }

    private void sendAndAwait(EPServiceProvider epService, KafkaConsumer<String, String> consumer, String eventId) {
        // send event
        String uniqueId = eventId + "___" + UUID.randomUUID().toString();
        epService.getEPRuntime().sendEvent(new SupportBean(uniqueId, 10));

        // await
        SupportAwaitUtil.awaitOrFail(10, TimeUnit.SECONDS, "failed to receive expected event", (Supplier<Object>) () -> {
            ConsumerRecords<String, String> rows = consumer.poll(1000);
            Iterator<ConsumerRecord<String, String>> it = rows.iterator();
            boolean found = false;
            while (it.hasNext()) {
                ConsumerRecord<String, String> row = it.next();
                log.info("Received: {}", row.value());
                if (row.value().contains(uniqueId)) {
                    found = true;
                }
            }
            return found ? true : null;
        });
    }

    private KafkaConsumer<String, String> initConsumer() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_BOOTSTRAP_SERVER);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, DEV_OUTPUT_TOPIC_JSON + "__mygroup");
        return new KafkaConsumer<>(consumerProps);
    }
}
