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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.HashMap;
import java.util.Map;

public class XMLFragmentEventTypeFactory {
    private final BeanEventTypeFactory eventTypeFactory;
    private final EventTypeCompileTimeRegistry optionalCompileTimeRegistry;
    private final EventTypeNameResolver eventTypeNameResolver;

    private Map<String, SchemaXMLEventType> rootTypes;
    private Map<String, SchemaXMLEventType> derivedTypes;

    public XMLFragmentEventTypeFactory(BeanEventTypeFactory eventTypeFactory, EventTypeCompileTimeRegistry optionalCompileTimeRegistry, EventTypeNameResolver eventTypeNameResolver) {
        this.eventTypeFactory = eventTypeFactory;
        this.optionalCompileTimeRegistry = optionalCompileTimeRegistry;
        this.eventTypeNameResolver = eventTypeNameResolver;
    }

    public void addRootType(SchemaXMLEventType type) {
        if (rootTypes == null) {
            rootTypes = new HashMap<>();
        }
        if (rootTypes.containsKey(type.getName())) {
            throw new IllegalStateException("Type '" + type.getName() + "' already exists");
        }
        rootTypes.put(type.getName(), type);
    }

    public EventType getTypeByName(String derivedEventTypeName) {
        if (derivedTypes == null) {
            derivedTypes = new HashMap<>();
        }
        return derivedTypes.get(derivedEventTypeName);
    }

    public EventType getCreateXMLDOMType(String rootTypeName, String derivedEventTypeName, String moduleName, SchemaElementComplex complex, String representsFragmentOfProperty) {
        if (rootTypes == null) {
            rootTypes = new HashMap<>();
        }
        if (derivedTypes == null) {
            derivedTypes = new HashMap<>();
        }
        SchemaXMLEventType type = rootTypes.get(rootTypeName);
        if (type == null) {
            throw new IllegalStateException("Failed to find XML root event type '" + rootTypeName + "'");
        }
        ConfigurationCommonEventTypeXMLDOM config = type.getConfigurationEventTypeXMLDOM();

        // add a new type
        ConfigurationCommonEventTypeXMLDOM xmlDom = new ConfigurationCommonEventTypeXMLDOM();
        xmlDom.setRootElementName("//" + complex.getName());    // such the reload of the type can resolve it
        xmlDom.setRootElementNamespace(complex.getNamespace());
        xmlDom.setAutoFragment(config.isAutoFragment());
        xmlDom.setEventSenderValidatesRoot(config.isEventSenderValidatesRoot());
        xmlDom.setXPathPropertyExpr(config.isXPathPropertyExpr());
        xmlDom.setXPathResolvePropertiesAbsolute(config.isXPathResolvePropertiesAbsolute());
        xmlDom.setSchemaResource(config.getSchemaResource());
        xmlDom.setSchemaText(config.getSchemaText());
        xmlDom.setXPathFunctionResolver(config.getXPathFunctionResolver());
        xmlDom.setXPathVariableResolver(config.getXPathVariableResolver());
        xmlDom.setDefaultNamespace(config.getDefaultNamespace());
        xmlDom.addNamespacePrefixes(config.getNamespacePrefixes());

        EventTypeMetadata metadata = new EventTypeMetadata(derivedEventTypeName, moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.XML, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.BUS, false, new EventTypeIdPair(CRC32Util.computeCRC32(derivedEventTypeName), -1));
        SchemaXMLEventType eventType = (SchemaXMLEventType) eventTypeFactory.getEventTypeFactory().createXMLType(metadata, xmlDom, type.getSchemaModel(), representsFragmentOfProperty, rootTypeName, eventTypeFactory, this, eventTypeNameResolver);
        derivedTypes.put(derivedEventTypeName, eventType);

        if (optionalCompileTimeRegistry != null) {
            optionalCompileTimeRegistry.newType(eventType);
        }
        return eventType;
    }

    public SchemaXMLEventType getRootTypeByName(String representsOriginalTypeName) {
        return rootTypes == null ? null : rootTypes.get(representsOriginalTypeName);
    }
}
