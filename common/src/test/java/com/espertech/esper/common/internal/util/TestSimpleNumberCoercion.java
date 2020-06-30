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

import com.espertech.esper.common.client.type.EPTypePremade;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.client.type.EPTypePremade.*;

public class TestSimpleNumberCoercion extends TestCase {
    public void testGetCoercer() {
        assertEquals(1d, SimpleNumberCoercerFactory.getCoercer(null, DOUBLEBOXED.getEPType()).coerceBoxed(1d));
        assertEquals(1d, SimpleNumberCoercerFactory.getCoercer(DOUBLEBOXED.getEPType(), DOUBLEBOXED.getEPType()).coerceBoxed(1d));
        assertEquals(5d, SimpleNumberCoercerFactory.getCoercer(INTEGERBOXED.getEPType(), DOUBLEBOXED.getEPType()).coerceBoxed(5));
        assertEquals(6d, SimpleNumberCoercerFactory.getCoercer(BYTEBOXED.getEPType(), DOUBLEBOXED.getEPType()).coerceBoxed((byte) 6));
        assertEquals(3f, SimpleNumberCoercerFactory.getCoercer(LONGBOXED.getEPType(), FLOATBOXED.getEPType()).coerceBoxed((long) 3));
        assertEquals((short) 2, SimpleNumberCoercerFactory.getCoercer(LONGBOXED.getEPType(), SHORTBOXED.getEPType()).coerceBoxed((long) 2));
        assertEquals(4, SimpleNumberCoercerFactory.getCoercer(LONGBOXED.getEPType(), INTEGERBOXED.getEPType()).coerceBoxed((long) 4));
        assertEquals((byte) 5, SimpleNumberCoercerFactory.getCoercer(LONGBOXED.getEPType(), BYTEBOXED.getEPType()).coerceBoxed((long) 5));
        assertEquals(8l, SimpleNumberCoercerFactory.getCoercer(LONGBOXED.getEPType(), LONGBOXED.getEPType()).coerceBoxed((long) 8));
        assertEquals(BigInteger.valueOf(8), SimpleNumberCoercerFactory.getCoercer(INTEGERPRIMITIVE.getEPType(), BIGINTEGER.getEPType()).coerceBoxed(8));
        assertEquals(new BigDecimal(9), SimpleNumberCoercerFactory.getCoercer(INTEGERPRIMITIVE.getEPType(), BIGDECIMAL.getEPType()).coerceBoxed(9));
        assertEquals(new BigDecimal(9.0d), SimpleNumberCoercerFactory.getCoercer(DOUBLEPRIMITIVE.getEPType(), BIGDECIMAL.getEPType()).coerceBoxed(9.0));

        assertEquals(new BigDecimal(9.0d), SimpleNumberCoercerFactory.getCoercerBigDecimal(DOUBLEPRIMITIVE.getEPType()).coerceBoxedBigDec(9.0));
        assertEquals(new BigDecimal(9), SimpleNumberCoercerFactory.getCoercerBigDecimal(LONGPRIMITIVE.getEPType()).coerceBoxedBigDec(9));
        assertEquals(BigDecimal.TEN, SimpleNumberCoercerFactory.getCoercerBigDecimal(BIGDECIMAL.getEPType()).coerceBoxedBigDec(BigDecimal.TEN));

        assertEquals(BigInteger.valueOf(9), SimpleNumberCoercerFactory.getCoercerBigInteger(LONGPRIMITIVE.getEPType()).coerceBoxedBigInt(9));
        assertEquals(BigInteger.TEN, SimpleNumberCoercerFactory.getCoercerBigInteger(BIGINTEGER.getEPType()).coerceBoxedBigInt(BigInteger.TEN));

        try {
            JavaClassHelper.coerceBoxed(10, INTEGERPRIMITIVE.getEPType().getType());
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }
}
