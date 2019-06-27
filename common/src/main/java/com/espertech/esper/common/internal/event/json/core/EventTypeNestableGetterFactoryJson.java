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
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;
import com.espertech.esper.common.internal.event.json.getter.fromschema.*;
import com.espertech.esper.common.internal.event.json.getter.provided.*;
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
                return new JsonGetterDynamicSimpleSchema(prop.getPropertyNameAtomic());
            }
            if (prop instanceof DynamicIndexedProperty) {
                DynamicIndexedProperty indexed = (DynamicIndexedProperty) prop;
                return new JsonGetterDynamicIndexedSchema(indexed.getPropertyNameAtomic(), indexed.getIndex());
            }
            if (prop instanceof DynamicMappedProperty) {
                DynamicMappedProperty mapped = (DynamicMappedProperty) prop;
                return new JsonGetterDynamicMappedSchema(mapped.getPropertyNameAtomic(), mapped.getKey());
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
                return getGetterIndexedUnderlyingArray(prop.getPropertyNameAtomic(), indexed.getIndex(), eventBeanTypedEventFactory, null, beanEventTypeFactory);
            }
            return null;
        }
        if (prop instanceof DynamicMappedProperty) {
            DynamicMappedProperty mapped = (DynamicMappedProperty) prop;
            if (type == null) {
                return null;
            }
            if (type instanceof Map || type == Map.class) {
                return getGetterMappedProperty(prop.getPropertyNameAtomic(), mapped.getKey());
            }
            return null;
        }
        return null;
    }

    public EventPropertyGetterMappedSPI getPropertyProvidedGetterMap(Map<String, Object> nestableTypes, String mappedPropertyName, MappedProperty mappedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(mappedPropertyName);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterMapRuntimeKeyedProvided(field.getOptionalField());
        }
        return new JsonGetterMapRuntimeKeyedSchema(field);
    }

    public EventPropertyGetterIndexedSPI getPropertyProvidedGetterIndexed(Map<String, Object> nestableTypes, String indexedPropertyName, IndexedProperty indexedProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(indexedPropertyName);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterIndexedRuntimeIndexProvided(field.getOptionalField());
        }
        return new JsonGetterIndexedRuntimeIndexSchema(field);
    }

    public EventPropertyGetterSPI getGetterProperty(String name, BeanEventType nativeFragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(name);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            if (field.getOptionalField().getType().isArray()) {
                return new JsonGetterSimpleProvidedWFragmentArray(field.getOptionalField(), nativeFragmentType, eventBeanTypedEventFactory);
            }
            return new JsonGetterSimpleProvidedWFragmentSimple(field.getOptionalField(), nativeFragmentType, eventBeanTypedEventFactory);
        }
        return new JsonGetterSimpleSchemaWFragment(field, detail.getUnderlyingClassName(), null, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterEventBean(String name, Class underlyingType) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterEventBeanArray(String name, EventType eventType) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterBeanNested(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(name);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterSimpleProvidedWFragmentSimple(field.getOptionalField(), eventType, eventBeanTypedEventFactory);
        }
        return new JsonGetterSimpleSchemaWFragment(field, detail.getUnderlyingClassName(), eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterBeanNestedArray(String name, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(name);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterSimpleProvidedWFragmentArray(field.getOptionalField(), eventType, eventBeanTypedEventFactory);
        }
        return new JsonGetterSimpleSchemaWFragmentArray(field, detail.getUnderlyingClassName(), eventType, eventBeanTypedEventFactory);
    }

    public EventPropertyGetterSPI getGetterIndexedEventBean(String propertyNameAtomic, int index) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterIndexedUnderlyingArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType innerType, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterIndexedProvided(index, detail.getUnderlyingClassName(), innerType, eventBeanTypedEventFactory, field.getOptionalField());
        }
        return new JsonGetterIndexedSchema(index, detail.getUnderlyingClassName(), innerType, eventBeanTypedEventFactory, field);
    }

    public EventPropertyGetterSPI getGetterIndexedClassArray(String propertyNameAtomic, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, Class componentType, BeanEventTypeFactory beanEventTypeFactory) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterIndexedProvidedBaseNative(eventBeanTypedEventFactory, beanEventTypeFactory, componentType, field.getOptionalField(), index);
        }
        return new JsonGetterIndexedSchema(index, detail.getUnderlyingClassName(), null, eventBeanTypedEventFactory, field);
    }

    public EventPropertyGetterSPI getGetterMappedProperty(String propertyNameAtomic, String key) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterMappedProvided(key, detail.getUnderlyingClassName(), field.getOptionalField());
        }
        return new JsonGetterMappedSchema(key, detail.getUnderlyingClassName(), field);
    }

    public EventPropertyGetterSPI getGetterNestedEntryBeanArray(String propertyNameAtomic, int index, EventPropertyGetter getter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterNestedArrayIndexedProvided(index, (JsonEventPropertyGetter) getter, detail.getUnderlyingClassName(), field.getOptionalField());
        }
        return new JsonGetterNestedArrayIndexedSchema(index, (JsonEventPropertyGetter) getter, detail.getUnderlyingClassName(), field);
    }

    public EventPropertyGetterSPI getGetterIndexedEntryEventBeanArrayElement(String propertyNameAtomic, int index, EventPropertyGetterSPI nestedGetter) {
        throw makeIllegalState();
    }

    public EventPropertyGetterSPI getGetterIndexedEntryPOJO(String propertyNameAtomic, int index, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class propertyTypeGetter) {
        JsonUnderlyingField field = findField(propertyNameAtomic);
        if (field.getOptionalField() == null) {
            throw makeIllegalState();
        }
        return new JsonGetterIndexedEntryPOJOProvided(field.getOptionalField(), index, nestedGetter, eventBeanTypedEventFactory, beanEventTypeFactory, propertyTypeGetter);
    }

    public EventPropertyGetterSPI getGetterNestedMapProp(String propertyName, MapEventPropertyGetter getterNestedMap) {
        return null;
    }

    public EventPropertyGetterSPI getGetterNestedPOJOProp(String propertyName, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class nestedReturnType, Class nestedComponentType) {
        JsonUnderlyingField field = findField(propertyName);
        if (field == null || field.getOptionalField() == null) {
            return null;
        }
        return new JsonGetterNestedPOJOPropProvided(eventBeanTypedEventFactory, beanEventTypeFactory, nestedReturnType, nestedComponentType, field.getOptionalField(), nestedGetter);
    }

    public EventPropertyGetterSPI getGetterNestedEventBean(String propertyName, EventPropertyGetterSPI nestedGetter) {
        return null;
    }

    public EventPropertyGetterSPI getGetterNestedPropertyProvidedGetterDynamic(Map<String, Object> nestableTypes, String propertyName, EventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new JsonGetterDynamicNestedSchema(propertyName, (JsonEventPropertyGetter) nestedGetter, detail.getUnderlyingClassName());
    }

    public EventPropertyGetterSPI getGetterNestedEntryBean(String propertyName, EventPropertyGetter innerGetter, EventType innerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        JsonUnderlyingField field = findField(propertyName);
        if (field == null) {
            return null;
        }
        if (field.getOptionalField() != null) {
            return new JsonGetterNestedProvided((JsonEventPropertyGetter) innerGetter, detail.getUnderlyingClassName(), field.getOptionalField());
        }
        return new JsonGetterNestedSchema((JsonEventPropertyGetter) innerGetter, detail.getUnderlyingClassName(), field);
    }

    public JsonEventPropertyGetter getGetterRootedDynamicNested(Property prop, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        if (prop instanceof DynamicSimpleProperty) {
            return new JsonGetterDynamicSimpleSchema(prop.getPropertyNameAtomic());
        } else if (prop instanceof DynamicIndexedProperty) {
            DynamicIndexedProperty indexed = (DynamicIndexedProperty) prop;
            return new JsonGetterDynamicIndexedSchema(indexed.getPropertyNameAtomic(), indexed.getIndex());
        } else if (prop instanceof DynamicMappedProperty) {
            DynamicMappedProperty mapped = (DynamicMappedProperty) prop;
            return new JsonGetterDynamicMappedSchema(mapped.getPropertyNameAtomic(), mapped.getKey());
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
