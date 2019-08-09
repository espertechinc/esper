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
package com.espertech.esper.common.client.hook.datetimemethod;

/**
 * Provides information about the public static method that implements the logic for the date-time method.
 */
public class DateTimeMethodModeStaticMethod implements DateTimeMethodMode {
    private final Class clazz;
    private final String methodName;

    /**
     * Ctor.
     * @param clazz class
     * @param methodName method
     */
    public DateTimeMethodModeStaticMethod(Class clazz, String methodName) {
        this.clazz = clazz;
        this.methodName = methodName;
    }

    /**
     * Returns the class
     * @return class
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the method
     * @return method
     */
    public String getMethodName() {
        return methodName;
    }
}
