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
package com.espertech.esper.common.internal.event.eventtypefactory;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.core.EventTypeNestableGetterFactoryJson;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventTypeDetail;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.variant.VariantSpec;
import com.espertech.esper.common.internal.event.xml.SchemaModel;
import com.espertech.esper.common.internal.event.xml.SchemaXMLEventType;
import com.espertech.esper.common.internal.event.xml.SimpleXMLEventType;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;

import java.util.LinkedHashMap;
import java.util.Set;

public class EventTypeFactoryImpl implements EventTypeFactory {
    public final static EventTypeFactoryImpl INSTANCE = new EventTypeFactoryImpl();

    private EventTypeFactoryImpl() {
    }

    public BeanEventType createBeanType(BeanEventTypeStem stem, EventTypeMetadata metadata, BeanEventTypeFactory beanEventTypeFactory, EventType[] superTypes, Set<EventType> deepSuperTypes, String startTimestampPropertyName, String endTimestampPropertyName) {
        return new BeanEventType(stem, metadata, beanEventTypeFactory, superTypes, deepSuperTypes, startTimestampPropertyName, endTimestampPropertyName);
    }

    public MapEventType createMap(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties,
                                  String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName,
                                  BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver) {
        Pair<EventType[], Set<EventType>> st = EventTypeUtility.getSuperTypesDepthFirst(superTypes, EventUnderlyingType.MAP, eventTypeNameResolver);
        properties = BaseNestableEventUtil.resolvePropertyTypes(properties, eventTypeNameResolver);
        return new MapEventType(metadata, properties,
                st.getFirst(), st.getSecond(), startTimestampPropertyName, endTimestampPropertyName,
                beanEventTypeFactory);
    }

    public ObjectArrayEventType createObjectArray(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver) {
        Pair<EventType[], Set<EventType>> st = EventTypeUtility.getSuperTypesDepthFirst(superTypes, EventUnderlyingType.OBJECTARRAY, eventTypeNameResolver);
        properties = BaseNestableEventUtil.resolvePropertyTypes(properties, eventTypeNameResolver);
        return new ObjectArrayEventType(metadata, properties, st.getFirst(), st.getSecond(),
                startTimestampPropertyName, endTimestampPropertyName, beanEventTypeFactory);
    }

    public WrapperEventType createWrapper(EventTypeMetadata metadata, EventType underlying, LinkedHashMap<String, Object> properties, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver) {
        return WrapperEventTypeUtil.makeWrapper(metadata, underlying, properties, beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory, eventTypeNameResolver);
    }

    public EventType createXMLType(EventTypeMetadata metadata, ConfigurationCommonEventTypeXMLDOM detail, SchemaModel schemaModel, String representsFragmentOfProperty, String representsOriginalTypeName, BeanEventTypeFactory beanEventTypeFactory, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory, EventTypeNameResolver eventTypeNameResolver) {
        if (metadata.isPropertyAgnostic()) {
            return new SimpleXMLEventType(metadata, detail, beanEventTypeFactory.getEventBeanTypedEventFactory(), eventTypeNameResolver, xmlFragmentEventTypeFactory);
        }
        return new SchemaXMLEventType(metadata, detail, schemaModel, representsFragmentOfProperty, representsOriginalTypeName, beanEventTypeFactory.getEventBeanTypedEventFactory(), eventTypeNameResolver, xmlFragmentEventTypeFactory);
    }

    public VariantEventType createVariant(EventTypeMetadata metadata, VariantSpec spec) {
        return new VariantEventType(metadata, spec);
    }

    public JsonEventType createJson(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver, JsonEventTypeDetail detail) {
        Pair<EventType[], Set<EventType>> st = EventTypeUtility.getSuperTypesDepthFirst(superTypes, EventUnderlyingType.JSON, eventTypeNameResolver);
        properties = BaseNestableEventUtil.resolvePropertyTypes(properties, eventTypeNameResolver);
        EventTypeNestableGetterFactoryJson getterFactoryJson = new EventTypeNestableGetterFactoryJson(detail);
        // We use a null-stand-in class as the actual underlying class is provided later
        return new JsonEventType(metadata, properties,
            st.getFirst(), st.getSecond(), startTimestampPropertyName, endTimestampPropertyName, getterFactoryJson, beanEventTypeFactory, detail, null);
    }
}
