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

import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.vaevent.PropertyUtility;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Getter for a dynamic indexed property (syntax field.indexed[0]?), using vanilla reflection.
 */
public class DynamicIndexedPropertyGetter extends DynamicPropertyGetterBase {
    private final String getterMethodName;
    private final Object[] parameters;
    private final int index;

    /**
     * Ctor.
     *
     * @param fieldName           property name
     * @param index               index to get the element at
     * @param eventAdapterService factory for event beans and event types
     */
    public DynamicIndexedPropertyGetter(String fieldName, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        this.parameters = new Object[]{index};
        this.index = index;
    }

    protected Method determineMethod(Class clazz) {
        Method method;

        try {
            return clazz.getMethod(getterMethodName, int.class);
        } catch (NoSuchMethodException ex1) {
            try {
                method = clazz.getMethod(getterMethodName);
            } catch (NoSuchMethodException e) {
                return null;
            }
            if (!method.getReturnType().isArray()) {
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
                Object array = descriptor.getMethod().invoke(underlying, null);
                if (array == null) {
                    return null;
                }
                if (Array.getLength(array) <= index) {
                    return null;
                }
                return Array.get(array, index);
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
