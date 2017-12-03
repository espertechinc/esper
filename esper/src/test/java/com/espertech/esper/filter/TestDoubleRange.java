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
package com.espertech.esper.filter;

import com.espertech.esper.filterspec.DoubleRange;
import junit.framework.TestCase;

public class TestDoubleRange extends TestCase {
    public void testNew() {
        DoubleRange range = new DoubleRange(10d, 20d);
        assertEquals(20d, range.getMax());
        assertEquals(10d, range.getMin());

        range = new DoubleRange(20d, 10d);
        assertEquals(20d, range.getMax());
        assertEquals(10d, range.getMin());
    }

    public void testEquals() {
        DoubleRange rangeOne = new DoubleRange(10d, 20d);
        DoubleRange rangeTwo = new DoubleRange(20d, 10d);
        DoubleRange rangeThree = new DoubleRange(20d, 11d);
        DoubleRange rangeFour = new DoubleRange(21d, 10d);

        assertEquals(rangeOne, rangeTwo);
        assertEquals(rangeTwo, rangeOne);
        assertFalse(rangeOne.equals(rangeThree));
        assertFalse(rangeOne.equals(rangeFour));
        assertFalse(rangeThree.equals(rangeFour));
    }

    public void testHash() {
        DoubleRange range = new DoubleRange(10d, 20d);
        int hashCode = 7;
        hashCode *= 31;
        hashCode ^= Double.valueOf(10).hashCode();
        hashCode *= 31;
        hashCode ^= Double.valueOf(20).hashCode();

        assertEquals(hashCode, range.hashCode());
    }
}
