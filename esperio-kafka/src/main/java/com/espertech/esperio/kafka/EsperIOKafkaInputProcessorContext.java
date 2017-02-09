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

import com.espertech.esper.core.service.EPServiceProviderSPI;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

public class EsperIOKafkaInputProcessorContext {
    private final KafkaConsumer consumer;
    private final EPServiceProviderSPI engine;
    private final Properties properties;
    private final EsperIOKafkaInputAdapter adapter;

    public EsperIOKafkaInputProcessorContext(KafkaConsumer consumer, EPServiceProviderSPI engine, Properties properties, EsperIOKafkaInputAdapter adapter) {
        this.consumer = consumer;
        this.engine = engine;
        this.properties = properties;
        this.adapter = adapter;
    }

    public KafkaConsumer getConsumer() {
        return consumer;
    }

    public EPServiceProviderSPI getEngine() {
        return engine;
    }

    public Properties getProperties() {
        return properties;
    }

    public EsperIOKafkaInputAdapter getAdapter() {
        return adapter;
    }
}
