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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import junit.framework.TestCase;

import java.lang.reflect.Method;

import static com.espertech.esper.common.client.type.EPTypePremade.*;

public class TestMethodResolver extends TestCase {
    public void testResolveMethodStaticOnly() throws Exception {
        Class declClass = Math.class;
        String methodName = "max";
        Class[] args = new Class[]{int.class, int.class};
        EPTypeClass[] ptypes = new EPTypeClass[]{INTEGERPRIMITIVE.getEPType(), INTEGERPRIMITIVE.getEPType()};
        Method expected = Math.class.getMethod(methodName, args);
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null));

        args = new Class[]{long.class, long.class};
        expected = Math.class.getMethod(methodName, args);
        ptypes = new EPTypeClass[]{INTEGERPRIMITIVE.getEPType(), LONGPRIMITIVE.getEPType()};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null));

        args = new Class[]{int.class, int.class};
        expected = Math.class.getMethod(methodName, args);
        ptypes = new EPTypeClass[]{INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType()};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null));

        args = new Class[]{long.class, long.class};
        expected = Math.class.getMethod(methodName, args);
        ptypes = new EPTypeClass[]{INTEGERBOXED.getEPType(), LONGBOXED.getEPType()};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null));

        args = new Class[]{float.class, float.class};
        expected = Math.class.getMethod(methodName, args);
        ptypes = new EPTypeClass[]{INTEGERBOXED.getEPType(), FLOATBOXED.getEPType()};
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null));

        declClass = System.class;
        methodName = "currentTimeMillis";
        args = new Class[0];
        expected = System.class.getMethod(methodName, args);
        ptypes = new EPTypeClass[0];
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null));
    }

    public void testResolveMethodStaticAndInstance() throws Exception {
        Class declClass = Math.class;
        String methodName = "max";
        Class[] args = new Class[]{int.class, int.class};
        EPTypeClass[] ptypes = new EPTypeClass[]{INTEGERPRIMITIVE.getEPType(), INTEGERPRIMITIVE.getEPType()};
        Method expected = Math.class.getMethod(methodName, args);
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, true, null, null));

        declClass = String.class;
        methodName = "trim";
        args = new Class[0];
        expected = String.class.getMethod(methodName, args);
        ptypes = new EPTypeClass[0];
        assertEquals(expected, MethodResolver.resolveMethod(declClass, methodName, ptypes, true, null, null));
    }

    public void testResolveMethodNotFound() throws Exception {
        boolean[] allowEventBeanType = new boolean[10];
        Class declClass = String.class;
        String methodName = "trim";
        EPTypeClass[] ptypes = null;
        try {
            MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }

        declClass = Math.class;
        methodName = "moox";
        ptypes = new EPTypeClass[]{INTEGERPRIMITIVE.getEPType(), INTEGERPRIMITIVE.getEPType()};
        try {
            MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }

        methodName = "max";
        ptypes = new EPTypeClass[]{BOOLEANPRIMITIVE.getEPType(), BOOLEANPRIMITIVE.getEPType()};
        try {
            MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }

        methodName = "max";
        ptypes = new EPTypeClass[]{INTEGERPRIMITIVE.getEPType(), INTEGERPRIMITIVE.getEPType(), BOOLEANPRIMITIVE.getEPType()};
        try {
            MethodResolver.resolveMethod(declClass, methodName, ptypes, false, null, null);
            fail();
        } catch (MethodResolverNoSuchMethodException e) {
            // Expected
        }
    }
}
