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

import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.runtime.client.EPRuntime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsperIOKafkaInputProcessorDefault implements EsperIOKafkaInputProcessor {

    private final static Logger log = LoggerFactory.getLogger(EsperIOKafkaInputProcessorDefault.class);

    private EPRuntime runtime;
    private EsperIOKafkaInputTimestampExtractor timestampExtractor;

    public void init(EsperIOKafkaInputProcessorContext context) {
        this.runtime = context.getRuntime();

        String timestampExtractorClassName = context.getProperties().getProperty(EsperIOKafkaConfig.INPUT_TIMESTAMPEXTRACTOR_CONFIG);
        if (timestampExtractorClassName != null) {
            timestampExtractor = (EsperIOKafkaInputTimestampExtractor) JavaClassHelper.instantiate(EsperIOKafkaInputTimestampExtractor.class, timestampExtractorClassName, context.getRuntime().getServicesContext().getClasspathImportServiceRuntime().getClassForNameProvider());
        }
    }

    public void process(ConsumerRecords<Object, Object> records) {
        for (ConsumerRecord record : records) {

            if (timestampExtractor != null) {
                long timestamp = timestampExtractor.extract(record);

                if (log.isDebugEnabled()) {
                    log.debug("Sending time span {}", timestamp);
                }
                runtime.getEventService().advanceTimeSpan(timestamp);
            }

            if (record.value() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending event {}", record.value().toString());
                }
                Object event = record.value();
                runtime.getEventService().sendEventBean(event, event.getClass().getSimpleName());
            }
        }
    }

    public void close() {

    }
}
