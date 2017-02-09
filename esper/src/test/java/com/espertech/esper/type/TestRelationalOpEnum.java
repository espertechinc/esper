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

import com.espertech.esper.supportunit.bean.SupportBean;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestRelationalOpEnum extends TestCase {
    boolean[][] expected = new boolean[][]{{false, false, true},  //GT
            {false, true, true}, // GE
            {true, false, false}, // LT
            {true, true, false}, // LE
    };

    public void testStringComputers() {
        String[][] parameters = new String[][]{{"a", "b"}, {"a", "a"}, {"b", "a"}};

        for (RelationalOpEnum op : RelationalOpEnum.values()) {
            for (int i = 0; i < parameters.length; i++) {
                boolean result = op.getComputer(String.class, String.class, String.class).compare(parameters[i][0], parameters[i][1]);
                assertEquals("op=" + op.toString() + ",i=" + i, expected[op.ordinal()][i], result);
            }
        }
    }

    public void testLongComputers() {
        long[][] parameters = new long[][]{{1, 2}, {1, 1}, {2, 1}};

        for (RelationalOpEnum op : RelationalOpEnum.values()) {
            for (int i = 0; i < parameters.length; i++) {
                boolean result = op.getComputer(Long.class, Long.class, long.class).compare(parameters[i][0], parameters[i][1]);
                assertEquals("op=" + op.toString() + ",i=" + i, expected[op.ordinal()][i], result);
            }
        }
    }

    public void testDoubleComputers() {
        double[][] parameters = new double[][]{{1, 2}, {1, 1}, {2, 1}};

        for (RelationalOpEnum op : RelationalOpEnum.values()) {
            for (int i = 0; i < parameters.length; i++) {
                boolean result = op.getComputer(Double.class, double.class, Double.class).compare(parameters[i][0], parameters[i][1]);
                assertEquals("op=" + op.toString() + ",i=" + i, expected[op.ordinal()][i], result);
            }
        }
    }

    public void testBigNumberComputers() {
        Object[][] parameters = new Object[][]{
                {false, BigInteger.valueOf(10), RelationalOpEnum.LE, BigInteger.valueOf(10), true},
                {false, BigInteger.valueOf(10), RelationalOpEnum.GE, BigInteger.valueOf(10), true},
                {false, BigInteger.valueOf(10), RelationalOpEnum.LT, BigInteger.valueOf(10), false},
                {false, BigInteger.valueOf(10), RelationalOpEnum.GT, BigInteger.valueOf(10), false},
                {false, 9, RelationalOpEnum.GE, BigInteger.valueOf(10), false},
                {false, BigInteger.valueOf(10), RelationalOpEnum.LE, (long) 10, true},
                {false, BigInteger.valueOf(6), RelationalOpEnum.LT, (byte) 7, true},
                {false, BigInteger.valueOf(6), RelationalOpEnum.GT, (byte) 7, false},
                {false, BigInteger.valueOf(6), RelationalOpEnum.GT, (double) 6, false},
                {false, BigInteger.valueOf(6), RelationalOpEnum.GE, (double) 6, true},
                {false, BigInteger.valueOf(6), RelationalOpEnum.LE, (double) 6, true},
                {false, BigInteger.valueOf(6), RelationalOpEnum.LT, (double) 6, false},
                {true, 9, RelationalOpEnum.GE, new BigDecimal(10), false},
                {true, new BigDecimal(6), RelationalOpEnum.LT, new BigDecimal(6), false},
                {true, new BigDecimal(6), RelationalOpEnum.GT, new BigDecimal(6), false},
                {true, new BigDecimal(6), RelationalOpEnum.GE, new BigDecimal(6), true},
                {true, new BigDecimal(6), RelationalOpEnum.LE, new BigDecimal(6), true},
                {true, new BigDecimal(10), RelationalOpEnum.LE, (long) 10, true},
                {true, new BigDecimal(6), RelationalOpEnum.LT, (byte) 7, true},
                {true, new BigDecimal(6), RelationalOpEnum.GT, (byte) 7, false},
                {true, new BigDecimal(6), RelationalOpEnum.GT, (double) 6, false},
                {true, new BigDecimal(6), RelationalOpEnum.GE, (double) 6, true},
                {true, new BigDecimal(6), RelationalOpEnum.LE, (double) 6, true},
                {true, new BigDecimal(6), RelationalOpEnum.LT, (double) 6, false},
        };

        for (int i = 0; i < parameters.length; i++) {
            boolean isBigDec = (Boolean) parameters[i][0];
            Object lhs = parameters[i][1];
            RelationalOpEnum e = (RelationalOpEnum) parameters[i][2];
            Object rhs = parameters[i][3];
            Object expected = parameters[i][4];

            RelationalOpEnum.Computer computer;
            if (isBigDec) {
                computer = e.getComputer(BigDecimal.class, lhs.getClass(), rhs.getClass());
            } else {
                computer = e.getComputer(BigInteger.class, lhs.getClass(), rhs.getClass());
            }

            Object result = null;
            try {
                result = computer.compare(lhs, rhs);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            assertEquals("line " + i + " lhs=" + lhs + " op=" + e.toString() + " rhs=" + rhs, expected, result);
        }
    }

    public void testInvalidGetComputer() {
        // Since we only do Double, Long and String compares
        tryInvalid(boolean.class);
        tryInvalid(long.class);
        tryInvalid(short.class);
        tryInvalid(SupportBean.class);
    }

    private void tryInvalid(Class clazz) {
        try {
            RelationalOpEnum.GE.getComputer(clazz, clazz, clazz);
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }
}
