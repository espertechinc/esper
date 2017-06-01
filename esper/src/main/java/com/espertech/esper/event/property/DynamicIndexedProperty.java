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
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.event.arr.ObjectArrayIndexedPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.DynamicIndexedPropertyGetter;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.map.MapIndexedPropertyGetter;
import com.espertech.esper.event.xml.BaseXMLEventType;
import com.espertech.esper.event.xml.DOMIndexedGetter;
import com.espertech.esper.event.xml.SchemaElementComplex;
import com.espertech.esper.event.xml.SchemaItem;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents a dynamic indexed property of a given name.
 * <p>
 * Dynamic properties always exist, have an Object type and are resolved to a method during runtime.
 */
public class DynamicIndexedProperty extends PropertyBase implements DynamicProperty, PropertyWithIndex {
    private final int index;

    /**
     * Ctor.
     *
     * @param propertyName is the property name
     * @param index        is the index of the array or indexed property
     */
    public DynamicIndexedProperty(String propertyName, int index) {
        super(propertyName);
        this.index = index;
    }

    public boolean isDynamic() {
        return true;
    }

    public String[] toPropertyArray() {
        return new String[]{this.getPropertyNameAtomic()};
    }

    public EventPropertyGetterSPI getGetter(BeanEventType eventType, EventAdapterService eventAdapterService) {
        return new DynamicIndexedPropertyGetter(propertyNameAtomic, index, eventAdapterService);
    }

    public Class getPropertyType(BeanEventType eventType, EventAdapterService eventAdapterService) {
        return Object.class;
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType beanEventType, EventAdapterService eventAdapterService) {
        return GenericPropertyDesc.getObjectGeneric();
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        return Object.class;
    }

    public MapEventPropertyGetter getGetterMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        return new MapIndexedPropertyGetter(this.getPropertyNameAtomic(), index);
    }

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventAdapterService eventAdapterService) {
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        return propertyIndex == null ? null : new ObjectArrayIndexedPropertyGetter(propertyIndex, index);
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
        writer.append('[');
        writer.append(Integer.toString(index));
        writer.append(']');
        writer.append('?');
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService, BaseXMLEventType eventType, String propertyExpression) {
        return new DOMIndexedGetter(propertyNameAtomic, index, null);
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService) {
        return null;  // dynamic properties always return Node
    }

    public EventPropertyGetterSPI getGetterDOM() {
        return new DOMIndexedGetter(propertyNameAtomic, index, null);
    }

    public int getIndex() {
        return index;
    }
}
