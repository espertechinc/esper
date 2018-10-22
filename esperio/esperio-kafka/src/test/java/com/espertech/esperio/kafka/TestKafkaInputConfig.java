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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimePluginLoader;
import com.espertech.esper.common.internal.util.FileUtil;
import junit.framework.TestCase;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.net.URL;
import java.util.Properties;

import static com.espertech.esperio.kafka.SupportConstants.DEV_BOOTSTRAP_SERVER;

public class TestKafkaInputConfig extends TestCase {
    public void testIt() {
        URL url = FileUtil.class.getClassLoader().getResource("esper-kafka-sample-config.xml");
        assertNotNull("Failed to find sample config file", url);

        Configuration configuration = new Configuration();
        configuration.configure(url);

        ConfigurationRuntimePluginLoader config = configuration.getRuntime().getPluginLoaders().get(0);
        assertEquals(EsperIOKafkaInputAdapterPlugin.class.getName(), config.getClassName());

        Properties props = config.getConfigProperties();
        assertEquals(DEV_BOOTSTRAP_SERVER, props.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(org.apache.kafka.common.serialization.StringDeserializer.class.getName(), props.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals("com.mycompany.MyCustomDeserializer", props.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("my_group_id", props.getProperty(ConsumerConfig.GROUP_ID_CONFIG));

        assertEquals("my_topic", props.get(EsperIOKafkaConfig.TOPICS_CONFIG));
        assertEquals(EsperIOKafkaInputProcessorDefault.class.getName(), props.get(EsperIOKafkaConfig.INPUT_PROCESSOR_CONFIG));
        assertEquals(EsperIOKafkaInputSubscriberByTopicList.class.getName(), props.get(EsperIOKafkaConfig.INPUT_SUBSCRIBER_CONFIG));
        assertEquals(EsperIOKafkaInputTimestampExtractorConsumerRecord.class.getName(), props.get(EsperIOKafkaConfig.INPUT_TIMESTAMPEXTRACTOR_CONFIG));
    }
}
