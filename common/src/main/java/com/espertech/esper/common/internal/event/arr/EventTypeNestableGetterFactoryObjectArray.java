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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.property.DynamicProperty;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;

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

    public EventPropertyGetterSPI getPropertyDynamicGetter(Map<String, Object> nestableTypes, String propertyExpression, DynamicProperty prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return prop.getGetterObjectArray(propertiesIndex, nestableTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public EventPropertyGetterSPI getGetterProperty(String name, BeanEventType nativeFragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        int index = getAssertIndex(name);
        return new ObjectArrayEntryPropertyGetter(index, nativeFragmentType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterEventBean(String name, Class underlyingType) {
        int index = getAssertIndex(name);
        return new ObjectArrayEventBeanPropertyGetter(index, underlyingType);
    }

    public EventPropertyGetterSPI getGetterEventBeanArray(String name, EventType eventType) {
        int index = getAssertIndex(name);
        return new ObjectArrayEventBeanArrayPropertyGetter(index, eventType.getUnderlyingType());
    }

    public EventPropertyGetterSPI getGetterBeanNestedArray(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        int index = getAssertIndex(name);
        return new ObjectArrayFragmentArrayPropertyGetter(index, eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterIndexedEventBean(String propertyNameAtomic, int index) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayEventBeanArrayIndexedPropertyGetter(propertyIndex, index);
    }

    public EventPropertyGetterSPI getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType innerType, BeanEventTypeFactory beanEventTypeFactory) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayArrayPropertyGetter(propertyIndex, index, eventBeanTypedEventFactory, innerType);
    }

    public EventPropertyGetterSPI getGetterIndexedClassArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, Class componentType, BeanEventTypeFactory beanEventTypeFactory) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayArrayPOJOEntryIndexedPropertyGetter(propertyIndex, index, eventBeanTypedEventFactory, beanEventTypeFactory, componentType);
    }

    public EventPropertyGetterSPI getGetterMappedProperty(String propertyNameAtomic, String key) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayMappedPropertyGetter(propertyIndex, key);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetterSPI nestedGetter) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayEventBeanArrayIndexedElementPropertyGetter(propertyIndex, index, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class propertyTypeGetter) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        return new ObjectArrayArrayPOJOBeanEntryIndexedPropertyGetter(propertyIndex, index, nestedGetter, eventBeanTypedEventFactory, beanEventTypeFactory, propertyTypeGetter);
    }

    public EventPropertyGetterSPI getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNested) {
        int index = getAssertIndex(propertyName);
        return new ObjectArrayMapPropertyGetter(index, getterNested);
    }

    public EventPropertyGetterSPI getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class nestedReturnType, Class nestedComponentType) {
        int index = getAssertIndex(propertyName);
        return new ObjectArrayPOJOEntryPropertyGetter(index, nestedGetter, eventBeanTypedEventFactory, beanEventTypeFactory, nestedReturnType, nestedComponentType);
    }

    public EventPropertyGetterSPI getGetterNestedEventBean(String propertyName, EventPropertyGetterSPI nestedGetter) {
        int index = getAssertIndex(propertyName);
        return new ObjectArrayEventBeanEntryPropertyGetter(index, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBean(String propertyName, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        int propertyIndex = getAssertIndex(propertyName);
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new ObjectArrayNestedEntryPropertyGetterObjectArray(propertyIndex, innerType, eventBeanTypedEventFactory, (ObjectArrayEventPropertyGetter) getter);
        }
        return new ObjectArrayNestedEntryPropertyGetterMap(propertyIndex, innerType, eventBeanTypedEventFactory, (MapEventPropertyGetter) getter);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        int propertyIndex = getAssertIndex(propertyNameAtomic);
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new ObjectArrayNestedEntryPropertyGetterArrayObjectArray(propertyIndex, innerType, eventBeanTypedEventFactory, index, (ObjectArrayEventPropertyGetter) getter);
        }
        return new ObjectArrayNestedEntryPropertyGetterArrayMap(propertyIndex, innerType, eventBeanTypedEventFactory, index, (MapEventPropertyGetter) getter);
    }

    public EventPropertyGetterSPI getGetterBeanNested(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        int index = getAssertIndex(name);
        if (eventType instanceof ObjectArrayEventType) {
            return new ObjectArrayPropertyGetterDefaultObjectArray(index, eventType, eventBeanTypedEventFactory);
        }
        return new ObjectArrayPropertyGetterDefaultMap(index, eventType, eventBeanTypedEventFactory);
    }

    private int getAssertIndex(String propertyName) {
        Integer index = propertiesIndex.get(propertyName);
        if (index == null) {
            throw new PropertyAccessException("Property '" + propertyName + "' could not be found as a property of type '" + eventTypeName + "'");
        }
        return index;
    }

    public EventPropertyGetterMappedSPI getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return mappedProperty.getGetterObjectArray(propertiesIndex, nestableTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public EventPropertyGetterIndexedSPI getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return indexedProperty.getGetterObjectArray(propertiesIndex, nestableTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public EventPropertyGetterSPI getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return null; // this case is not supported
    }

    public EventPropertyGetterSPI getGetterRootedDynamicNested(Property prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        throw new IllegalStateException("This getter is not available for object-array events");
    }
}
