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
package com.espertech.esper.common.internal.event.eventtyperepo;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventAdapterException;
import com.espertech.esper.common.internal.event.xml.SchemaModel;
import com.espertech.esper.common.internal.event.xml.SchemaXMLEventType;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;
import com.espertech.esper.common.internal.event.xml.XSDSchemaMapper;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.Map;

public class EventTypeRepositoryXMLTypeUtil {
    public static void buildXMLTypes(EventTypeRepositoryImpl repo, Map<String, ConfigurationCommonEventTypeXMLDOM> eventTypesXMLDOM, BeanEventTypeFactory beanEventTypeFactory, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory, ClasspathImportService classpathImportService) {
        // Add from the configuration the XML DOM names and type def
        for (Map.Entry<String, ConfigurationCommonEventTypeXMLDOM> entry : eventTypesXMLDOM.entrySet()) {
            if (repo.getTypeByName(entry.getKey()) != null) {
                continue;
            }

            SchemaModel schemaModel = null;
            if ((entry.getValue().getSchemaResource() != null) || (entry.getValue().getSchemaText() != null)) {
                try {
                    schemaModel = XSDSchemaMapper.loadAndMap(entry.getValue().getSchemaResource(), entry.getValue().getSchemaText(), classpathImportService);
                } catch (Exception ex) {
                    throw new ConfigurationException(ex.getMessage(), ex);
                }
            }

            try {
                addXMLDOMType(repo, entry.getKey(), entry.getValue(), schemaModel, beanEventTypeFactory, xmlFragmentEventTypeFactory);
            } catch (Throwable ex) {
                throw new ConfigurationException(ex.getMessage(), ex);
            }
        }
    }

    private static void addXMLDOMType(EventTypeRepositoryImpl repo, String eventTypeName, ConfigurationCommonEventTypeXMLDOM detail, SchemaModel schemaModel, BeanEventTypeFactory beanEventTypeFactory, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory) {
        if (detail.getRootElementName() == null) {
            throw new EventAdapterException("Required root element name has not been supplied");
        }

        EventType existingType = repo.getTypeByName(eventTypeName);
        if (existingType != null) {
            String message = "Event type named '" + eventTypeName + "' has already been declared with differing column name or type information";
            throw new ConfigurationException(message);
        }

        boolean propertyAgnostic = detail.getSchemaResource() == null && detail.getSchemaText() == null;
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, null, EventTypeTypeClass.STREAM, EventTypeApplicationType.XML, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.BUS, propertyAgnostic, new EventTypeIdPair(CRC32Util.computeCRC32(eventTypeName), -1));
        EventType type = beanEventTypeFactory.getEventTypeFactory().createXMLType(metadata, detail, schemaModel, null, metadata.getName(), beanEventTypeFactory, xmlFragmentEventTypeFactory, repo);
        repo.addType(type);

        if (type instanceof SchemaXMLEventType) {
            xmlFragmentEventTypeFactory.addRootType((SchemaXMLEventType) type);
        }
    }
}
