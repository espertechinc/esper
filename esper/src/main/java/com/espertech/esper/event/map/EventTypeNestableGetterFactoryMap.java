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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventPropertyGetterIndexed;
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeNestableGetterFactory;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;

import java.util.Map;

public class EventTypeNestableGetterFactoryMap implements EventTypeNestableGetterFactory {
    public EventPropertyGetter getPropertyProvidedGetter(Map<String, Object> nestableTypes, String propertyName, Property prop, EventAdapterService eventAdapterService) {
        return prop.getGetterMap(nestableTypes, eventAdapterService);
    }

    public EventPropertyGetterMapped getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventAdapterService eventAdapterService) {
        return mappedProperty.getGetterMap(nestableTypes, eventAdapterService);
    }

    public EventPropertyGetterIndexed getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventAdapterService eventAdapterService) {
        return indexedProperty.getGetterMap(nestableTypes, eventAdapterService);
    }

    public EventPropertyGetter getGetterProperty(String name, BeanEventType nativeFragmentType, EventAdapterService eventAdapterService) {
        return new MapEntryPropertyGetter(name, nativeFragmentType, eventAdapterService);
    }

    public MapEventPropertyGetter getGetterEventBean(String name) {
        return new MapEventBeanPropertyGetter(name);
    }

    public MapEventPropertyGetter getGetterEventBeanArray(String name, EventType eventType) {
        return new MapEventBeanArrayPropertyGetter(name, eventType.getUnderlyingType());
    }

    public MapEventPropertyGetter getGetterBeanNestedArray(String name, EventType eventType, EventAdapterService eventAdapterService) {
        return new MapFragmentArrayPropertyGetter(name, eventType, eventAdapterService);
    }

    public EventPropertyGetter getGetterIndexedEventBean(String propertyNameAtomic, int index) {
        return new MapEventBeanArrayIndexedPropertyGetter(propertyNameAtomic, index);
    }

    public EventPropertyGetter getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, EventType innerType) {
        return new MapArrayPropertyGetter(propertyNameAtomic, index, eventAdapterService, innerType);
    }

    public EventPropertyGetter getGetterIndexedPOJO(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, Class componentType) {
        return new MapArrayPOJOEntryIndexedPropertyGetter(propertyNameAtomic, index, eventAdapterService, componentType);
    }

    public EventPropertyGetter getGetterMappedProperty(String propertyNameAtomic, String key) {
        return new MapMappedPropertyGetter(propertyNameAtomic, key);
    }

    public EventPropertyGetter getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetter nestedGetter) {
        return new MapEventBeanArrayIndexedElementPropertyGetter(propertyNameAtomic, index, nestedGetter);
    }

    public EventPropertyGetter getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class propertyTypeGetter) {
        return new MapArrayPOJOBeanEntryIndexedPropertyGetter(propertyNameAtomic, index, nestedGetter, eventAdapterService, propertyTypeGetter);
    }

    public EventPropertyGetter getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNestedMap) {
        return new MapMapPropertyGetter(propertyName, getterNestedMap);
    }

    public EventPropertyGetter getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class nestedReturnType, Class nestedComponentType) {
        return new MapPOJOEntryPropertyGetter(propertyName, nestedGetter, eventAdapterService, nestedReturnType, nestedComponentType);
    }

    public EventPropertyGetter getGetterNestedEventBean(String propertyName, EventPropertyGetter nestedGetter) {
        return new MapEventBeanEntryPropertyGetter(propertyName, nestedGetter);
    }

    public EventPropertyGetter getGetterNestedEntryBean(String propertyName, EventPropertyGetter getter, EventType innerType, EventAdapterService eventAdapterService) {
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new MapNestedEntryPropertyGetterObjectArray(propertyName, innerType, eventAdapterService, (ObjectArrayEventPropertyGetter) getter);
        }
        return new MapNestedEntryPropertyGetterMap(propertyName, innerType, eventAdapterService, (MapEventPropertyGetter) getter);
    }

    public EventPropertyGetter getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventAdapterService eventAdapterService) {
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new MapNestedEntryPropertyGetterArrayObjectArray(propertyNameAtomic, innerType, eventAdapterService, index, (ObjectArrayEventPropertyGetter) getter);
        }
        return new MapNestedEntryPropertyGetterArrayMap(propertyNameAtomic, innerType, eventAdapterService, index, (MapEventPropertyGetter) getter);
    }

    public MapEventPropertyGetter getGetterBeanNested(String name, EventType eventType, EventAdapterService eventAdapterService) {
        if (eventType instanceof ObjectArrayEventType) {
            return new MapPropertyGetterDefaultObjectArray(name, eventType, eventAdapterService);
        }
        return new MapPropertyGetterDefaultMap(name, eventType, eventAdapterService);
    }

    public EventPropertyGetter getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventAdapterService eventAdapterService) {
        return new MapNestedEntryPropertyGetterPropertyProvidedDynamic(propertyName, null, eventAdapterService, nestedGetter);
    }
}
