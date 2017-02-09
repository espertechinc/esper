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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyType;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.arr.ObjectArrayPropertyGetterDefaultObjectArray;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.InternalEventPropDescriptor;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.map.MapPropertyGetterDefaultNoFragment;
import com.espertech.esper.event.xml.*;

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

    public EventPropertyGetter getGetter(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }
        if (!propertyDesc.getPropertyType().equals(EventPropertyType.SIMPLE)) {
            return null;
        }
        return eventType.getGetter(propertyNameAtomic);
    }

    public Class getPropertyType(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }
        return propertyDesc.getReturnType();
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, EventAdapterService eventAdapterService) {
        InternalEventPropDescriptor propertyDesc = eventType.getSimpleProperty(propertyNameAtomic);
        if (propertyDesc == null) {
            return null;
        }
        return propertyDesc.getReturnTypeGeneric();
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
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
        } else if (def instanceof String) {
            String propertyName = def.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(propertyName);
            if (isArray) {
                propertyName = EventTypeUtility.getPropertyRemoveArray(propertyName);
            }

            EventType eventType = eventAdapterService.getExistsTypeByName(propertyName);
            if (eventType instanceof MapEventType) {
                if (isArray) {
                    return Map[].class;
                } else {
                    return Map.class;
                }
            }
            if (eventType instanceof ObjectArrayEventType) {
                if (isArray) {
                    return Object[][].class;
                } else {
                    return Object[].class;
                }
            }
        }
        String message = "Nestable map type configuration encountered an unexpected value type of '"
                + def.getClass() + " for property '" + propertyNameAtomic + "', expected Map or Class";
        throw new PropertyAccessException(message);
    }

    public MapEventPropertyGetter getGetterMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        // The simple, none-dynamic property needs a definition of the map contents else no property
        if (optionalMapPropTypes == null) {
            return null;
        }
        Object def = optionalMapPropTypes.get(propertyNameAtomic);
        if (def == null) {
            return null;
        }
        return new MapPropertyGetterDefaultNoFragment(propertyNameAtomic, eventAdapterService);
    }

    public void toPropertyEPL(StringWriter writer) {
        writer.append(propertyNameAtomic);
    }

    public EventPropertyGetter getGetterDOM() {
        return new DOMAttributeAndElementGetter(propertyNameAtomic);
    }

    public EventPropertyGetter getGetterDOM(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService, BaseXMLEventType xmlEventType, String propertyExpression) {
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
            FragmentFactoryDOMGetter complexFragmentFactory = new FragmentFactoryDOMGetter(eventAdapterService, xmlEventType, propertyExpression);
            if (complex.getName().equals(propertyNameAtomic)) {
                return new DOMComplexElementGetter(propertyNameAtomic, complexFragmentFactory, complex.isArray());
            }
        }

        return null;
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex complexProperty, EventAdapterService eventAdapterService) {
        return SchemaUtil.findPropertyMapping(complexProperty, propertyNameAtomic);
    }

    public boolean isDynamic() {
        return false;
    }

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventAdapterService eventAdapterService) {
        // The simple, none-dynamic property needs a definition of the map contents else no property
        if (nestableTypes == null) {
            return null;
        }
        Integer propertyIndex = indexPerProperty.get(propertyNameAtomic);
        if (propertyIndex == null) {
            return null;
        }
        return new ObjectArrayPropertyGetterDefaultObjectArray(propertyIndex, null, eventAdapterService);
    }
}
