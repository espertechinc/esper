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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.*;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;

import java.util.Map;

public class EventTypeNestableGetterFactoryObjectArray implements EventTypeNestableGetterFactory {

    private final String eventTypeName;
    private final Map<String, Integer> propertiesIndex;

    public EventTypeNestableGetterFactoryObjectArray(String eventTypeName, Map<String, Integer> propertiesIndex) {
        this.eventTypeName = eventTypeName;
        this.propertiesIndex = propertiesIndex;
    }

    public Map<String, Integer> getPropertiesIndex() {
        return propertiesIndex;
    }

    public EventPropertyGetterSPI getPropertyProvidedGetter(Map<String, Object> nestableTypes, String propertyName, Property prop, EventAdapterService eventAdapterService) {
        return prop.getGetterObjectArray(propertiesIndex, nestableTypes, eventAdapterService);
    }

    public EventPropertyGetterSPI getGetterProperty(String name, BeanEventType nativeFragmentType, EventAdapterService eventAdapterService) {
        int index = getAssertIndex(name);
        return new ObjectArrayEntryPropertyGetter(index, nativeFragmentType, eventAdapterService);
    }

    public EventPropertyGetterSPI getGetterEventBean(String name, Class underlyingType) {
        int index = getAssertIndex(name);
        return new ObjectArrayEventBeanPropertyGetter(index, underlyingType);
    }

    public EventPropertyGetterSPI getGetterEventBeanArray(String name, EventType eventType) {
        int index = getAssertIndex(name);
        return new ObjectArrayEventBeanArrayPropertyGetter(index, eventType.getUnderlyingType());
    }

    public EventPropertyGetterSPI getGetterBeanNestedArray(String name, EventType eventType, EventAdapterService eventAdapterService) {
        int index = getAssertIndex(name);
        return new ObjectArrayFragmentArrayPropertyGetter(index, eventType, eventAdapterService);
    }

    public EventPropertyGetterSPI getGetterIndexedEventBean(String propertyNameAtomic, int index) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayEventBeanArrayIndexedPropertyGetter(propertyIndex, index);
    }

    public EventPropertyGetterSPI getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, EventType innerType) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayArrayPropertyGetter(propertyIndex, index, eventAdapterService, innerType);
    }

    public EventPropertyGetterSPI getGetterIndexedPOJO(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, Class componentType) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayArrayPOJOEntryIndexedPropertyGetter(propertyIndex, index, eventAdapterService, componentType);
    }

    public EventPropertyGetterSPI getGetterMappedProperty(String propertyNameAtomic, String key) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayMappedPropertyGetter(propertyIndex, key);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetterSPI nestedGetter) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayEventBeanArrayIndexedElementPropertyGetter(propertyIndex, index, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class propertyTypeGetter) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayArrayPOJOBeanEntryIndexedPropertyGetter(propertyIndex, index, nestedGetter, eventAdapterService, propertyTypeGetter);
    }

    public EventPropertyGetterSPI getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNested) {
        int index = getAssertIndex(propertyName);
        return new ObjectArrayMapPropertyGetter(index, getterNested);
    }

    public EventPropertyGetterSPI getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class nestedReturnType, Class nestedComponentType) {
        int index = getAssertIndex(propertyName);
        return new ObjectArrayPOJOEntryPropertyGetter(index, nestedGetter, eventAdapterService, nestedReturnType, nestedComponentType);
    }

    public EventPropertyGetterSPI getGetterNestedEventBean(String propertyName, EventPropertyGetterSPI nestedGetter) {
        int index = getAssertIndex(propertyName);
        return new ObjectArrayEventBeanEntryPropertyGetter(index, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBean(String propertyName, EventPropertyGetter getter, EventType innerType, EventAdapterService eventAdapterService) {
        int propertyIndex = getAssertIndex(propertyName);
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new ObjectArrayNestedEntryPropertyGetterObjectArray(propertyIndex, innerType, eventAdapterService, (ObjectArrayEventPropertyGetter) getter);
        }
        return new ObjectArrayNestedEntryPropertyGetterMap(propertyIndex, innerType, eventAdapterService, (MapEventPropertyGetter) getter);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventAdapterService eventAdapterService) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new ObjectArrayNestedEntryPropertyGetterArrayObjectArray(propertyIndex, innerType, eventAdapterService, index, (ObjectArrayEventPropertyGetter) getter);
        }
        return new ObjectArrayNestedEntryPropertyGetterArrayMap(propertyIndex, innerType, eventAdapterService, index, (MapEventPropertyGetter) getter);
    }

    public EventPropertyGetterSPI getGetterBeanNested(String name, EventType eventType, EventAdapterService eventAdapterService) {
        int index = getAssertIndex(name);
        if (eventType instanceof ObjectArrayEventType) {
            return new ObjectArrayPropertyGetterDefaultObjectArray(index, eventType, eventAdapterService);
        }
        return new ObjectArrayPropertyGetterDefaultMap(index, eventType, eventAdapterService);
    }

    private int getAssertIndex(String propertyName) {
        Integer index = propertiesIndex.get(propertyName);
        if (index == null) {
            throw new PropertyAccessException("Property '" + propertyName + "' could not be found as a property of type '" + eventTypeName + "'");
        }
        return index;
    }

    public EventPropertyGetterMappedSPI getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventAdapterService eventAdapterService) {
        return mappedProperty.getGetterObjectArray(propertiesIndex, nestableTypes, eventAdapterService);
    }

    public EventPropertyGetterIndexedSPI getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventAdapterService eventAdapterService) {
        return indexedProperty.getGetterObjectArray(propertiesIndex, nestableTypes, eventAdapterService);
    }

    public EventPropertyGetterSPI getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventAdapterService eventAdapterService) {
        return null; // this case is not supported
    }
}
