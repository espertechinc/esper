/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esperio.kafka;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationPluginLoader;
import com.espertech.esper.util.FileUtil;
import junit.framework.TestCase;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.net.URL;
import java.util.Properties;

import static com.espertech.esperio.kafka.SupportConstants.DEV_BOOTSTRAP_SERVER;

public class TestKafkaInputConfig extends TestCase {
    public void test() {
        Properties props = new Properties();

// Kafka Consumer Properties
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, com.mycompany.MyCustomDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my_group_id");

// EsperIO Kafka Input Adapter Properties
        props.put(EsperIOKafkaConfig.ESPERIO_SUBSCRIBER_CONFIG, EsperIOKafkaInputSubscriberByTopicList.class.getName());
        props.put(EsperIOKafkaConfig.ESPERIO_TOPICS_CONFIG, "my_topic");
        props.put(EsperIOKafkaConfig.ESPERIO_PROCESSOR_CONFIG, EsperIOKafkaInputProcessorDefault.class.getName());
        props.put(EsperIOKafkaConfig.ESPERIO_TIMESTAMPEXTRACTOR_CONFIG, EsperIOKafkaInputTimestampExtractorConsumerRecord.class.getName());

        Configuration config = new Configuration();
        config.addPluginLoader("KafkaInput", EsperIOKafkaInputAdapterPlugin.class.getName(), props, null);

        // start adapter
        EsperIOKafkaInputAdapter adapter = new EsperIOKafkaInputAdapter(props, "engineURI");
        adapter.start();

// destroy the adapter when done
        httpAdapter.destroy();
    }

    public void testIt() {
        URL url = FileUtil.class.getClassLoader().getResource("esper-kafka-sample-config.xml");
        assertNotNull("Failed to find sample config file", url);

        Configuration configuration = new Configuration();
        configuration.configure(url);

        ConfigurationPluginLoader config = configuration.getPluginLoaders().get(0);
        assertEquals(EsperIOKafkaInputAdapterPlugin.class.getName(), config.getClassName());

        Properties props = config.getConfigProperties();
        assertEquals(DEV_BOOTSTRAP_SERVER, props.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(org.apache.kafka.common.serialization.StringDeserializer.class.getName(), props.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals("com.mycompany.MyCustomDeserializer", props.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("my_group_id", props.getProperty(ConsumerConfig.GROUP_ID_CONFIG));

        assertEquals("my_topic", props.get(EsperIOKafkaConfig.ESPERIO_TOPICS_CONFIG));
        assertEquals(EsperIOKafkaInputProcessorDefault.class.getName(), props.get(EsperIOKafkaConfig.ESPERIO_PROCESSOR_CONFIG));
        assertEquals(EsperIOKafkaInputSubscriberByTopicList.class.getName(), props.get(EsperIOKafkaConfig.ESPERIO_SUBSCRIBER_CONFIG));
        assertEquals(EsperIOKafkaInputTimestampExtractorConsumerRecord.class.getName(), props.get(EsperIOKafkaConfig.ESPERIO_TIMESTAMPEXTRACTOR_CONFIG));
    }
}
