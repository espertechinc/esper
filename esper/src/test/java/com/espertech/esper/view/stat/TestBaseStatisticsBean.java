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

public class TestBaseStatisticsBean extends TestCase {
    private final int PRECISION_DIGITS = 6;

    public void testAddRemoveXOnly() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        assertEquals(Double.NaN, stat.getXAverage());
        assertEquals(0, stat.getN());

        stat.addPoint(10);
        stat.addPoint(20);
        assertEquals(15d, stat.getXAverage());
        assertEquals(0d, stat.getYAverage());
        assertEquals(2, stat.getN());

        stat.removePoint(10);
        assertEquals(20d, stat.getXAverage());
        assertEquals(0d, stat.getYAverage());
        assertEquals(1, stat.getN());
    }

    public void testAverage() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        assertEquals(Double.NaN, stat.getXAverage());
        assertEquals(Double.NaN, stat.getYAverage());
        assertEquals(0, stat.getN());

        stat.removePoint(2, 363636);
        assertEquals(Double.NaN, stat.getXAverage());
        assertEquals(Double.NaN, stat.getYAverage());
        assertEquals(0, stat.getN());

        stat.addPoint(10, -2);
        assertEquals(10d, stat.getXAverage());
        assertEquals(-2d, stat.getYAverage());
        assertEquals(1, stat.getN());

        stat.addPoint(20, 4);
        assertEquals(15d, stat.getXAverage());
        assertEquals(1d, stat.getYAverage());
        assertEquals(2, stat.getN());

        stat.addPoint(1, 4);
        assertEquals(31d / 3d, stat.getXAverage());
        assertEquals(6d / 3d, stat.getYAverage());
        assertEquals(3, stat.getN());

        stat.addPoint(1, -10);
        assertEquals(8d, stat.getXAverage());
        assertEquals(-4d / 4d, stat.getYAverage());
        assertEquals(4, stat.getN());

        stat.addPoint(-32, -11);
        assertEquals(0d, stat.getXAverage());
        assertEquals(-15d / 5d, stat.getYAverage());
        assertEquals(5, stat.getN());

        stat.removePoint(-32, -10);
        assertEquals(32d / 4d, stat.getXAverage());
        assertEquals(-5d / 4d, stat.getYAverage());
        assertEquals(4, stat.getN());

        stat.removePoint(8, -5);
        assertEquals(24d / 3d, stat.getXAverage());
        assertEquals(0d, stat.getYAverage());
        assertEquals(3, stat.getN());

        stat.removePoint(2, 50);
        assertEquals(22d / 2d, stat.getXAverage());
        assertEquals(-50d / 2d, stat.getYAverage());
        assertEquals(2, stat.getN());

        stat.removePoint(1, 1);
        assertEquals(21d / 1d, stat.getXAverage());
        assertEquals(-51d, stat.getYAverage());
        assertEquals(1, stat.getN());

        stat.removePoint(3, 3);
        assertEquals(Double.NaN, stat.getXAverage());
        assertEquals(Double.NaN, stat.getYAverage());
        assertEquals(0, stat.getN());
    }

    public void testSum() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        assertEquals(0d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(0, stat.getN());

        stat.addPoint(10, -2);
        assertEquals(10d, stat.getXSum());
        assertEquals(-2d, stat.getYSum());
        assertEquals(1, stat.getN());

        stat.addPoint(3.5, -3);
        assertEquals(13.5d, stat.getXSum());
        assertEquals(-5d, stat.getYSum());
        assertEquals(2, stat.getN());

        stat.addPoint(1, 5);
        assertEquals(14.5d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(3, stat.getN());

        stat.removePoint(9, 1.5);
        assertEquals(5.5d, stat.getXSum());
        assertEquals(-1.5d, stat.getYSum());
        assertEquals(2, stat.getN());

        stat.removePoint(9.5, -1.5);
        assertEquals(-4d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(1, stat.getN());

        stat.removePoint(1, -1);
        assertEquals(0d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(0, stat.getN());

        stat.removePoint(1, 1);
        assertEquals(0d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(0, stat.getN());

        stat.addPoint(1.11, -3.333);
        assertEquals(1.11d, stat.getXSum());
        assertEquals(-3.333d, stat.getYSum());
        assertEquals(1, stat.getN());

        stat.addPoint(2.22, 3.333);
        assertEquals(1.11d + 2.22d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(2, stat.getN());

        stat.addPoint(-3.32, 0);
        assertEquals(1.11d + 2.22d - 3.32d, stat.getXSum());
        assertEquals(0d, stat.getYSum());
        assertEquals(3, stat.getN());
    }

    public void testStddev_STDEVPA() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        assertEquals(Double.NaN, stat.getXStandardDeviationPop());
        assertEquals(Double.NaN, stat.getYStandardDeviationPop());
        assertEquals(0, stat.getN());

        stat.addPoint(1, 10500);
        assertEquals(0.0d, stat.getXStandardDeviationPop());
        assertEquals(0.0d, stat.getYStandardDeviationPop());
        assertEquals(1, stat.getN());

        stat.addPoint(2, 10200);
        assertEquals(0.5d, stat.getXStandardDeviationPop());
        assertEquals(150d, stat.getYStandardDeviationPop());
        assertEquals(2, stat.getN());

        stat.addPoint(1.5, 10500);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationPop(), 0.40824829, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationPop(), 141.4213562, PRECISION_DIGITS));
        assertEquals(3, stat.getN());

        stat.addPoint(-0.1, 10500);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationPop(), 0.777817459, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationPop(), 129.9038106, PRECISION_DIGITS));
        assertEquals(4, stat.getN());

        stat.removePoint(2, 10200);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationPop(), 0.668331255, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationPop(), 0.0, PRECISION_DIGITS));
        assertEquals(3, stat.getN());

        stat.addPoint(0.89, 10499);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationPop(), 0.580102362, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationPop(), 0.433012702, PRECISION_DIGITS));
        assertEquals(4, stat.getN());

        stat.addPoint(1.23, 10500);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationPop(), 0.543860276, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationPop(), 0.4, PRECISION_DIGITS));
        assertEquals(5, stat.getN());
    }

    public void testStddevAndVariance_STDEV() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        assertEquals(Double.NaN, stat.getXStandardDeviationSample());
        assertEquals(Double.NaN, stat.getXVariance());
        assertEquals(Double.NaN, stat.getYStandardDeviationSample());
        assertEquals(Double.NaN, stat.getYVariance());

        stat.addPoint(100, -1);
        assertEquals(Double.NaN, stat.getXVariance());
        assertEquals(Double.NaN, stat.getXStandardDeviationSample());
        assertEquals(Double.NaN, stat.getYVariance());
        assertEquals(Double.NaN, stat.getYStandardDeviationSample());

        stat.addPoint(150, -1);
        assertEquals(1250d, stat.getXVariance());
        assertEquals(0d, stat.getYVariance());
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationSample(), 35.35533906, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationSample(), 0, PRECISION_DIGITS));

        stat.addPoint(0, -1.1);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXVariance(), 5833.33333333, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYVariance(), 0.003333333, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationSample(), 76.37626158, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationSample(), 0.057735027, PRECISION_DIGITS));

        stat.removePoint(100, -1);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXVariance(), 11250, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYVariance(), 0.005, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationSample(), 106.0660172, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationSample(), 0.070710678, PRECISION_DIGITS));

        stat.addPoint(-149, 0);
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXVariance(), 22350.333333, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYVariance(), 0.37, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getXStandardDeviationSample(), 149.5002787, PRECISION_DIGITS));
        assertTrue(DoubleValueAssertionUtil.equals(stat.getYStandardDeviationSample(), 0.608276253, PRECISION_DIGITS));
    }

    public void testClone() {
        BaseStatisticsBean stat = new BaseStatisticsBean();

        stat.addPoint(100, 10);
        stat.addPoint(200, 20);

        BaseStatisticsBean cloned = (BaseStatisticsBean) stat.clone();
        assertEquals(2, cloned.getN());
        assertEquals(300d, cloned.getXSum());
        assertEquals(150d, cloned.getXAverage());
        assertEquals(30d, cloned.getYSum());
        assertEquals(15d, cloned.getYAverage());
        assertTrue(DoubleValueAssertionUtil.equals(cloned.getXStandardDeviationPop(), 50.0, PRECISION_DIGITS));
    }
}