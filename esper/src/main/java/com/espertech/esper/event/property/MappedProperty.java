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
package com.espertech.esper.event.property;

import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndMapped;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetterAndMapped;
import com.espertech.esper.event.arr.ObjectArrayMappedPropertyGetter;
import com.espertech.esper.event.bean.*;
import com.espertech.esper.event.map.MapEventPropertyGetterAndMapped;
import com.espertech.esper.event.map.MapMappedPropertyGetter;
import com.espertech.esper.event.xml.*;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a mapped property or array property, ie. an 'value' property with read method getValue(int index)
 * or a 'array' property via read method getArray() returning an array.
 */
public class MappedProperty extends PropertyBase implements PropertyWithKey {
    private String key;

    public MappedProperty(String propertyName) {
        super(propertyName);
    }

    /**
     * Ctor.
     *
     * @param propertyName is the property name of the mapped property
     * @param key          is the key value to access the mapped property
     */
    public MappedProperty(String propertyName, String key) {
        super(propertyName);
        this.key = key;
    }

    /**
     * Returns the key value for mapped access.
     *
     * @return key value
     */
    public String getKey() {
        return key;
    }

    public String[] toPropertyArray() {
        return new String[]{this.getPropertyNameAtomic()};
    }

    public boolean isDynamic() {
        return false;
    }

    public EventPropertyGetterAndMapped getGetter(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor propertyDesc = eventType.getMappedProperty(propertyNameAtomic);
        if (propertyDesc != null) {
            Method method = propertyDesc.getReadMethod();
            FastClass fastClass = eventType.getFastClass();
            if (fastClass != null) {
                FastMethod fastMethod = fastClass.getMethod(method);
                return new KeyedFastPropertyGetter(fastMethod, key, eventAdapterService);
            } else {
                return new KeyedMethodPropertyGetter(method, key, eventAdapterService);
            }
        }

        // Try the array as a simple property
        propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }

        Class returnType = propertyDesc.getReturnType();
        if (!JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
            return null;
        }

        if (propertyDesc.getReadMethod() != null) {
            FastClass fastClass = eventType.getFastClass();
            Method method = propertyDesc.getReadMethod();
            if (fastClass != null) {
                FastMethod fastMethod = fastClass.getMethod(method);
                return new KeyedMapFastPropertyGetter(method, fastMethod, key, eventAdapterService);
            } else {
                return new KeyedMapMethodPropertyGetter(method, key, eventAdapterService);
            }
        } else {
            Field field = propertyDesc.getAccessorField();
            return new KeyedMapFieldPropertyGetter(field, key, eventAdapterService);
        }
    }

    public Class getPropertyType(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor propertyDesc = eventType.getMappedProperty(propertyNameAtomic);
        if (propertyDesc != null) {
            return propertyDesc.getReadMethod().getReturnType();
        }

        // Check if this is an method returning array which is a type of simple property
        InternalEventPropDescriptor descriptor = eventType.getSimpleProperty(propertyNameAtomic);
        if (descriptor == null) {
            return null;
        }

        Class returnType = descriptor.getReturnType();
        if (!JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
            return null;
        }
        if (descriptor.getReadMethod() != null) {
            return JavaClassHelper.getGenericReturnTypeMap(descriptor.getReadMethod(), false);
        } else if (descriptor.getAccessorField() != null) {
            return JavaClassHelper.getGenericFieldTypeMap(descriptor.getAccessorField(), false);
        } else {
            return null;
        }
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor propertyDesc = eventType.getMappedProperty(propertyNameAtomic);
        if (propertyDesc != null) {
            return new GenericPropertyDesc(propertyDesc.getReadMethod().getReturnType());
        }

        // Check if this is an method returning array which is a type of simple property
        InternalEventPropDescriptor descriptor = eventType.getSimpleProperty(propertyNameAtomic);
        if (descriptor == null) {
            return null;
        }

        Class returnType = descriptor.getReturnType();
        if (!JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
            return null;
        }
        if (descriptor.getReadMethod() != null) {
            Class genericType = JavaClassHelper.getGenericReturnTypeMap(descriptor.getReadMethod(), false);
            return new GenericPropertyDesc(genericType);
        } else if (descriptor.getAccessorField() != null) {
            Class genericType = JavaClassHelper.getGenericFieldTypeMap(descriptor.getAccessorField(), false);
            return new GenericPropertyDesc(genericType);
        } else {
            return null;
        }
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        Object type = optionalMapPropTypes.get(this.getPropertyNameAtomic());
        if (type == null) {
            return null;
        }
        if (type instanceof Class) {
            if (JavaClassHelper.isImplementsInterface((Class) type, Map.class)) {
                return Object.class;
            }
        }
        return null;  // Mapped properties are not allowed in non-dynamic form in a map
    }

    public MapEventPropertyGetterAndMapped getGetterMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        Object type = optionalMapPropTypes.get(getPropertyNameAtomic());
        if (type == null) {
            return null;
        }
        if (type instanceof Class) {
            if (JavaClassHelper.isImplementsInterface((Class) type, Map.class)) {
                return new MapMappedPropertyGetter(getPropertyNameAtomic(), this.getKey());
            }
        }
        if (type instanceof Map) {
            return new MapMappedPropertyGetter(getPropertyNameAtomic(), this.getKey());
        }
        return null;
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
        writer.append("('");
        writer.append(key);
        writer.append("')");
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService, BaseXMLEventType eventType, String propertyExpression) {
        for (SchemaElementComplex complex : complexProperty.getChildren()) {
            if (!complex.getName().equals(propertyNameAtomic)) {
                continue;
            }
            for (SchemaItemAttribute attribute : complex.getAttributes()) {
                if (!attribute.getName().toLowerCase(Locale.ENGLISH).equals("id")) {
                    continue;
                }
            }

            return new DOMMapGetter(propertyNameAtomic, key, null);
        }

        return null;
    }

    public EventPropertyGetterSPI getGetterDOM() {
        return new DOMMapGetter(propertyNameAtomic, key, null);
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService) {
        for (SchemaElementComplex complex : complexProperty.getChildren()) {
            if (!complex.getName().equals(propertyNameAtomic)) {
                continue;
            }
            for (SchemaItemAttribute attribute : complex.getAttributes()) {
                if (!attribute.getName().toLowerCase(Locale.ENGLISH).equals("id")) {
                    continue;
                }
            }

            return complex;
        }

        return null;
    }

    public ObjectArrayEventPropertyGetterAndMapped getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventAdapterService eventAdapterService) {
        Integer index = indexPerProperty.get(propertyNameAtomic);
        if (index == null) {
            return null;
        }
        Object type = nestableTypes.get(getPropertyNameAtomic());
        if (type instanceof Class) {
            if (JavaClassHelper.isImplementsInterface((Class) type, Map.class)) {
                return new ObjectArrayMappedPropertyGetter(index, this.getKey());
            }
        }
        if (type instanceof Map) {
            return new ObjectArrayMappedPropertyGetter(index, this.getKey());
        }
        return null;
    }
}
