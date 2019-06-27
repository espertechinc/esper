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

import com.espertech.esper.common.internal.event.arr.ObjectArrayDynamicPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayPropertyGetterDefaultObjectArray;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.getter.DynamicSimplePropertyGetterByField;
import com.espertech.esper.common.internal.event.bean.getter.DynamicSimplePropertyGetterByMethod;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.map.MapDynamicPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import com.espertech.esper.common.internal.event.xml.DOMAttributeAndElementGetter;
import com.espertech.esper.common.internal.event.xml.SchemaElementComplex;
import com.espertech.esper.common.internal.event.xml.SchemaItem;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents a dynamic simple property of a given name.
 * <p>
 * Dynamic properties always exist, have an Object type and are resolved to a method during runtime.
 */
public class DynamicSimpleProperty extends PropertyBase implements DynamicProperty, PropertySimple {
    /**
     * Ctor.
     *
     * @param propertyName is the property name
     */
    public DynamicSimpleProperty(String propertyName) {
        super(propertyName);
    }

    public EventPropertyGetterSPI getGetter(BeanEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        if (!eventType.getStem().isPublicFields()) {
            return new DynamicSimplePropertyGetterByMethod(propertyNameAtomic, eventBeanTypedEventFactory, beanEventTypeFactory);
        }
        return new DynamicSimplePropertyGetterByField(propertyNameAtomic, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public boolean isDynamic() {
        return true;
    }

    public String[] toPropertyArray() {
        return new String[]{this.getPropertyNameAtomic()};
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
        return new MapDynamicPropertyGetter(propertyNameAtomic);
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BaseXMLEventType eventType, String propertyExpression) {
        return new DOMAttributeAndElementGetter(propertyNameAtomic);
    }

    public EventPropertyGetterSPI getGetterDOM() {
        return new DOMAttributeAndElementGetter(propertyNameAtomic);
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty) {
        return null;    // always returns Node
    }

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        // The simple, none-dynamic property needs a definition of the map contents else no property
        if (nestableTypes == null) {
            return new ObjectArrayDynamicPropertyGetter(propertyNameAtomic);
        }
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        if (propertyIndex == null) {
            return new ObjectArrayDynamicPropertyGetter(propertyNameAtomic);
        }
        return new ObjectArrayPropertyGetterDefaultObjectArray(propertyIndex, null, eventBeanTypedEventFactory);
    }
}
