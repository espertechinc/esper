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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.runtime.client.EPRuntime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsperIOKafkaInputProcessorJson implements EsperIOKafkaInputProcessor {

    private final static Logger log = LoggerFactory.getLogger(EsperIOKafkaInputProcessorJson.class);

    private EPRuntime runtime;
    private EsperIOKafkaInputTimestampExtractor timestampExtractor;
    private String eventTypeName;

    public void init(EsperIOKafkaInputProcessorContext context) {
        this.runtime = context.getRuntime();

        String timestampExtractorClassName = context.getProperties().getProperty(EsperIOKafkaConfig.INPUT_TIMESTAMPEXTRACTOR_CONFIG);
        if (timestampExtractorClassName != null) {
            timestampExtractor = (EsperIOKafkaInputTimestampExtractor) JavaClassHelper.instantiate(EsperIOKafkaInputTimestampExtractor.class, timestampExtractorClassName, context.getRuntime().getServicesContext().getClasspathImportServiceRuntime().getClassForNameProvider());
        }

        eventTypeName = context.getProperties().getProperty(EsperIOKafkaConfig.INPUT_EVENTTYPENAME);
        if (eventTypeName == null) {
            throw new EPException("Processor requires configuring the event type name in '" + EsperIOKafkaConfig.INPUT_EVENTTYPENAME + "'");
        }
    }

    public void process(ConsumerRecords<Object, Object> records) {
        for (ConsumerRecord record : records) {

            blockingCheckEventType();

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
                String json = record.value().toString();
                try {
                    runtime.getEventService().sendEventJson(json, eventTypeName);
                }
                catch (EPException ex) {
                    log.error("Exception processing message: " + ex.getMessage(), ex);
                }
            }
        }
    }

    private void blockingCheckEventType() {
        while (true) {
            boolean found = runtime.getEventTypeService().getBusEventType(eventTypeName) != null;
            if (found) {
                break;
            }

            log.info("Waiting to find json event type '" + eventTypeName + "', the type has not been defined or does not have bus-visibility, waiting for 5 seconds");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void close() {

    }
}
