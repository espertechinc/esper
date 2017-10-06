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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsperIOKafkaInputProcessorDefault implements EsperIOKafkaInputProcessor {

    private final static Logger log = LoggerFactory.getLogger(EsperIOKafkaInputProcessorDefault.class);

    private EPServiceProvider engine;
    private EsperIOKafkaInputTimestampExtractor timestampExtractor;

    public void init(EsperIOKafkaInputProcessorContext context) {
        this.engine = context.getEngine();

        String timestampExtractorClassName = context.getProperties().getProperty(EsperIOKafkaConfig.INPUT_TIMESTAMPEXTRACTOR_CONFIG);
        if (timestampExtractorClassName != null) {
            timestampExtractor = (EsperIOKafkaInputTimestampExtractor) JavaClassHelper.instantiate(EsperIOKafkaInputTimestampExtractor.class, timestampExtractorClassName, context.getEngine().getEngineImportService().getClassForNameProvider());
        }
    }

    public void process(ConsumerRecords<Object, Object> records) {
        for (ConsumerRecord record : records) {

            if (timestampExtractor != null) {
                long timestamp = timestampExtractor.extract(record);

                if (log.isDebugEnabled()) {
                    log.debug("Sending time span {}", timestamp);
                }
                engine.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(timestamp));
            }

            if (record.value() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending event {}", record.value().toString());
                }
                engine.getEPRuntime().sendEvent(record.value());
            }
        }
    }

    public void close() {

    }
}
