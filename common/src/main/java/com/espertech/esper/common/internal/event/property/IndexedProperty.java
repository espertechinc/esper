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
package com.espertech.esper.common.internal.event.property;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.arr.ObjectArrayArrayPOJOEntryIndexedPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayArrayPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetterAndIndexed;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;
import com.espertech.esper.common.internal.event.bean.getter.*;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterAndIndexed;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.map.MapArrayPOJOEntryIndexedPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapArrayPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetterAndIndexed;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.xml.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Represents an indexed property or array property, ie. an 'value' property with read method getValue(int index)
 * or a 'array' property via read method getArray() returning an array.
 */
public class IndexedProperty extends PropertyBase implements PropertyWithIndex {
    private int index;

    public IndexedProperty(String propertyName) {
        super(propertyName);
    }

    /**
     * Ctor.
     *
     * @param propertyName is the property name
     * @param index        is the index to use to access the property value
     */
    public IndexedProperty(String propertyName, int index) {
        super(propertyName);
        this.index = index;
    }

    public boolean isDynamic() {
        return false;
    }

    public String[] toPropertyArray() {
        return new String[]{this.getPropertyNameAtomic()};
    }

    /**
     * Returns index for indexed access.
     *
     * @return index value
     */
    public int getIndex() {
        return index;
    }

    public EventPropertyGetterAndIndexed getGetter(BeanEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        PropertyStem propertyDesc = eventType.getIndexedProperty(propertyNameAtomic);
        if (propertyDesc != null) {
            return new KeyedMethodPropertyGetter(propertyDesc.getReadMethod(), index, eventBeanTypedEventFactory, beanEventTypeFactory);
        }

        // Try the array as a simple property
        propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }

        Class returnType = propertyDesc.getReturnType();
        if (returnType.isArray()) {
            if (propertyDesc.getReadMethod() != null) {
                Method method = propertyDesc.getReadMethod();
                return new ArrayMethodPropertyGetter(method, index, eventBeanTypedEventFactory, beanEventTypeFactory);
            } else {
                Field field = propertyDesc.getAccessorField();
                return new ArrayFieldPropertyGetter(field, index, eventBeanTypedEventFactory, beanEventTypeFactory);
            }
        } else if (JavaClassHelper.isImplementsInterface(returnType, List.class)) {
            if (propertyDesc.getReadMethod() != null) {
                Method method = propertyDesc.getReadMethod();
                return new ListMethodPropertyGetter(method, index, eventBeanTypedEventFactory, beanEventTypeFactory);
            } else {
                Field field = propertyDesc.getAccessorField();
                return new ListFieldPropertyGetter(field, index, eventBeanTypedEventFactory, beanEventTypeFactory);
            }
        } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
            if (propertyDesc.getReadMethod() != null) {
                Method method = propertyDesc.getReadMethod();
                return new IterableMethodPropertyGetter(method, index, eventBeanTypedEventFactory, beanEventTypeFactory);
            } else {
                Field field = propertyDesc.getAccessorField();
                return new IterableFieldPropertyGetter(field, index, eventBeanTypedEventFactory, beanEventTypeFactory);
            }
        }

        return null;
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory) {
        PropertyStem descriptor = eventType.getIndexedProperty(propertyNameAtomic);
        if (descriptor != null) {
            return new GenericPropertyDesc(descriptor.getReturnType());
        }

        // Check if this is an method returning array which is a type of simple property
        descriptor = eventType.getSimpleProperty(propertyNameAtomic);
        if (descriptor == null) {
            return null;
        }

        Class returnType = descriptor.getReturnType();
        if (returnType.isArray()) {
            return new GenericPropertyDesc(returnType.getComponentType());
        } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
            if (descriptor.getReadMethod() != null) {
                Class genericType = JavaClassHelper.getGenericReturnType(descriptor.getReadMethod(), false);
                return new GenericPropertyDesc(genericType);
            } else if (descriptor.getAccessorField() != null) {
                Class genericType = JavaClassHelper.getGenericFieldType(descriptor.getAccessorField(), false);
                return new GenericPropertyDesc(genericType);
            } else {
                return null;
            }
        }
        return null;
    }

    public Class getPropertyType(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory) {
        PropertyStem descriptor = eventType.getIndexedProperty(propertyNameAtomic);
        if (descriptor != null) {
            return descriptor.getReturnType();
        }

        // Check if this is an method returning array which is a type of simple property
        descriptor = eventType.getSimpleProperty(propertyNameAtomic);
        if (descriptor == null) {
            return null;
        }

        Class returnType = descriptor.getReturnType();
        if (returnType.isArray()) {
            return returnType.getComponentType();
        } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
            if (descriptor.getReadMethod() != null) {
                return JavaClassHelper.getGenericReturnType(descriptor.getReadMethod(), false);
            } else if (descriptor.getAccessorField() != null) {
                return JavaClassHelper.getGenericFieldType(descriptor.getAccessorField(), false);
            } else {
                return null;
            }
        }
        return null;
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, BeanEventTypeFactory beanEventTypeFactory) {
        Object type = optionalMapPropTypes.get(propertyNameAtomic);
        if (type == null) {
            return null;
        }
        if (type instanceof TypeBeanOrUnderlying[]) {
            EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
            if (!(innerType instanceof MapEventType)) {
                return null;
            }
            return Map[].class;
        } else {
            if (!(type instanceof Class)) {
                return null;
            }
            if (!((Class) type).isArray()) {
                return null;
            }
            return ((Class) type).getComponentType();
        }
    }

    public MapEventPropertyGetterAndIndexed getGetterMap(Map optionalMapPropTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        Object type = optionalMapPropTypes.get(propertyNameAtomic);
        if (type == null) {
            return null;
        }
        if (type instanceof TypeBeanOrUnderlying[]) {
            EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
            if (!(innerType instanceof MapEventType)) {
                return null;
            }
            return new MapArrayPropertyGetter(this.propertyNameAtomic, index, eventBeanTypedEventFactory, innerType);
        } else {
            if (!(type instanceof Class)) {
                return null;
            }
            if (!((Class) type).isArray()) {
                return null;
            }
            Class componentType = ((Class) type).getComponentType();
            // its an array
            return new MapArrayPOJOEntryIndexedPropertyGetter(propertyNameAtomic, index, eventBeanTypedEventFactory, beanEventTypeFactory, componentType);
        }
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
        writer.append("[");
        writer.append(Integer.toString(index));
        writer.append("]");
    }

    public EventPropertyGetterSPI getGetterDOM() {
        return new DOMIndexedGetter(propertyNameAtomic, index, null);
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BaseXMLEventType eventType, String propertyExpression) {
        for (SchemaElementSimple simple : complexProperty.getSimpleElements()) {
            if (!simple.isArray()) {
                continue;
            }
            if (!simple.getName().equals(propertyNameAtomic)) {
                continue;
            }
            return new DOMIndexedGetter(propertyNameAtomic, index, null);
        }

        for (SchemaElementComplex complex : complexProperty.getChildren()) {
            if (!complex.isArray()) {
                continue;
            }
            if (!complex.getName().equals(propertyNameAtomic)) {
                continue;
            }
            return new DOMIndexedGetter(propertyNameAtomic, index, new FragmentFactoryDOMGetter(eventBeanTypedEventFactory, eventType, propertyExpression));
        }

        return null;
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty) {
        for (SchemaElementSimple simple : complexProperty.getSimpleElements()) {
            if (!simple.isArray()) {
                continue;
            }
            if (!simple.getName().equals(propertyNameAtomic)) {
                continue;
            }

            // return the simple as a non-array since an index is provided
            return new SchemaElementSimple(simple.getName(), simple.getNamespace(), simple.getXsSimpleType(), simple.getTypeName(), false, simple.getFractionDigits());
        }

        for (SchemaElementComplex complex : complexProperty.getChildren()) {
            if (!complex.isArray()) {
                continue;
            }
            if (!complex.getName().equals(propertyNameAtomic)) {
                continue;
            }

            // return the complex as a non-array since an index is provided
            return new SchemaElementComplex(complex.getName(), complex.getNamespace(), complex.getAttributes(), complex.getChildren(), complex.getSimpleElements(), false, complex.getOptionalSimpleType(), complex.getOptionalSimpleTypeName());
        }

        return null;
    }

    /**
     * Returns the index number for an indexed property expression.
     *
     * @param propertyName property expression
     * @return index
     */
    public static Integer getIndex(String propertyName) {
        int start = propertyName.indexOf('[');
        int end = propertyName.indexOf(']');
        String indexStr = propertyName.substring(start, end);
        return Integer.parseInt(indexStr);
    }

    public ObjectArrayEventPropertyGetterAndIndexed getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        if (propertyIndex == null) {
            return null;
        }
        Object type = nestableTypes.get(getPropertyNameAtomic());
        if (type == null) {
            return null;
        }
        if (type instanceof TypeBeanOrUnderlying[]) {
            EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
            if (!(innerType instanceof ObjectArrayEventType)) {
                return null;
            }
            return new ObjectArrayArrayPropertyGetter(propertyIndex, index, eventBeanTypedEventFactory, innerType);
        } else {
            if (!(type instanceof Class)) {
                return null;
            }
            if (!((Class) type).isArray()) {
                return null;
            }
            Class componentType = ((Class) type).getComponentType();
            // its an array
            return new ObjectArrayArrayPOJOEntryIndexedPropertyGetter(propertyIndex, index, eventBeanTypedEventFactory, beanEventTypeFactory, componentType);
        }
    }
}
