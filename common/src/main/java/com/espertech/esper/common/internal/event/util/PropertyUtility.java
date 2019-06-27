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
package com.espertech.esper.common.internal.event.util;

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PropertyUtility {
    public static PropertyAccessException getIllegalAccessException(Field field, IllegalAccessException e) {
        return getAccessExceptionField(field, e);
    }

    public static PropertyAccessException getIllegalArgumentException(Field field, IllegalArgumentException e) {
        return getAccessExceptionField(field, e);
    }

    private static PropertyAccessException getAccessExceptionField(Field field, Exception e) {
        Class declaring = field.getDeclaringClass();
        String message = "Failed to obtain field value for field " + field.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + e.getMessage();
        throw new PropertyAccessException(message, e);
    }

    private static PropertyAccessException getMismatchException(Class declared, Object object, ClassCastException e) {
        String classNameExpected = JavaClassHelper.getClassNameFullyQualPretty(declared);
        String classNameReceived;
        if (object != null) {
            classNameReceived = JavaClassHelper.getClassNameFullyQualPretty(object.getClass());
        } else {
            classNameReceived = "null";
        }

        if (classNameExpected.equals(classNameReceived)) {
            classNameExpected = JavaClassHelper.getClassNameFullyQualPrettyWithClassloader(declared);
            classNameReceived = object != null ? JavaClassHelper.getClassNameFullyQualPrettyWithClassloader(object.getClass()) : "null";
        }

        String message = "Mismatched getter instance to event bean type, expected " + classNameExpected + " but received " + classNameReceived;
        throw new PropertyAccessException(message, e);
    }

    public static PropertyAccessException getIllegalAccessException(Method method, IllegalAccessException e) {
        return getAccessExceptionMethod(method, e);
    }

    public static PropertyAccessException getIllegalArgumentException(Method method, IllegalArgumentException e) {
        return getAccessExceptionMethod(method, e);
    }

    private static PropertyAccessException getAccessExceptionMethod(Method method, Exception e) {
        Class declaring = method.getDeclaringClass();
        String message = "Failed to invoke method " + method.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + e.getMessage();
        throw new PropertyAccessException(message, e);
    }

    public static PropertyAccessException getMismatchException(Method method, Object object, ClassCastException e) {
        return getMismatchException(method.getDeclaringClass(), object, e);
    }

    public static PropertyAccessException getMismatchException(Field field, Object object, ClassCastException e) {
        return getMismatchException(field.getDeclaringClass(), object, e);
    }

    public static PropertyAccessException getInvocationTargetException(Method method, InvocationTargetException e) {
        Class declaring = method.getDeclaringClass();
        String message = "Failed to invoke method " + method.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + e.getTargetException().getMessage();
        throw new PropertyAccessException(message, e);
    }

    public static PropertyAccessException getGeneralException(Method method, Throwable t) {
        Class declaring = method.getDeclaringClass();
        String message = "Failed to invoke method " + method.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + t.getMessage();
        throw new PropertyAccessException(message, t);
    }

    public static PropertyAccessException getGeneralException(Field field, Throwable t) {
        Class declaring = field.getDeclaringClass();
        String message = "Failed to obtain field value for field " + field.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + t.getMessage();
        throw new PropertyAccessException(message, t);
    }
}
