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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation for a property list builder that considers any public method
 * and public field as the exposed event properties, plus any explicitly configured props.
 */
public class PropertyListBuilderPublic implements PropertyListBuilder {
    private ConfigurationEventTypeLegacy legacyConfig;

    /**
     * Ctor.
     *
     * @param legacyConfig configures legacy type
     */
    public PropertyListBuilderPublic(ConfigurationEventTypeLegacy legacyConfig) {
        if (legacyConfig == null) {
            throw new IllegalArgumentException("Required configuration not passed");
        }
        this.legacyConfig = legacyConfig;
    }

    public List<InternalEventPropDescriptor> assessProperties(Class clazz) {
        List<InternalEventPropDescriptor> result = new LinkedList<InternalEventPropDescriptor>();
        PropertyListBuilderExplicit.getExplicitProperties(result, clazz, legacyConfig);
        addPublicFields(result, clazz);
        addPublicMethods(result, clazz);
        return result;
    }

    private static void addPublicMethods(List<InternalEventPropDescriptor> result, Class clazz) {
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getReturnType() == void.class) {
                continue;
            }
            if (methods[i].getParameterTypes().length >= 2) {
                continue;
            }
            if (methods[i].getParameterTypes().length == 1) {
                Class parameterType = methods[i].getParameterTypes()[0];
                if ((parameterType != int.class) && ((parameterType != Integer.class)) &&
                        (parameterType != String.class)) {
                    continue;
                }
            }

            InternalEventPropDescriptor desc = PropertyListBuilderExplicit.makeMethodDesc(methods[i], methods[i].getName());
            result.add(desc);
        }

        PropertyHelper.removeJavaProperties(result);
    }

    private static void addPublicFields(List<InternalEventPropDescriptor> result, Class clazz) {
        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; i++) {
            InternalEventPropDescriptor desc = PropertyListBuilderExplicit.makeFieldDesc(fields[i], fields[i].getName());
            result.add(desc);
        }
    }
}
