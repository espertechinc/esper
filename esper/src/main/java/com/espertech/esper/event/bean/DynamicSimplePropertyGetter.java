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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Getter for a dynamic property (syntax field.inner?), using vanilla reflection.
 */
public class DynamicSimplePropertyGetter extends DynamicPropertyGetterBase {
    private final String getterMethodName;
    private final String isMethodName;

    /**
     * Ctor.
     *
     * @param fieldName           the property name
     * @param eventAdapterService factory for event beans and event types
     */
    public DynamicSimplePropertyGetter(String fieldName, EventAdapterService eventAdapterService) {
        super(eventAdapterService);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        isMethodName = PropertyHelper.getIsMethodName(fieldName);
    }

    protected Object call(DynamicPropertyDescriptor descriptor, Object underlying) {
        try {
            return descriptor.getMethod().invoke(underlying, null);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(descriptor.getMethod().getJavaMethod(), underlying, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(descriptor.getMethod().getJavaMethod(), e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getMethod().getJavaMethod(), e);
        }
    }

    protected Method determineMethod(Class clazz) {
        try {
            return clazz.getMethod(getterMethodName);
        } catch (NoSuchMethodException ex1) {
            try {
                return clazz.getMethod(isMethodName);
            } catch (NoSuchMethodException ex2) {
                return null;
            }
        }
    }
}
