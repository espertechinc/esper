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

import com.espertech.esper.runtime.client.EPRuntime;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

public class EsperIOKafkaInputSubscriberContext {
    private final KafkaConsumer consumer;
    private final EPRuntime runtime;
    private final Properties properties;

    public EsperIOKafkaInputSubscriberContext(KafkaConsumer consumer, EPRuntime runtime, Properties properties) {
        this.consumer = consumer;
        this.runtime = runtime;
        this.properties = properties;
    }

    public KafkaConsumer getConsumer() {
        return consumer;
    }

    public EPRuntime getRuntime() {
        return runtime;
    }

    public Properties getProperties() {
        return properties;
    }
}
