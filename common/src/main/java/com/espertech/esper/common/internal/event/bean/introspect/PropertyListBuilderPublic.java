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
package com.espertech.esper.common.internal.event.bean.introspect;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation for a property list builder that considers any public method
 * and public field as the exposed event properties, plus any explicitly configured props.
 */
public class PropertyListBuilderPublic implements PropertyListBuilder {
    private ConfigurationCommonEventTypeBean legacyConfig;

    /**
     * Ctor.
     *
     * @param legacyConfig configures legacy type
     */
    public PropertyListBuilderPublic(ConfigurationCommonEventTypeBean legacyConfig) {
        if (legacyConfig == null) {
            throw new IllegalArgumentException("Required configuration not passed");
        }
        this.legacyConfig = legacyConfig;
    }

    public List<PropertyStem> assessProperties(Class clazz) {
        List<PropertyStem> result = new LinkedList<PropertyStem>();
        PropertyListBuilderExplicit.getExplicitProperties(result, clazz, legacyConfig);
        addPublicFields(result, clazz);
        addPublicMethods(result, clazz);
        return result;
    }

    private static void addPublicMethods(List<PropertyStem> result, Class clazz) {
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

            PropertyStem desc = PropertyListBuilderExplicit.makeMethodDesc(methods[i], methods[i].getName());
            result.add(desc);
        }

        PropertyHelper.removeJavaProperties(result);
    }

    private static void addPublicFields(List<PropertyStem> result, Class clazz) {
        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; i++) {
            PropertyStem desc = PropertyListBuilderExplicit.makeFieldDesc(fields[i], fields[i].getName());
            result.add(desc);
        }
    }
}
