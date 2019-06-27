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
package com.espertech.esper.common.internal.event.bean.core;

import java.lang.reflect.Method;

/**
 * Provides method information for dynamic (unchecked) properties of each class for use in obtaining property values.
 */
public class DynamicPropertyDescriptorByMethod {
    private Class clazz;
    private Method method;
    private boolean hasParameters;

    /**
     * Ctor.
     *
     * @param clazz         the class to match when looking for a method
     * @param method        the fast method to call
     * @param hasParameters true if the method takes parameters
     */
    public DynamicPropertyDescriptorByMethod(Class clazz, Method method, boolean hasParameters) {
        this.clazz = clazz;
        this.method = method;
        this.hasParameters = hasParameters;
    }

    /**
     * Returns the class for the method.
     *
     * @return class to match on
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the method to invoke.
     *
     * @return method to invoke
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns true if the method takes parameters.
     *
     * @return indicator if parameters are required
     */
    public boolean isHasParameters() {
        return hasParameters;
    }
}
