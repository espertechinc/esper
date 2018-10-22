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
package com.espertech.esper.common.internal.event.avro;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMeta;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventTypeAvroHandlerFactory {
    private static final Logger log = LoggerFactory.getLogger(EventTypeAvroHandlerFactory.class);

    public static EventTypeAvroHandler resolve(ClasspathImportService classpathImportService, ConfigurationCommonEventTypeMeta.AvroSettings avroSettings, String handlerClass) {
        // Make services that depend on snapshot config entries
        EventTypeAvroHandler avroHandler = EventTypeAvroHandlerUnsupported.INSTANCE;
        if (avroSettings.isEnableAvro()) {
            try {
                avroHandler = (EventTypeAvroHandler) JavaClassHelper.instantiate(EventTypeAvroHandler.class, handlerClass, classpathImportService.getClassForNameProvider());
            } catch (Throwable t) {
                log.debug("Avro provider {} not instantiated, not enabling Avro support: {}", handlerClass, t.getMessage());
            }
            try {
                avroHandler.init(avroSettings, classpathImportService);
            } catch (Throwable t) {
                throw new ConfigurationException("Failed to initialize Esper-Avro: " + t.getMessage(), t);
            }
        }
        return avroHandler;
    }
}
