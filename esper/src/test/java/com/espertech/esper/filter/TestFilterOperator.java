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

import com.espertech.esper.filterspec.FilterOperator;
import junit.framework.TestCase;

public class TestFilterOperator extends TestCase {
    public void testRanges() {
        assertTrue(FilterOperator.parseRangeOperator(false, false, false) == FilterOperator.RANGE_OPEN);
        assertTrue(FilterOperator.parseRangeOperator(true, true, false) == FilterOperator.RANGE_CLOSED);
        assertTrue(FilterOperator.parseRangeOperator(true, false, false) == FilterOperator.RANGE_HALF_OPEN);
        assertTrue(FilterOperator.parseRangeOperator(false, true, false) == FilterOperator.RANGE_HALF_CLOSED);
        assertTrue(FilterOperator.parseRangeOperator(false, false, true) == FilterOperator.NOT_RANGE_OPEN);
        assertTrue(FilterOperator.parseRangeOperator(true, true, true) == FilterOperator.NOT_RANGE_CLOSED);
        assertTrue(FilterOperator.parseRangeOperator(true, false, true) == FilterOperator.NOT_RANGE_HALF_OPEN);
        assertTrue(FilterOperator.parseRangeOperator(false, true, true) == FilterOperator.NOT_RANGE_HALF_CLOSED);
    }

    public void testIsComparison() {
        assertTrue(FilterOperator.GREATER.isComparisonOperator());
        assertTrue(FilterOperator.GREATER_OR_EQUAL.isComparisonOperator());
        assertTrue(FilterOperator.LESS.isComparisonOperator());
        assertTrue(FilterOperator.LESS_OR_EQUAL.isComparisonOperator());
        assertFalse(FilterOperator.RANGE_CLOSED.isComparisonOperator());
        assertFalse(FilterOperator.EQUAL.isComparisonOperator());
        assertFalse(FilterOperator.NOT_EQUAL.isComparisonOperator());
    }

    public void testIsRange() {
        assertTrue(FilterOperator.RANGE_OPEN.isRangeOperator());
        assertTrue(FilterOperator.RANGE_CLOSED.isRangeOperator());
        assertTrue(FilterOperator.RANGE_HALF_OPEN.isRangeOperator());
        assertTrue(FilterOperator.RANGE_HALF_CLOSED.isRangeOperator());
        assertFalse(FilterOperator.NOT_RANGE_HALF_CLOSED.isRangeOperator());
        assertFalse(FilterOperator.NOT_RANGE_OPEN.isRangeOperator());
        assertFalse(FilterOperator.NOT_RANGE_CLOSED.isRangeOperator());
        assertFalse(FilterOperator.NOT_RANGE_HALF_OPEN.isRangeOperator());
        assertFalse(FilterOperator.LESS.isRangeOperator());
        assertFalse(FilterOperator.EQUAL.isRangeOperator());
        assertFalse(FilterOperator.NOT_EQUAL.isRangeOperator());
    }

    public void testIsInvertedRange() {
        assertFalse(FilterOperator.RANGE_OPEN.isInvertedRangeOperator());
        assertFalse(FilterOperator.RANGE_CLOSED.isInvertedRangeOperator());
        assertFalse(FilterOperator.RANGE_HALF_OPEN.isInvertedRangeOperator());
        assertFalse(FilterOperator.RANGE_HALF_CLOSED.isInvertedRangeOperator());
        assertTrue(FilterOperator.NOT_RANGE_HALF_CLOSED.isInvertedRangeOperator());
        assertTrue(FilterOperator.NOT_RANGE_OPEN.isInvertedRangeOperator());
        assertTrue(FilterOperator.NOT_RANGE_CLOSED.isInvertedRangeOperator());
        assertTrue(FilterOperator.NOT_RANGE_HALF_OPEN.isInvertedRangeOperator());
        assertFalse(FilterOperator.LESS.isInvertedRangeOperator());
        assertFalse(FilterOperator.EQUAL.isInvertedRangeOperator());
        assertFalse(FilterOperator.NOT_EQUAL.isInvertedRangeOperator());
    }
}