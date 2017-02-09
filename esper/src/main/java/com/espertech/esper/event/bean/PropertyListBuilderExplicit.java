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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.event.EventPropertyType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Introspector that considers explicitly configured event properties only.
 */
public class PropertyListBuilderExplicit implements PropertyListBuilder {
    private ConfigurationEventTypeLegacy legacyConfig;

    /**
     * Ctor.
     *
     * @param legacyConfig is a legacy type specification containing
     *                     information about explicitly configured fields and methods
     */
    public PropertyListBuilderExplicit(ConfigurationEventTypeLegacy legacyConfig) {
        if (legacyConfig == null) {
            throw new IllegalArgumentException("Required configuration not passed");
        }
        this.legacyConfig = legacyConfig;
    }

    public List<InternalEventPropDescriptor> assessProperties(Class clazz) {
        List<InternalEventPropDescriptor> result = new LinkedList<InternalEventPropDescriptor>();
        getExplicitProperties(result, clazz, legacyConfig);
        return result;
    }

    /**
     * Populates explicitly-defined properties into the result list.
     *
     * @param result       is the resulting list of event property descriptors
     * @param clazz        is the class to introspect
     * @param legacyConfig supplies specification of explicit methods and fields to expose
     */
    protected static void getExplicitProperties(List<InternalEventPropDescriptor> result,
                                                Class clazz,
                                                ConfigurationEventTypeLegacy legacyConfig) {
        for (ConfigurationEventTypeLegacy.LegacyFieldPropDesc desc : legacyConfig.getFieldProperties()) {
            result.add(makeDesc(clazz, desc));
        }
        for (ConfigurationEventTypeLegacy.LegacyMethodPropDesc desc : legacyConfig.getMethodProperties()) {
            result.add(makeDesc(clazz, desc));
        }
    }

    private static InternalEventPropDescriptor makeDesc(Class clazz, ConfigurationEventTypeLegacy.LegacyMethodPropDesc methodDesc) {
        Method[] methods = clazz.getMethods();
        Method method = null;
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().equals(methodDesc.getAccessorMethodName())) {
                continue;
            }
            if (methods[i].getReturnType() == void.class) {
                continue;
            }
            if (methods[i].getParameterTypes().length >= 2) {
                continue;
            }
            if (methods[i].getParameterTypes().length == 0) {
                method = methods[i];
                break;
            }

            Class parameterType = methods[i].getParameterTypes()[0];
            if ((parameterType != int.class) && ((parameterType != Integer.class)) &&
                    (parameterType != String.class)) {
                continue;
            }

            method = methods[i];
            break;
        }

        if (method == null) {
            throw new ConfigurationException("Configured method named '" +
                    methodDesc.getAccessorMethodName() + "' not found for class " + clazz.getName());
        }

        return makeMethodDesc(method, methodDesc.getName());
    }

    private static InternalEventPropDescriptor makeDesc(Class clazz, ConfigurationEventTypeLegacy.LegacyFieldPropDesc fieldDesc) {
        Field field;
        try {
            field = clazz.getField(fieldDesc.getAccessorFieldName());
        } catch (NoSuchFieldException ex) {
            throw new ConfigurationException("Configured field named '" +
                    fieldDesc.getAccessorFieldName() + "' not found for class " + clazz.getName());
        }
        return makeFieldDesc(field, fieldDesc.getName());
    }

    /**
     * Makes a simple-type event property descriptor based on a reflected field.
     *
     * @param field is the public field
     * @param name  is the name of the event property
     * @return property descriptor
     */
    protected static InternalEventPropDescriptor makeFieldDesc(Field field, String name) {
        return new InternalEventPropDescriptor(name, field, EventPropertyType.SIMPLE);
    }

    /**
     * Makes an event property descriptor based on a reflected method, considering
     * the methods parameters to determine if this is an indexed or mapped event property.
     *
     * @param method is the public method
     * @param name   is the name of the event property
     * @return property descriptor
     */
    protected static InternalEventPropDescriptor makeMethodDesc(Method method, String name) {
        EventPropertyType propertyType;

        if (method.getParameterTypes().length == 1) {
            Class parameterType = method.getParameterTypes()[0];
            if (parameterType == String.class) {
                propertyType = EventPropertyType.MAPPED;
            } else {
                propertyType = EventPropertyType.INDEXED;
            }
        } else {
            propertyType = EventPropertyType.SIMPLE;
        }

        return new InternalEventPropDescriptor(name, method, propertyType);
    }
}
