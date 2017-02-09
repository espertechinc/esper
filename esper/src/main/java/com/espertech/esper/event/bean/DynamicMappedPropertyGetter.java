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

import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.vaevent.PropertyUtility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Getter for a dynamic mapped property (syntax field.mapped('key')?), using vanilla reflection.
 */
public class DynamicMappedPropertyGetter extends DynamicPropertyGetterBase {
    private final String getterMethodName;
    private final Object[] parameters;

    /**
     * Ctor.
     *
     * @param fieldName           property name
     * @param key                 mapped access key
     * @param eventAdapterService factory for event beans and event types
     */
    public DynamicMappedPropertyGetter(String fieldName, String key, EventAdapterService eventAdapterService) {
        super(eventAdapterService);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        this.parameters = new Object[]{key};
    }

    public Method determineMethod(Class clazz) throws PropertyAccessException {
        try {
            return clazz.getMethod(getterMethodName, String.class);
        } catch (NoSuchMethodException ex1) {
            Method method;
            try {
                method = clazz.getMethod(getterMethodName);
            } catch (NoSuchMethodException e) {
                return null;
            }

            if (method.getReturnType() != Map.class) {
                return null;
            }
            return method;
        }
    }

    protected Object call(DynamicPropertyDescriptor descriptor, Object underlying) {
        try {
            if (descriptor.isHasParameters()) {
                return descriptor.getMethod().invoke(underlying, parameters);
            } else {
                Object result = descriptor.getMethod().invoke(underlying, null);
                if ((result instanceof Map) && (result != null)) {
                    Map map = (Map) result;
                    return map.get(parameters[0]);
                }
                return null;
            }
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(descriptor.getMethod().getJavaMethod(), underlying, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(descriptor.getMethod().getJavaMethod(), e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getMethod().getJavaMethod(), e);
        }
    }
}
