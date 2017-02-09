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

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.espertech.esperio.kafka.EsperIOKafkaConfig.INPUT_PROCESSOR_CONFIG;
import static com.espertech.esperio.kafka.EsperIOKafkaConfig.INPUT_SUBSCRIBER_CONFIG;

public class EsperIOKafkaInputAdapter {
    private static final Logger log = LoggerFactory.getLogger(EsperIOKafkaInputAdapter.class);

    private final Properties properties;
    private final String engineURI;

    private KafkaConsumer consumer;
    private ExecutorService executorService;
    private EsperIOKafkaInputRunnable runnable;
    private EsperIOKafkaInputProcessor processor;

    public EsperIOKafkaInputAdapter(Properties properties, String engineURI) {
        this.properties = properties;
        this.engineURI = engineURI;
    }

    public void start() {

        if (log.isInfoEnabled()) {
            log.info("Starting EsperIO Kafka Input Adapter for engine URI '{}'", engineURI);
        }

        // Obtain Kafka consumer properties from provided and excluding esperio
        Properties consumerProperties = new Properties();
        for (String propertyName : properties.stringPropertyNames()) {
            if (!propertyName.startsWith("esperio")) {
                consumerProperties.put(propertyName, properties.getProperty(propertyName));
            }
        }

        // Obtain Kafka consumer
        try {
            consumer = new KafkaConsumer<>(consumerProperties);
        } catch (Throwable t) {
            log.error("Error obtaining Kafka consumer for URI '{}': {}", engineURI, t.getMessage(), t);
        }

        // Obtain engine
        EPServiceProviderSPI engine = (EPServiceProviderSPI) EPServiceProviderManager.getProvider(engineURI);

        // Obtain and invoke subscriber
        String subscriberClassName = getRequiredProperty(properties, INPUT_SUBSCRIBER_CONFIG);
        EsperIOKafkaInputSubscriber subscriber;
        try {
            subscriber = (EsperIOKafkaInputSubscriber) JavaClassHelper.instantiate(EsperIOKafkaInputSubscriber.class, subscriberClassName, engine.getEngineImportService().getClassForNameProvider());
            EsperIOKafkaInputSubscriberContext subscriberContext = new EsperIOKafkaInputSubscriberContext(consumer, engine, properties);
            subscriber.subscribe(subscriberContext);
        } catch (Throwable t) {
            throw new ConfigurationException("Unexpected exception invoking subscriber subscribe method on class " + subscriberClassName + " for engine URI '" + engineURI + "': " + t.getMessage(), t);
        }

        // Obtain and initialize processor
        String processorClassName = getRequiredProperty(properties, INPUT_PROCESSOR_CONFIG);
        try {
            processor = (EsperIOKafkaInputProcessor) JavaClassHelper.instantiate(EsperIOKafkaInputProcessor.class, processorClassName, engine.getEngineImportService().getClassForNameProvider());
            EsperIOKafkaInputProcessorContext processorContext = new EsperIOKafkaInputProcessorContext(consumer, engine, properties, this);
            processor.init(processorContext);
        } catch (Throwable t) {
            throw new ConfigurationException("Unexpected exception invoking processor init method on class " + processorClassName + " for engine URI '" + engineURI + "': " + t.getMessage(), t);
        }

        // Start executor
        executorService = Executors.newFixedThreadPool(1, new EsperIOKafkaInputThreadFactory(engineURI));

        // Submit runnable
        runnable = new EsperIOKafkaInputRunnable(consumer, processor);
        executorService.submit(runnable);

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO Kafka Input Adapter for engine URI '{}'", engineURI);
        }
    }

    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying Esper Kafka Input Adapter for engine URI '{}'", engineURI);
        }

        runnable.setShutdown(true);

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        processor.close();

        consumer.close();
    }

    protected static String getRequiredProperty(Properties properties, String config) {
        String value = properties.getProperty(config);
        if (value == null) {
            throw new ConfigurationException("Property '" + config + "' not provided");
        }
        return value;
    }
}
