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

import com.espertech.esper.supportunit.util.SupportCtorInt;
import com.espertech.esper.supportunit.util.SupportCtorIntObjectArray;
import com.espertech.esper.supportunit.util.SupportCtorNone;
import com.espertech.esper.supportunit.util.SupportCtorObjectArray;
import junit.framework.TestCase;

public class TestConstructorHelper extends TestCase {
    public void testValidInvokeConstructor() throws Exception {
        Object[] parameters = new Object[]{"test", 1};
        SupportCtorObjectArray objOne = (SupportCtorObjectArray) ConstructorHelper.invokeConstructor(SupportCtorObjectArray.class, parameters);
        assertEquals(parameters, objOne.getArguments());

        SupportCtorInt objTwo = (SupportCtorInt) ConstructorHelper.invokeConstructor(SupportCtorInt.class, new Object[]{99});
        assertEquals(99, objTwo.getSomeValue());
        objTwo = (SupportCtorInt) ConstructorHelper.invokeConstructor(SupportCtorInt.class, new Object[]{new Integer(13)});
        assertEquals(13, objTwo.getSomeValue());

        SupportCtorIntObjectArray objThree = (SupportCtorIntObjectArray) ConstructorHelper.invokeConstructor(SupportCtorIntObjectArray.class, new Object[]{1});
        assertEquals(1, objThree.getSomeValue());
        objThree = (SupportCtorIntObjectArray) ConstructorHelper.invokeConstructor(SupportCtorIntObjectArray.class, parameters);
        assertEquals(parameters, objThree.getArguments());
    }

    public void testInvalidInvokeConstructor() throws Exception {
        // No Ctor
        try {
            ConstructorHelper.invokeConstructor(SupportCtorNone.class, new Object[0]);
            fail();
        } catch (NoSuchMethodException ex) {
            // Expected
        }

        // Not matching Ctor - number of params
        try {
            ConstructorHelper.invokeConstructor(SupportCtorInt.class, new Object[0]);
            fail();
        } catch (NoSuchMethodException ex) {
            // Expected
        }

        // Type not matching
        try {
            ConstructorHelper.invokeConstructor(SupportCtorInt.class, new Object[]{"a"});
            fail();
        } catch (NoSuchMethodException ex) {
            // Expected
        }
    }
}
