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

import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.arr.ObjectArrayArrayPOJOEntryIndexedPropertyGetter;
import com.espertech.esper.event.arr.ObjectArrayArrayPropertyGetter;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetterAndIndexed;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.bean.*;
import com.espertech.esper.event.map.MapArrayPOJOEntryIndexedPropertyGetter;
import com.espertech.esper.event.map.MapArrayPropertyGetter;
import com.espertech.esper.event.map.MapEventPropertyGetterAndIndexed;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.xml.*;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

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

    public EventPropertyGetterAndIndexed getGetter(BeanEventType eventType, EventAdapterService eventAdapterService) {
        FastClass fastClass = eventType.getFastClass();
        InternalEventPropDescriptor propertyDesc = eventType.getIndexedProperty(propertyNameAtomic);
        if (propertyDesc != null) {
            if (fastClass != null) {
                Method method = propertyDesc.getReadMethod();
                FastMethod fastMethod = fastClass.getMethod(method);
                return new KeyedFastPropertyGetter(fastMethod, index, eventAdapterService);
            } else {
                return new KeyedMethodPropertyGetter(propertyDesc.getReadMethod(), index, eventAdapterService);
            }
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
                if (fastClass != null) {
                    FastMethod fastMethod = fastClass.getMethod(method);
                    return new ArrayFastPropertyGetter(fastMethod, index, eventAdapterService);
                } else {
                    return new ArrayMethodPropertyGetter(method, index, eventAdapterService);
                }
            } else {
                Field field = propertyDesc.getAccessorField();
                return new ArrayFieldPropertyGetter(field, index, eventAdapterService);
            }
        } else if (JavaClassHelper.isImplementsInterface(returnType, List.class)) {
            if (propertyDesc.getReadMethod() != null) {
                Method method = propertyDesc.getReadMethod();
                if (fastClass != null) {
                    FastMethod fastMethod = fastClass.getMethod(method);
                    return new ListFastPropertyGetter(method, fastMethod, index, eventAdapterService);
                } else {
                    return new ListMethodPropertyGetter(method, index, eventAdapterService);
                }
            } else {
                Field field = propertyDesc.getAccessorField();
                return new ListFieldPropertyGetter(field, index, eventAdapterService);
            }
        } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
            if (propertyDesc.getReadMethod() != null) {
                Method method = propertyDesc.getReadMethod();
                if (fastClass != null) {
                    FastMethod fastMethod = fastClass.getMethod(method);
                    return new IterableFastPropertyGetter(method, fastMethod, index, eventAdapterService);
                } else {
                    return new IterableMethodPropertyGetter(method, index, eventAdapterService);
                }
            } else {
                Field field = propertyDesc.getAccessorField();
                return new IterableFieldPropertyGetter(field, index, eventAdapterService);
            }
        }

        return null;
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor descriptor = eventType.getIndexedProperty(propertyNameAtomic);
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

    public Class getPropertyType(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor descriptor = eventType.getIndexedProperty(propertyNameAtomic);
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

    public Class getPropertyTypeMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        Object type = optionalMapPropTypes.get(propertyNameAtomic);
        if (type == null) {
            return null;
        }
        if (type instanceof String) {
            // resolve a property that is a map event type
            String nestedName = type.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
            if (isArray) {
                nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
            }
            EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
            if (!(innerType instanceof MapEventType)) {
                return null;
            }
            if (!isArray) {
                return null; // must be declared as an index to use array notation
            } else {
                return Map[].class;
            }
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

    public MapEventPropertyGetterAndIndexed getGetterMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        Object type = optionalMapPropTypes.get(propertyNameAtomic);
        if (type == null) {
            return null;
        }
        if (type instanceof String) {
            // resolve a property that is a map event type
            String nestedName = type.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
            if (isArray) {
                nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
            }
            EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
            if (!(innerType instanceof MapEventType)) {
                return null;
            }
            if (!isArray) {
                return null; // must be declared as an array to use an indexed notation
            } else {
                return new MapArrayPropertyGetter(this.propertyNameAtomic, index, eventAdapterService, innerType);
            }
        } else {
            if (!(type instanceof Class)) {
                return null;
            }
            if (!((Class) type).isArray()) {
                return null;
            }
            Class componentType = ((Class) type).getComponentType();
            // its an array
            return new MapArrayPOJOEntryIndexedPropertyGetter(propertyNameAtomic, index, eventAdapterService, componentType);
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

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService, BaseXMLEventType eventType, String propertyExpression) {
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
            return new DOMIndexedGetter(propertyNameAtomic, index, new FragmentFactoryDOMGetter(eventAdapterService, eventType, propertyExpression));
        }

        return null;
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService) {
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

    public ObjectArrayEventPropertyGetterAndIndexed getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventAdapterService eventAdapterService) {
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        if (propertyIndex == null) {
            return null;
        }
        Object type = nestableTypes.get(getPropertyNameAtomic());
        if (type == null) {
            return null;
        }
        if (type instanceof String) {
            // resolve a property that is a map event type
            String nestedName = type.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
            if (isArray) {
                nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
            }
            EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
            if (!(innerType instanceof ObjectArrayEventType)) {
                return null;
            }
            if (!isArray) {
                return null; // must be declared as an array to use an indexed notation
            } else {
                return new ObjectArrayArrayPropertyGetter(propertyIndex, index, eventAdapterService, innerType);
            }
        } else {
            if (!(type instanceof Class)) {
                return null;
            }
            if (!((Class) type).isArray()) {
                return null;
            }
            Class componentType = ((Class) type).getComponentType();
            // its an array
            return new ObjectArrayArrayPOJOEntryIndexedPropertyGetter(propertyIndex, index, eventAdapterService, componentType);
        }
    }
}
