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

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class EsperIOKafkaInputRunnable implements Runnable {
    private final KafkaConsumer consumer;
    private final EsperIOKafkaInputProcessor processor;

    private boolean shutdown;

    public EsperIOKafkaInputRunnable(KafkaConsumer consumer, EsperIOKafkaInputProcessor processor) {
        this.consumer = consumer;
        this.processor = processor;
    }

    public void run() {
        while (!shutdown) {
            ConsumerRecords<Object, Object> records = consumer.poll(1000);
            processor.process(records);
        }
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }
}
