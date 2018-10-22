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
package com.espertech.esper.common.internal.event.path;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactory;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.event.variant.VariantSpec;
import com.espertech.esper.common.internal.event.xml.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EventTypeCollectorImpl implements EventTypeCollector {
    private final Map<String, EventType> moduleEventTypes;
    private final BeanEventTypeFactory beanEventTypeFactory;
    private final EventTypeFactory eventTypeFactory;
    private final BeanEventTypeStemService beanEventTypeStemService;
    private final EventTypeNameResolver eventTypeNameResolver;
    private final XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory;
    private final EventTypeAvroHandler eventTypeAvroHandler;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public EventTypeCollectorImpl(Map<String, EventType> moduleEventTypes, BeanEventTypeFactory beanEventTypeFactory, EventTypeFactory eventTypeFactory, BeanEventTypeStemService beanEventTypeStemService, EventTypeNameResolver eventTypeNameResolver, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory, EventTypeAvroHandler eventTypeAvroHandler, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.moduleEventTypes = moduleEventTypes;
        this.beanEventTypeFactory = beanEventTypeFactory;
        this.eventTypeFactory = eventTypeFactory;
        this.beanEventTypeStemService = beanEventTypeStemService;
        this.eventTypeNameResolver = eventTypeNameResolver;
        this.xmlFragmentEventTypeFactory = xmlFragmentEventTypeFactory;
        this.eventTypeAvroHandler = eventTypeAvroHandler;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void registerMap(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName) {
        MapEventType eventType = eventTypeFactory.createMap(metadata, properties, superTypes, startTimestampPropertyName, endTimestampPropertyName, beanEventTypeFactory, eventTypeNameResolver);
        handleRegister(eventType);
    }


    public void registerObjectArray(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName) {
        ObjectArrayEventType eventType = eventTypeFactory.createObjectArray(metadata, properties, superTypes, startTimestampPropertyName, endTimestampPropertyName, beanEventTypeFactory, eventTypeNameResolver);
        handleRegister(eventType);
    }

    public void registerWrapper(EventTypeMetadata metadata, EventType underlying, LinkedHashMap<String, Object> properties) {
        WrapperEventType eventType = eventTypeFactory.createWrapper(metadata, underlying, properties, beanEventTypeFactory, eventTypeNameResolver);
        handleRegister(eventType);
    }

    public void registerBean(EventTypeMetadata metadata, Class clazz, String startTimestampName, String endTimestampName, EventType[] superTypes, Set<EventType> deepSuperTypes) {
        BeanEventTypeStem stem = beanEventTypeStemService.getCreateStem(clazz, null);
        BeanEventType eventType = eventTypeFactory.createBeanType(stem, metadata, beanEventTypeFactory, superTypes, deepSuperTypes, startTimestampName, endTimestampName);
        handleRegister(eventType);
    }

    public void registerXML(EventTypeMetadata metadata, String representsFragmentOfProperty, String representsOriginalTypeName) {
        EventType existing = xmlFragmentEventTypeFactory.getTypeByName(metadata.getName());
        if (existing != null) {
            handleRegister(existing);
            return;
        }

        SchemaXMLEventType schemaType = xmlFragmentEventTypeFactory.getRootTypeByName(representsOriginalTypeName);
        if (schemaType == null) {
            throw new EPException("Failed to find XML schema type '" + representsOriginalTypeName + "'");
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(representsFragmentOfProperty);
        SchemaElementComplex schemaModelRoot = SchemaUtil.findRootElement(schemaType.getSchemaModel(), schemaType.getConfigurationEventTypeXMLDOM().getRootElementNamespace(), schemaType.getRootElementName());
        SchemaItem item = prop.getPropertyTypeSchema(schemaModelRoot);
        SchemaElementComplex complex = (SchemaElementComplex) item;
        EventType eventType = xmlFragmentEventTypeFactory.getCreateXMLDOMType(representsOriginalTypeName, metadata.getName(), metadata.getModuleName(), complex, representsFragmentOfProperty);
        handleRegister(eventType);
    }

    public void registerAvro(EventTypeMetadata metadata, String schemaJson) {
        EventType eventType = eventTypeAvroHandler.newEventTypeFromJson(metadata, eventBeanTypedEventFactory, schemaJson);
        handleRegister(eventType);
    }

    public void registerVariant(EventTypeMetadata metadata, EventType[] variants, boolean any) {
        VariantSpec spec = new VariantSpec(variants, any ? ConfigurationCommonVariantStream.TypeVariance.ANY : ConfigurationCommonVariantStream.TypeVariance.PREDEFINED);
        EventType eventType = eventTypeFactory.createVariant(metadata, spec);
        handleRegister(eventType);
    }

    private void handleRegister(EventType eventType) {
        if (moduleEventTypes.containsKey(eventType.getName())) {
            throw new IllegalStateException("Event type '" + eventType.getName() + "' attempting to register multiple times");
        }
        moduleEventTypes.put(eventType.getName(), eventType);
    }
}
