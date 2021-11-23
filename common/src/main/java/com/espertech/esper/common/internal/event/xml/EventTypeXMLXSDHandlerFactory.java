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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMeta;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventTypeXMLXSDHandlerFactory {
    private static final Logger log = LoggerFactory.getLogger(EventTypeXMLXSDHandlerFactory.class);

    public static EventTypeXMLXSDHandler resolve(ClasspathImportService classpathImportService, ConfigurationCommonEventTypeMeta config, String handlerClass) {
        // Make services that depend on snapshot config entries
        EventTypeXMLXSDHandler xmlxsdHandler = EventTypeXMLXSDHandlerUnsupported.INSTANCE;
        if (config.isEnableXMLXSD()) {
            try {
                xmlxsdHandler = JavaClassHelper.instantiate(EventTypeXMLXSDHandler.class, handlerClass, classpathImportService.getClassForNameProvider());
            } catch (Throwable t) {
                log.warn("XML-XSD provider {} not instantiated, not enabling XML-XSD support: {}", handlerClass, t.getMessage());
            }
        }
        return xmlxsdHandler;
    }
}
