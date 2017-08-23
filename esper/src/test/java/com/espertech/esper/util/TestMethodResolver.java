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
package com.espertech.esper.util;

import junit.framework.TestCase;

import java.lang.reflect.Method;

public class TestMethodResolver extends TestCase {
    public void testResolveMethodStaticOnly() throws Exception {
        Class declClass = Math.class;
        String methodName = "max";
        Class[] args = new Class[]{int.class, int.class};
        Method expected = Math.class.getMethod(methodName, args);
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, false, null, null));

        args = new Class[]{long.class, long.class};
        expected = Math.class.getMethod(methodName, args);
        args = new Class[]{int.class, long.class};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, false, null, null));

        args = new Class[]{int.class, int.class};
        expected = Math.class.getMethod(methodName, args);
        args = new Class[]{Integer.class, Integer.class};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, false, null, null));

        args = new Class[]{long.class, long.class};
        expected = Math.class.getMethod(methodName, args);
        args = new Class[]{Integer.class, Long.class};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, false, null, null));

        args = new Class[]{float.class, float.class};
        expected = Math.class.getMethod(methodName, args);
        args = new Class[]{Integer.class, Float.class};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, false, null, null));

        declClass = System.class;
        methodName = "currentTimeMillis";
        args = new Class[0];
        expected = System.class.getMethod(methodName, args);
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, false, null, null));
    }

    public void testResolveMethodStaticAndInstance() throws Exception {
        boolean[] allowEventBeanType = new boolean[10];
        Class declClass = Math.class;
        String methodName = "max";
        Class[] args = new Class[]{int.class, int.class};
        Method expected = Math.class.getMethod(methodName, args);
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, true, null, null));

        declClass = String.class;
        methodName = "trim";
        args = new Class[0];
        expected = String.class.getMethod(methodName, args);
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, args, true, null, null));
    }

    public void testResolveMethodNotFound() throws Exception {
        boolean[] allowEventBeanType = new boolean[10];
        Class declClass = String.class;
        String methodName = "trim";
        Class[] args = null;
        try {
            MethodResolver.resolveMethod(declClass, methodName, args, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }

        declClass = Math.class;
        methodName = "moox";
        args = new Class[]{int.class, int.class};
        try {
            MethodResolver.resolveMethod(declClass, methodName, args, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }

        methodName = "max";
        args = new Class[]{boolean.class, boolean.class};
        try {
            MethodResolver.resolveMethod(declClass, methodName, args, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }

        methodName = "max";
        args = new Class[]{int.class, int.class, boolean.class};
        try {
            MethodResolver.resolveMethod(declClass, methodName, args, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }
    }
}
