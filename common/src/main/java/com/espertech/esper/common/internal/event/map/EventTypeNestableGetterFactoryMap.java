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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.property.DynamicProperty;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;

import java.util.Map;

public class EventTypeNestableGetterFactoryMap implements EventTypeNestableGetterFactory {
    public EventPropertyGetterSPI getPropertyDynamicGetter(Map<String, Object> nestableTypes, String propertyExpression, DynamicProperty prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return prop.getGetterMap(nestableTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public EventPropertyGetterMappedSPI getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return mappedProperty.getGetterMap(nestableTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public EventPropertyGetterIndexedSPI getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return indexedProperty.getGetterMap(nestableTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public EventPropertyGetterSPI getGetterProperty(String name, BeanEventType nativeFragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new MapEntryPropertyGetter(name, nativeFragmentType, eventBeanTypedEventFactory);
    }

    public MapEventPropertyGetter getGetterEventBean(String name, Class underlyingType) {
        return new MapEventBeanPropertyGetter(name, underlyingType);
    }

    public MapEventPropertyGetter getGetterEventBeanArray(String name, EventType eventType) {
        return new MapEventBeanArrayPropertyGetter(name, eventType.getUnderlyingType());
    }

    public MapEventPropertyGetter getGetterBeanNestedArray(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new MapFragmentArrayPropertyGetter(name, eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterIndexedEventBean(String propertyNameAtomic, int index) {
        return new MapEventBeanArrayIndexedPropertyGetter(propertyNameAtomic, index);
    }

    public EventPropertyGetterSPI getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType innerType, BeanEventTypeFactory beanEventTypeFactory) {
        return new MapArrayPropertyGetter(propertyNameAtomic, index, eventBeanTypedEventFactory, innerType);
    }

    public EventPropertyGetterSPI getGetterIndexedClassArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, Class componentType, BeanEventTypeFactory beanEventTypeFactory) {
        return new MapArrayPOJOEntryIndexedPropertyGetter(propertyNameAtomic, index, eventBeanTypedEventFactory, beanEventTypeFactory, componentType);
    }

    public EventPropertyGetterSPI getGetterMappedProperty(String propertyNameAtomic, String key) {
        return new MapMappedPropertyGetter(propertyNameAtomic, key);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetterSPI nestedGetter) {
        return new MapEventBeanArrayIndexedElementPropertyGetter(propertyNameAtomic, index, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class propertyTypeGetter) {
        return new MapArrayPOJOBeanEntryIndexedPropertyGetter(propertyNameAtomic, index, nestedGetter, eventBeanTypedEventFactory, beanEventTypeFactory, propertyTypeGetter);
    }

    public EventPropertyGetterSPI getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNestedMap) {
        return new MapMapPropertyGetter(propertyName, getterNestedMap);
    }

    public EventPropertyGetterSPI getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class nestedReturnType, Class nestedComponentType) {
        return new MapPOJOEntryPropertyGetter(propertyName, nestedGetter, eventBeanTypedEventFactory, nestedReturnType, nestedComponentType, beanEventTypeFactory);
    }

    public EventPropertyGetterSPI getGetterNestedEventBean(String propertyName, EventPropertyGetterSPI nestedGetter) {
        return new MapEventBeanEntryPropertyGetter(propertyName, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBean(String propertyName, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new MapNestedEntryPropertyGetterObjectArray(propertyName, innerType, eventBeanTypedEventFactory, (ObjectArrayEventPropertyGetter) getter);
        }
        return new MapNestedEntryPropertyGetterMap(propertyName, innerType, eventBeanTypedEventFactory, (MapEventPropertyGetter) getter);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (getter instanceof ObjectArrayEventPropertyGetter) {
            return new MapNestedEntryPropertyGetterArrayObjectArray(propertyNameAtomic, innerType, eventBeanTypedEventFactory, index, (ObjectArrayEventPropertyGetter) getter);
        }
        return new MapNestedEntryPropertyGetterArrayMap(propertyNameAtomic, innerType, eventBeanTypedEventFactory, index, (MapEventPropertyGetter) getter);
    }

    public MapEventPropertyGetter getGetterBeanNested(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (eventType instanceof ObjectArrayEventType) {
            return new MapPropertyGetterDefaultObjectArray(name, eventType, eventBeanTypedEventFactory);
        }
        return new MapPropertyGetterDefaultMap(name, eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new MapNestedEntryPropertyGetterPropertyProvidedDynamic(propertyName, null, eventBeanTypedEventFactory, nestedGetter);
    }

    public EventPropertyGetterSPI getGetterRootedDynamicNested(Property prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return prop.getGetterMap(null, eventBeanTypedEventFactory, beanEventTypeFactory);
    }
}
