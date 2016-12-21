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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class SupportConstants {
    public final static String DEV_BOOTSTRAP_SERVER = "127.0.0.1:9092";
    public final static String DEV_TOPIC_SUPPORTBEAN_STRING = "esperio_regression_input_t1";
    public final static String DEV_TOPIC_SUPPORTBEAN_JAVASERIALIZED = "esperio_regression_input_t2";
    public final static String DEV_TOPIC_BYTES = "esperio_regression_input_t3";

    public static Properties getPluginProps(String topicName, String valueDeserializerClassName, String timestampExtractorClassName) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_BOOTSTRAP_SERVER);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClassName);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, topicName + "__mygroup");
        props.put(EsperIOKafkaConfig.ESPERIO_SUBSCRIBER_CONFIG, EsperIOKafkaInputSubscriberByTopicList.class.getName());
        props.put(EsperIOKafkaConfig.ESPERIO_TOPICS_CONFIG, topicName);
        props.put(EsperIOKafkaConfig.ESPERIO_PROCESSOR_CONFIG, EsperIOKafkaInputProcessorDefault.class.getName());
        if (timestampExtractorClassName != null) {
            props.put(EsperIOKafkaConfig.ESPERIO_TIMESTAMPEXTRACTOR_CONFIG, timestampExtractorClassName);
        }
        return props;
    }

    public static EPServiceProvider getEngine(String uri, Properties pluginProperties) {
        Configuration engineConfig = new Configuration();
        engineConfig.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        engineConfig.addPluginLoader(EsperIOKafkaInputAdapterPlugin.class.getSimpleName(), EsperIOKafkaInputAdapterPlugin.class.getName(), pluginProperties, null);
        return EPServiceProviderManager.getProvider(uri, engineConfig);
    }

    public static Properties getProducerProps(String valueSerializerClassName) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_BOOTSTRAP_SERVER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClassName);
        return props;
    }
}
