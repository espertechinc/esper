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

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestSimpleTypeCasterFactory extends TestCase {
    public void testGetCaster() throws Exception {
        Object[][] tests = new Object[][]{
                {long.class, 10, 10L},
                {double.class, 1, 1d},
                {int.class, 0x1, 1},
                {float.class, 100, 100f},
                {Integer.class, (short) 2, 2},
                {Byte.class, (short) 2, (byte) 2},
                {short.class, (long) 2, (short) 2},
                {char.class, 'a', 'a'},
        };

        for (int i = 0; i < tests.length; i++) {
            SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(null, (Class) tests[i][0]);
            assertEquals("error in row:" + i, tests[i][2], caster.cast(tests[i][1]));
        }

        assertEquals('A', SimpleTypeCasterFactory.getCaster(String.class, char.class).cast("ABC"));
        assertEquals(BigInteger.valueOf(100), SimpleTypeCasterFactory.getCaster(Long.class, BigInteger.class).cast(100L));
        assertEquals(new BigDecimal(100), SimpleTypeCasterFactory.getCaster(Long.class, BigDecimal.class).cast(100L));
        assertEquals(new BigDecimal(100d), SimpleTypeCasterFactory.getCaster(Double.class, BigDecimal.class).cast(100d));
    }
}
