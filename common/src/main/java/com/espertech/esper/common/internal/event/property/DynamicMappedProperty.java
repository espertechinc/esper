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
import com.espertech.esper.common.internal.event.arr.ObjectArrayMappedPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.getter.DynamicMappedPropertyGetterByField;
import com.espertech.esper.common.internal.event.bean.getter.DynamicMappedPropertyGetterByMethod;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapMappedPropertyGetter;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import com.espertech.esper.common.internal.event.xml.DOMMapGetter;
import com.espertech.esper.common.internal.event.xml.SchemaElementComplex;
import com.espertech.esper.common.internal.event.xml.SchemaItem;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents a dynamic mapped property of a given name.
 * <p>
 * Dynamic properties always exist, have an Object type and are resolved to a method during runtime.
 */
public class DynamicMappedProperty extends PropertyBase implements DynamicProperty, PropertyWithKey {
    private final String key;

    /**
     * Ctor.
     *
     * @param propertyName is the property name
     * @param key          is the mapped access key
     */
    public DynamicMappedProperty(String propertyName, String key) {
        super(propertyName);
        this.key = key;
    }

    public boolean isDynamic() {
        return true;
    }

    public String[] toPropertyArray() {
        return new String[]{this.getPropertyNameAtomic()};
    }

    public EventPropertyGetterSPI getGetter(BeanEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        if (!eventType.getStem().isPublicFields()) {
            return new DynamicMappedPropertyGetterByMethod(propertyNameAtomic, key, eventBeanTypedEventFactory, beanEventTypeFactory);
        }
        return new DynamicMappedPropertyGetterByField(propertyNameAtomic, key, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public Class getPropertyType(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory) {
        return Object.class;
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType beanEventType, BeanEventTypeFactory beanEventTypeFactory) {
        return GenericPropertyDesc.getObjectGeneric();
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, BeanEventTypeFactory beanEventTypeFactory) {
        return Object.class;
    }

    public MapEventPropertyGetter getGetterMap(Map optionalMapPropTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return new MapMappedPropertyGetter(this.getPropertyNameAtomic(), key);
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
        writer.append("('");
        writer.append(key);
        writer.append("')");
        writer.append('?');
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BaseXMLEventType eventType, String propertyExpression) {
        return new DOMMapGetter(propertyNameAtomic, key, null);
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty) {
        return null;  // always returns Node
    }

    public EventPropertyGetterSPI getGetterDOM() {
        return new DOMMapGetter(propertyNameAtomic, key, null);
    }

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        return propertyIndex == null ? null : new ObjectArrayMappedPropertyGetter(propertyIndex, key);
    }

    public String getKey() {
        return key;
    }
}
