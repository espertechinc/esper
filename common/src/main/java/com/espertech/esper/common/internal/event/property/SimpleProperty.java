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
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayPropertyGetterDefaultObjectArray;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventPropertyType;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.map.MapEventBeanPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapPropertyGetterDefaultNoFragment;
import com.espertech.esper.common.internal.event.xml.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents a simple property of a given name.
 */
public class SimpleProperty extends PropertyBase implements PropertySimple {
    /**
     * Ctor.
     *
     * @param propertyName is the property name
     */
    public SimpleProperty(String propertyName) {
        super(propertyName);
    }

    public String[] toPropertyArray() {
        return new String[]{this.getPropertyNameAtomic()};
    }

    public EventPropertyGetterSPI getGetter(BeanEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        PropertyStem propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }
        if (!propertyDesc.getPropertyType().equals(EventPropertyType.SIMPLE)) {
            return null;
        }
        return eventType.getGetterSPI(propertyNameAtomic);
    }

    public Class getPropertyType(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory) {
        PropertyStem propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }
        return propertyDesc.getReturnType();
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, BeanEventTypeFactory beanEventTypeFactory) {
        PropertyStem propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }
        return propertyDesc.getReturnTypeGeneric();
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, BeanEventTypeFactory beanEventTypeFactory) {
        // The simple, none-dynamic property needs a definition of the map contents else no property
        if (optionalMapPropTypes == null) {
            return null;
        }
        Object def = optionalMapPropTypes.get(propertyNameAtomic);
        if (def == null) {
            return null;
        }
        if (def instanceof Class) {
            return (Class) def;
        } else if (def instanceof Map) {
            return Map.class;
        } else if (def instanceof TypeBeanOrUnderlying) {
            EventType eventType = ((TypeBeanOrUnderlying) def).getEventType();
            return eventType.getUnderlyingType();
        } else if (def instanceof TypeBeanOrUnderlying[]) {
            EventType eventType = ((TypeBeanOrUnderlying[]) def)[0].getEventType();
            return JavaClassHelper.getArrayType(eventType.getUnderlyingType());
        } else if (def instanceof EventType) {
            EventType eventType = (EventType) def;
            return eventType.getUnderlyingType();
        } else if (def instanceof EventType[]) {
            EventType[] eventType = (EventType[]) def;
            return JavaClassHelper.getArrayType(eventType[0].getUnderlyingType());
        }
        String message = "Nestable map type configuration encountered an unexpected value type of '"
                + def.getClass() + "' for property '" + propertyNameAtomic + "', expected Map or Class";
        throw new PropertyAccessException(message);
    }

    public MapEventPropertyGetter getGetterMap(Map optionalMapPropTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        // The simple, none-dynamic property needs a definition of the map contents else no property
        if (optionalMapPropTypes == null) {
            return null;
        }
        Object def = optionalMapPropTypes.get(propertyNameAtomic);
        if (def == null) {
            return null;
        }
        if (def instanceof EventType) {
            return new MapEventBeanPropertyGetter(propertyNameAtomic, ((EventType) def).getUnderlyingType());
        }
        return new MapPropertyGetterDefaultNoFragment(propertyNameAtomic, eventBeanTypedEventFactory);
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
    }

    public EventPropertyGetterSPI getGetterDOM() {
        return new DOMAttributeAndElementGetter(propertyNameAtomic);
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex complexProperty, EventBeanTypedEventFactory eventBeanTypedEventFactory, BaseXMLEventType xmlEventType, String propertyExpression) {
        for (SchemaItemAttribute attribute : complexProperty.getAttributes()) {
            if (attribute.getName().equals(propertyNameAtomic)) {
                return new DOMSimpleAttributeGetter(propertyNameAtomic);
            }
        }

        for (SchemaElementSimple simple : complexProperty.getSimpleElements()) {
            if (simple.getName().equals(propertyNameAtomic)) {
                return new DOMComplexElementGetter(propertyNameAtomic, null, simple.isArray());
            }
        }

        for (SchemaElementComplex complex : complexProperty.getChildren()) {
            FragmentFactoryDOMGetter complexFragmentFactory = new FragmentFactoryDOMGetter(eventBeanTypedEventFactory, xmlEventType, propertyExpression);
            if (complex.getName().equals(propertyNameAtomic)) {
                return new DOMComplexElementGetter(propertyNameAtomic, complexFragmentFactory, complex.isArray());
            }
        }

        return null;
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty) {
        return SchemaUtil.findPropertyMapping(complexProperty, propertyNameAtomic);
    }

    public boolean isDynamic() {
        return false;
    }

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        // The simple, none-dynamic property needs a definition of the map contents else no property
        if (nestableTypes == null) {
            return null;
        }
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        if (propertyIndex == null) {
            return null;
        }
        return new ObjectArrayPropertyGetterDefaultObjectArray(propertyIndex, null, eventBeanTypedEventFactory);
    }
}
