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
package com.espertech.esper.view.stat;

import com.espertech.esper.supportunit.util.DoubleValueAssertionUtil;
import junit.framework.TestCase;

public class TestRegressionBean extends TestCase {
    private final int PRECISION_DIGITS = 6;

    public void testLINEST() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        assertEquals(Double.NaN, stat.getSlope());
        assertEquals(Double.NaN, stat.getYIntercept());
        assertEquals(0, stat.getN());

        stat.addPoint(1, 15);
        assertEquals(Double.NaN, stat.getSlope());
        assertEquals(Double.NaN, stat.getYIntercept());
        assertEquals(1, stat.getN());

        stat.addPoint(2, 20);
        assertEquals(5d, stat.getSlope());
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYIntercept(), 10, PRECISION_DIGITS));
        assertEquals(2, stat.getN());

        stat.addPoint(1, 17);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getSlope(), 4, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYIntercept(), 12, PRECISION_DIGITS));
        assertEquals(3, stat.getN());

        stat.addPoint(1.4, 14);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getSlope(), 3.731343284, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYIntercept(), 11.46268657, PRECISION_DIGITS));
        assertEquals(4, stat.getN());

        stat.removePoint(1, 17);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getSlope(), 5.394736842, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYIntercept(), 8.421052632, PRECISION_DIGITS));
        assertEquals(3, stat.getN());

        stat.addPoint(0, 0);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getSlope(), 9.764150943, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYIntercept(), 1.509433962, PRECISION_DIGITS));
        assertEquals(4, stat.getN());
    }
}