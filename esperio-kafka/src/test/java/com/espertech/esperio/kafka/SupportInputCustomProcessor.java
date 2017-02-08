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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.nio.ByteBuffer;

public class SupportInputCustomProcessor implements EsperIOKafkaInputProcessor {

    private static EsperIOKafkaInputProcessorContext context;
    private static boolean closed;

    public void init(EsperIOKafkaInputProcessorContext context) {
        this.context = context;
    }

    public void process(ConsumerRecords<Object, Object> records) {
        for (ConsumerRecord record : records) {
            byte[] value = (byte[]) record.value();
            int intValue = ByteBuffer.wrap(value).getInt();
            context.getEngine().getEPRuntime().sendEvent(new SupportBean("key", intValue));
        }
    }

    public void close() {
        closed = true;
    }

    public static EsperIOKafkaInputProcessorContext getContext() {
        return context;
    }

    public static boolean isClosed() {
        return closed;
    }
}
