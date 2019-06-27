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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.property.DynamicProperty;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;

import java.util.Map;

public interface EventTypeNestableGetterFactory {

    public EventPropertyGetterSPI getPropertyDynamicGetter(Map<String, Object> nestableTypes, String propertyExpression, DynamicProperty prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);

    public EventPropertyGetterMappedSPI getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);

    public EventPropertyGetterIndexedSPI getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);

    public EventPropertyGetterSPI getGetterProperty(String name, BeanEventType nativeFragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    public EventPropertyGetterSPI getGetterEventBean(String name, Class underlyingType);

    public EventPropertyGetterSPI getGetterEventBeanArray(String name, EventType eventType);

    public EventPropertyGetterSPI getGetterBeanNested(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    public EventPropertyGetterSPI getGetterBeanNestedArray(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    public EventPropertyGetterSPI getGetterIndexedEventBean(String propertyNameAtomic, int index);

    public EventPropertyGetterSPI getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType innerType, BeanEventTypeFactory beanEventTypeFactory);

    public EventPropertyGetterSPI getGetterIndexedClassArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, Class componentType, BeanEventTypeFactory beanEventTypeFactory);

    public EventPropertyGetterSPI getGetterMappedProperty(String propertyNameAtomic, String key);

    public EventPropertyGetterSPI getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    public EventPropertyGetterSPI getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetterSPI nestedGetter);

    public EventPropertyGetterSPI getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class propertyTypeGetter);

    public EventPropertyGetterSPI getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNestedMap);

    public EventPropertyGetterSPI getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class nestedReturnType, Class nestedComponentType);

    public EventPropertyGetterSPI getGetterNestedEventBean(String propertyName, EventPropertyGetterSPI nestedGetter);

    public EventPropertyGetterSPI getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    public EventPropertyGetterSPI getGetterNestedEntryBean(String propertyName, EventPropertyGetter innerGetter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    public EventPropertyGetterSPI getGetterRootedDynamicNested(Property prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);
}
