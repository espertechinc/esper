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
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventTypeDetail;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.variant.VariantSpec;
import com.espertech.esper.common.internal.event.xml.SchemaModel;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;

import java.util.LinkedHashMap;
import java.util.Set;

public interface EventTypeFactory {
    BeanEventType createBeanType(BeanEventTypeStem stem, EventTypeMetadata metadata, BeanEventTypeFactory beanEventTypeFactory, EventType[] superTypes, Set<EventType> deepSuperTypes, String startTimestampPropertyName, String endTimestampPropertyName);

    MapEventType createMap(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver);

    ObjectArrayEventType createObjectArray(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver);

    WrapperEventType createWrapper(EventTypeMetadata metadata, EventType underlying, LinkedHashMap<String, Object> properties, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver);

    EventType createXMLType(EventTypeMetadata metadata, ConfigurationCommonEventTypeXMLDOM detail, SchemaModel schemaModel, String representsFragmentOfProperty, String representsOriginalTypeName, BeanEventTypeFactory beanEventTypeFactory, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory, EventTypeNameResolver eventTypeNameResolver);

    VariantEventType createVariant(EventTypeMetadata metadata, VariantSpec spec);

    JsonEventType createJson(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes, String startTimestampPropertyName, String endTimestampPropertyName, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver, JsonEventTypeDetail detail);
}
