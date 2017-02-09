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
package com.espertech.esper.type;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestArithTypeEnum extends TestCase {
    public void testAddDouble() {
        MathArithTypeEnum.Computer computer = MathArithTypeEnum.ADD.getComputer(Double.class, Double.class, Double.class, false, false, null);
        assertEquals(12.1d, computer.compute(5.5, 6.6));
    }

    public void testInvalidGetComputer() {
        // Since we only do Double, Float, Integer and Long as results
        tryInvalid(String.class);
        tryInvalid(long.class);
        tryInvalid(short.class);
        tryInvalid(byte.class);
    }

    public void testAllComputers() {
        final Class[] testClasses = {
                Float.class, Double.class, Integer.class, Long.class};

        for (Class clazz : testClasses) {
            for (MathArithTypeEnum type : MathArithTypeEnum.values()) {
                MathArithTypeEnum.Computer computer = type.getComputer(clazz, clazz, clazz, false, false, null);
                Number result = computer.compute(3, 4);

                if (type == MathArithTypeEnum.ADD) {
                    assertEquals(clazz, result.getClass());
                    assertEquals(7d, result.doubleValue());
                }
                if (type == MathArithTypeEnum.SUBTRACT) {
                    assertEquals(clazz, result.getClass());
                    assertEquals(-1d, result.doubleValue());
                }
                if (type == MathArithTypeEnum.MULTIPLY) {
                    assertEquals(clazz, result.getClass());
                    assertEquals(12d, result.doubleValue());
                }
                if (type == MathArithTypeEnum.DIVIDE) {
                    assertEquals(Double.class, result.getClass());
                    if ((clazz == Integer.class) || (clazz == Long.class)) {
                        assertEquals("clazz=" + clazz, 0.75d, result.doubleValue());
                    } else {
                        assertEquals("clazz=" + clazz, 3 / 4d, result.doubleValue());
                    }
                }
            }
        }
    }

    public void testBigNumberComputers() {
        Object[][] parameters = new Object[][]{
                {true, new BigDecimal(6), MathArithTypeEnum.DIVIDE, new BigDecimal(3), new BigDecimal(2)},
                {false, BigInteger.valueOf(10), MathArithTypeEnum.ADD, BigInteger.valueOf(10), BigInteger.valueOf(20)},
                {false, BigInteger.valueOf(100), MathArithTypeEnum.SUBTRACT, BigInteger.valueOf(10), BigInteger.valueOf(90)},
                {false, BigInteger.valueOf(10), MathArithTypeEnum.MULTIPLY, BigInteger.valueOf(10), BigInteger.valueOf(100)},
                {false, BigInteger.valueOf(100), MathArithTypeEnum.DIVIDE, BigInteger.valueOf(5), BigInteger.valueOf(20)},

                {false, 9, MathArithTypeEnum.ADD, BigInteger.valueOf(10), BigInteger.valueOf(19)},
                {false, BigInteger.valueOf(6), MathArithTypeEnum.SUBTRACT, (byte) 7, BigInteger.valueOf(-1)},
                {false, BigInteger.valueOf(10), MathArithTypeEnum.DIVIDE, (long) 4, BigInteger.valueOf(2)},
                {false, BigInteger.valueOf(6), MathArithTypeEnum.MULTIPLY, (byte) 7, BigInteger.valueOf(42)},

                {true, BigInteger.valueOf(6), MathArithTypeEnum.ADD, (double) 7, new BigDecimal(13.0)},
                {true, BigInteger.valueOf(6), MathArithTypeEnum.SUBTRACT, (double) 5, new BigDecimal(1.0)},
                {true, BigInteger.valueOf(6), MathArithTypeEnum.MULTIPLY, (double) 5, new BigDecimal(30.0)},
                {true, BigInteger.valueOf(6), MathArithTypeEnum.DIVIDE, (double) 2, new BigDecimal(3)},

                {true, 9, MathArithTypeEnum.ADD, new BigDecimal(10), new BigDecimal(19)},
                {true, new BigDecimal(6), MathArithTypeEnum.SUBTRACT, new BigDecimal(5), new BigDecimal(1)},
                {true, new BigDecimal(6), MathArithTypeEnum.MULTIPLY, new BigDecimal(5), new BigDecimal(30)},
                {true, new BigDecimal(6), MathArithTypeEnum.ADD, new BigDecimal(7), new BigDecimal(13)},

                {true, new BigDecimal(10), MathArithTypeEnum.ADD, (long) 8, new BigDecimal(18)},
                {true, new BigDecimal(10), MathArithTypeEnum.DIVIDE, (long) 8, new BigDecimal(1.25)},
                {true, new BigDecimal(6), MathArithTypeEnum.SUBTRACT, (byte) 7, new BigDecimal(-1)},
                {true, new BigDecimal(6), MathArithTypeEnum.MULTIPLY, (byte) 7, new BigDecimal(42)},

                {true, new BigDecimal(6), MathArithTypeEnum.MULTIPLY, (double) 3, new BigDecimal(18.0)},
                {true, new BigDecimal(6), MathArithTypeEnum.ADD, (double) 2, new BigDecimal(8.0)},
                {true, new BigDecimal(6), MathArithTypeEnum.DIVIDE, (double) 4, new BigDecimal(1.5)},
                {true, new BigDecimal(6), MathArithTypeEnum.SUBTRACT, (double) 8, new BigDecimal(-2.0)},
        };

        for (int i = 0; i < parameters.length; i++) {
            boolean isBigDec = (Boolean) parameters[i][0];
            Object lhs = parameters[i][1];
            MathArithTypeEnum e = (MathArithTypeEnum) parameters[i][2];
            Object rhs = parameters[i][3];
            Object expected = parameters[i][4];

            MathArithTypeEnum.Computer computer;
            if (isBigDec) {
                computer = e.getComputer(BigDecimal.class, lhs.getClass(), rhs.getClass(), false, false, null);
            } else {
                computer = e.getComputer(BigInteger.class, lhs.getClass(), rhs.getClass(), false, false, null);
            }

            Object result = null;
            try {
                result = computer.compute((Number) lhs, (Number) rhs);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            assertEquals("line " + i + " lhs=" + lhs + " op=" + e.toString() + " rhs=" + rhs, expected, result);
        }
    }

    private void tryInvalid(Class clazz) {
        try {
            MathArithTypeEnum.ADD.getComputer(clazz, clazz, clazz, false, false, null);
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }
}
