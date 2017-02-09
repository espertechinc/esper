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

public class TestSimpleNumberCoercion extends TestCase {
    public void testGetCoercer() {
        assertEquals(1d, SimpleNumberCoercerFactory.getCoercer(null, Double.class).coerceBoxed(1d));
        assertEquals(1d, SimpleNumberCoercerFactory.getCoercer(Double.class, Double.class).coerceBoxed(1d));
        assertEquals(5d, SimpleNumberCoercerFactory.getCoercer(Integer.class, Double.class).coerceBoxed(5));
        assertEquals(6d, SimpleNumberCoercerFactory.getCoercer(Byte.class, Double.class).coerceBoxed((byte) 6));
        assertEquals(3f, SimpleNumberCoercerFactory.getCoercer(Long.class, Float.class).coerceBoxed((long) 3));
        assertEquals((short) 2, SimpleNumberCoercerFactory.getCoercer(Long.class, Short.class).coerceBoxed((long) 2));
        assertEquals(4, SimpleNumberCoercerFactory.getCoercer(Long.class, Integer.class).coerceBoxed((long) 4));
        assertEquals((byte) 5, SimpleNumberCoercerFactory.getCoercer(Long.class, Byte.class).coerceBoxed((long) 5));
        assertEquals(8l, SimpleNumberCoercerFactory.getCoercer(Long.class, Long.class).coerceBoxed((long) 8));
        assertEquals(BigInteger.valueOf(8), SimpleNumberCoercerFactory.getCoercer(int.class, BigInteger.class).coerceBoxed(8));
        assertEquals(new BigDecimal(9), SimpleNumberCoercerFactory.getCoercer(int.class, BigDecimal.class).coerceBoxed(9));
        assertEquals(new BigDecimal(9.0d), SimpleNumberCoercerFactory.getCoercer(double.class, BigDecimal.class).coerceBoxed(9.0));

        assertEquals(new BigDecimal(9.0d), SimpleNumberCoercerFactory.getCoercerBigDecimal(double.class).coerceBoxedBigDec(9.0));
        assertEquals(new BigDecimal(9), SimpleNumberCoercerFactory.getCoercerBigDecimal(long.class).coerceBoxedBigDec(9));
        assertEquals(BigDecimal.TEN, SimpleNumberCoercerFactory.getCoercerBigDecimal(BigDecimal.class).coerceBoxedBigDec(BigDecimal.TEN));

        assertEquals(BigInteger.valueOf(9), SimpleNumberCoercerFactory.getCoercerBigInteger(long.class).coerceBoxedBigInt(9));
        assertEquals(BigInteger.TEN, SimpleNumberCoercerFactory.getCoercerBigInteger(BigInteger.class).coerceBoxedBigInt(BigInteger.TEN));

        try {
            JavaClassHelper.coerceBoxed(10, int.class);
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }
}
