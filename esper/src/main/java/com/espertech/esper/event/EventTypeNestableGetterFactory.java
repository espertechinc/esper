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
package com.espertech.esper.event;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventPropertyGetterIndexed;
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;

import java.util.Map;

public interface EventTypeNestableGetterFactory {

    public EventPropertyGetter getPropertyProvidedGetter(Map<String, Object> nestableTypes, String propertyName, Property prop, EventAdapterService eventAdapterService);

    public EventPropertyGetterMapped getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventAdapterService eventAdapterService);

    public EventPropertyGetterIndexed getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventAdapterService eventAdapterService);

    public EventPropertyGetter getGetterProperty(String name, BeanEventType nativeFragmentType, EventAdapterService eventAdapterService);

    public EventPropertyGetter getGetterEventBean(String name);

    public EventPropertyGetter getGetterEventBeanArray(String name, EventType eventType);

    public EventPropertyGetter getGetterBeanNested(String name, EventType eventType, EventAdapterService eventAdapterService);

    public EventPropertyGetter getGetterBeanNestedArray(String name, EventType eventType, EventAdapterService eventAdapterService);

    public EventPropertyGetter getGetterIndexedEventBean(String propertyNameAtomic, int index);

    public EventPropertyGetter getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, EventType innerType);

    public EventPropertyGetter getGetterIndexedPOJO(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, Class componentType);

    public EventPropertyGetter getGetterMappedProperty(String propertyNameAtomic, String key);

    public EventPropertyGetter getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventAdapterService eventAdapterService);

    public EventPropertyGetter getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetter nestedGetter);

    public EventPropertyGetter getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class propertyTypeGetter);

    public EventPropertyGetter getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNestedMap);

    public EventPropertyGetter getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class nestedReturnType, Class nestedComponentType);

    public EventPropertyGetter getGetterNestedEventBean(String propertyName, EventPropertyGetter nestedGetter);

    public EventPropertyGetter getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventAdapterService eventAdapterService);

    public EventPropertyGetter getGetterNestedEntryBean(String propertyName, EventPropertyGetter innerGetter, EventType innerType, EventAdapterService eventAdapterService);

}
