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
import com.espertech.esper.common.internal.util.UuidGenerator;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class SupportConstants {
    public final static String DEV_BOOTSTRAP_SERVER = "localhost:9092";
    public final static String DEV_INPUT_TOPIC_SUPPORTBEAN_STRING = "esperio_regression_input_t1";
    public final static String DEV_INPUT_TOPIC_SUPPORTBEAN_JAVASERIALIZED = "esperio_regression_input_t2";
    public final static String DEV_INPUT_TOPIC_BYTES = "esperio_regression_input_t3";
    public final static String DEV_INPUT_TOPIC_JSON = "esperio_regression_input_t4";
    public final static String DEV_OUTPUT_TOPIC_JSON = "esperio_regression_output_t1";

    public static Properties getInputPluginProps(String topicName, String valueDeserializerClassName, String timestampExtractorClassName) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_BOOTSTRAP_SERVER);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClassName);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, topicName + "__mygroup");
        props.put(EsperIOKafkaConfig.INPUT_SUBSCRIBER_CONFIG, EsperIOKafkaInputSubscriberByTopicList.class.getName());
        props.put(EsperIOKafkaConfig.TOPICS_CONFIG, topicName);
        props.put(EsperIOKafkaConfig.INPUT_PROCESSOR_CONFIG, EsperIOKafkaInputProcessorDefault.class.getName());
        if (timestampExtractorClassName != null) {
            props.put(EsperIOKafkaConfig.INPUT_TIMESTAMPEXTRACTOR_CONFIG, timestampExtractorClassName);
        }
        return props;
    }

    public static Properties getOutputPluginProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_BOOTSTRAP_SERVER);
        return props;
    }

    public static EPRuntime getEngineWKafkaInput(String uri, Properties pluginProperties) {
        Configuration configuration = new Configuration();
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getRuntime().addPluginLoader(EsperIOKafkaInputAdapterPlugin.class.getSimpleName(), EsperIOKafkaInputAdapterPlugin.class.getName(), pluginProperties, null);
        configuration.getCommon().addEventType(SupportBean.class);
        return EPRuntimeProvider.getRuntime(uri, configuration);
    }

    public static EPRuntime getEngineWKafkaOutput(String uri, Properties pluginProperties) {
        Configuration configuration = new Configuration();
        configuration.getCommon().addImport(KafkaOutputDefault.class);
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getRuntime().addPluginLoader(EsperIOKafkaOutputAdapterPlugin.class.getSimpleName(), EsperIOKafkaOutputAdapterPlugin.class.getName(), pluginProperties, null);
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        return EPRuntimeProvider.getRuntime(uri, configuration);
    }

    public static Properties getProducerProps(String valueSerializerClassName) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_BOOTSTRAP_SERVER);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, SupportConstants.class.getName() + "-" + UuidGenerator.generate());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClassName);
        return props;
    }
}
