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
package com.espertech.esper.common.internal.event.json.core;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.getter.*;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.property.*;

import java.util.Map;

public class EventTypeNestableGetterFactoryJson implements EventTypeNestableGetterFactory {

    private final JsonEventTypeDetail detail;

    public EventTypeNestableGetterFactoryJson(JsonEventTypeDetail detail) {
        this.detail = detail;
    }

    public EventPropertyGetterSPI getPropertyDynamicGetter(Map<String, Object> nestableTypes, String propertyExpression, DynamicProperty prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        Object type = nestableTypes.get(prop.getPropertyNameAtomic());
        if (type == null && detail.isDynamic()) { // we do not know this property
            if (prop instanceof PropertySimple) {
                return new JsonGetterDynamicSimple(prop.getPropertyNameAtomic());
            }
            if (prop instanceof DynamicIndexedProperty) {
                DynamicIndexedProperty indexed = (DynamicIndexedProperty) prop;
                return new JsonGetterDynamicIndexed(indexed.getPropertyNameAtomic(), indexed.getIndex());
            }
            if (prop instanceof DynamicMappedProperty) {
                DynamicMappedProperty mapped = (DynamicMappedProperty) prop;
                return new JsonGetterDynamicMapped(mapped.getPropertyNameAtomic(), mapped.getKey());
            }
            throw new IllegalStateException("Unrecognized dynamic property " + prop);
        }
        // we do know this property
        if (prop instanceof DynamicSimpleProperty) {
            if (type == null) {
                return null;
            }
            if (type instanceof Class) {
                return getGetterProperty(prop.getPropertyNameAtomic(), null, eventBeanTypedEventFactory);
            }
            if (type instanceof TypeBeanOrUnderlying) {
                EventType eventType = ((TypeBeanOrUnderlying) type).getEventType();
                return getGetterBeanNested(prop.getPropertyNameAtomic(), eventType, eventBeanTypedEventFactory);
            }
            return null;
        }
        if (prop instanceof DynamicIndexedProperty) {
            DynamicIndexedProperty indexed = (DynamicIndexedProperty) prop;
            if (type == null) {
                return null;
            }
            if (type instanceof Class && ((Class) type).isArray()) {
                return getGetterIndexedClassArray(prop.getPropertyNameAtomic(), indexed.getIndex(), eventBeanTypedEventFactory, ((Class) type).getComponentType(), beanEventTypeFactory);
            }
            if (type instanceof TypeBeanOrUnderlying[]) {
                return getGetterIndexedUnderlyingArray(prop.getPropertyNameAtomic(), indexed.getIndex(), eventBeanTypedEventFactory, null);
            }
            return null;
        }
        if (prop instanceof DynamicMappedProperty) {
            DynamicMappedProperty mapped = (DynamicMappedProperty) prop;
            if (type == null) {
                return null;
            }
            if (type instanceof Map) {
                return getGetterMappedProperty(prop.getPropertyNameAtomic(), mapped.getKey());
            }
            return null;
        }
        return null;
    }

    public EventPropertyGetterMappedSPI getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(mappedPropertyName);
        return field == null ? null : new JsonGetterMapRuntimeKeyed(field);
    }

    public EventPropertyGetterIndexedSPI getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(indexedPropertyName);
        return field == null ? null : new JsonGetterIndexedRuntimeIndex(field);
    }

    public EventPropertyGetterSPI getGetterProperty(String name, BeanEventType nativeFragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(name);
        return field == null ? null : new JsonGetterSimpleWFragmentSimple(field, detail.getUnderlyingClassName(), null, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterEventBean(String name, Class underlyingType) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterEventBeanArray(String name, EventType eventType) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterBeanNested(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(name);
        return field == null ? null : new JsonGetterSimpleWFragmentSimple(field, detail.getUnderlyingClassName(), eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterBeanNestedArray(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(name);
        return new JsonGetterSimpleWFragmentArray(field, detail.getUnderlyingClassName(), eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterIndexedEventBean(String propertyNameAtomic, int index) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType innerType) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        return field == null ? null : new JsonGetterIndexed(field, index, detail.getUnderlyingClassName(), innerType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterIndexedClassArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, Class componentType, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        return field == null ? null : new JsonGetterIndexed(field, index, detail.getUnderlyingClassName(), null, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterMappedProperty(String propertyNameAtomic, String key) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        return field == null ? null : new JsonGetterMapped(field, key, detail.getUnderlyingClassName());
    }

    public EventPropertyGetterSPI getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        return field == null ? null : new JsonGetterNestedArrayIndexed(field, index, (JsonEventPropertyGetter) getter, detail.getUnderlyingClassName(), innerType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetterSPI nestedGetter) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class propertyTypeGetter) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNestedMap) {
        return null;
    }

    public EventPropertyGetterSPI getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class nestedReturnType, Class nestedComponentType) {
        return null;
    }

    public EventPropertyGetterSPI getGetterNestedEventBean(String propertyName, EventPropertyGetterSPI nestedGetter) {
        return null;
    }

    public EventPropertyGetterSPI getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new JsonGetterDynamicNested(propertyName, (JsonEventPropertyGetter) nestedGetter, detail.getUnderlyingClassName());
    }

    public EventPropertyGetterSPI getGetterNestedEntryBean(String propertyName, EventPropertyGetter innerGetter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(propertyName);
        if (field == null) {
            return null;
        }
        return new JsonGetterNested(field, (JsonEventPropertyGetter) innerGetter, detail.getUnderlyingClassName());
    }

    public JsonEventPropertyGetter getGetterRootedDynamicNested(Property prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        if (prop instanceof DynamicSimpleProperty) {
            return new JsonGetterDynamicSimple(prop.getPropertyNameAtomic());
        } else if (prop instanceof DynamicIndexedProperty) {
            DynamicIndexedProperty indexed = (DynamicIndexedProperty) prop;
            return new JsonGetterDynamicIndexed(indexed.getPropertyNameAtomic(), indexed.getIndex());
        } else if (prop instanceof DynamicMappedProperty) {
            DynamicMappedProperty mapped = (DynamicMappedProperty) prop;
            return new JsonGetterDynamicMapped(mapped.getPropertyNameAtomic(), mapped.getKey());
        } else if (prop instanceof NestedProperty) {
            NestedProperty nested = (NestedProperty) prop;
            JsonEventPropertyGetter[] getters = new JsonEventPropertyGetter[nested.getProperties().size()];
            for (int i = 0; i < nested.getProperties().size(); i++) {
                getters[i] = getGetterRootedDynamicNested(nested.getProperties().get(i), eventBeanTypedEventFactory, beanEventTypeFactory);
            }
            return new JsonGetterDynamicNestedChain(detail.getUnderlyingClassName(), getters);
        } else {
            throw new IllegalStateException("Rerecognized dynamic property " + prop);
        }
    }

    private JsonUnderlyingField findField(String name) {
        return detail.getFieldDescriptors().get(name);
    }

    private IllegalStateException makeIllegalState() {
        return new IllegalStateException("An implementation of this getter is not available for Json event types");
    }
}
