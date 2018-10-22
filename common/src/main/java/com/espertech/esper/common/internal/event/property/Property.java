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

import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import com.espertech.esper.common.internal.event.xml.SchemaElementComplex;
import com.espertech.esper.common.internal.event.xml.SchemaItem;

import java.io.StringWriter;
import java.util.Map;

/**
 * Interface for a property of an event of type BeanEventType (JavaBean event). Properties are designed to
 * handle the different types of properties for such events: indexed, mapped, simple, nested, or a combination of
 * those.
 */
public interface Property {
    /**
     * Returns the property type.
     *
     * @param eventType            is the event type representing the JavaBean
     * @param beanEventTypeFactory bean factory
     * @return property type class
     */
    public Class getPropertyType(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory);

    /**
     * Returns the property type plus its generic type parameter, if any.
     *
     * @param eventType            is the event type representing the JavaBean
     * @param beanEventTypeFactory bean factory
     * @return type and generic descriptor
     */
    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory);

    /**
     * Returns value getter for the property of an event of the given event type.
     *
     * @param eventType                  is the type of event to make a getter for
     * @param eventBeanTypedEventFactory factory for event beans and event types
     * @param beanEventTypeFactory       bean factory
     * @return fast property value getter for property
     */
    public EventPropertyGetterSPI getGetter(BeanEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);

    /**
     * Returns the property type for use with Map event representations.
     *
     * @param optionalMapPropTypes a map-within-map type definition, if supplied, or null if not supplied
     * @param beanEventTypeFactory bean factory
     * @return property type @param optionalMapPropTypes
     */
    public Class getPropertyTypeMap(Map optionalMapPropTypes, BeanEventTypeFactory beanEventTypeFactory);

    /**
     * Returns the getter-method for use with Map event representations.
     *
     * @param optionalMapPropTypes       a map-within-map type definition, if supplied, or null if not supplied
     * @param eventBeanTypedEventFactory for resolving further map event types that are property types
     * @param beanEventTypeFactory       bean factory
     * @return getter for maps
     */
    public MapEventPropertyGetter getGetterMap(Map optionalMapPropTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory);

    /**
     * Returns the property type for use with DOM event representations.
     *
     * @param complexProperty a element-within-element type definition
     * @return property type
     */
    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty);

    /**
     * Returns the getter-method for use with XML DOM event representations.
     *
     * @param complexProperty            a element-within-element type definition
     * @param eventBeanTypedEventFactory for resolving or creating further event types that are property types
     * @param xmlEventType               the event type
     * @param propertyExpression         the full property expression
     * @return getter
     */
    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BaseXMLEventType xmlEventType, String propertyExpression);

    /**
     * Returns the getter-method for use with XML DOM event representations.
     *
     * @return getter
     */
    public EventPropertyGetterSPI getGetterDOM();

    /**
     * Write the EPL-representation of the property.
     *
     * @param writer to write to
     */
    public void toPropertyEPL(StringWriter writer);

    /**
     * Return a String-array of atomic property names.
     *
     * @return array of atomic names in a property expression
     */
    public String[] toPropertyArray();

    /**
     * Returns true for dynamic properties.
     *
     * @return false for not-dynamic properties, true for dynamic properties.
     */
    public boolean isDynamic();

    public String getPropertyNameAtomic();
}
