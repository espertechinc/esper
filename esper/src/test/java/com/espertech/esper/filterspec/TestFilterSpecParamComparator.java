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
package com.espertech.esper.filterspec;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestFilterSpecParamComparator extends TestCase {
    private FilterSpecParamComparator comparator;

    public void setUp() {
        comparator = new FilterSpecParamComparator();
    }

    public void testCompareOneByOne() {
        FilterOperator param1 = FilterOperator.EQUAL;
        FilterOperator param4 = FilterOperator.RANGE_CLOSED;
        FilterOperator param7 = FilterOperator.GREATER;
        FilterOperator param8 = FilterOperator.NOT_EQUAL;
        FilterOperator param9 = FilterOperator.IN_LIST_OF_VALUES;
        FilterOperator param10 = FilterOperator.NOT_RANGE_CLOSED;
        FilterOperator param11 = FilterOperator.NOT_IN_LIST_OF_VALUES;

        // Compare same comparison types
        assertTrue(comparator.compare(param8, param1) == 1);
        assertTrue(comparator.compare(param1, param8) == -1);

        assertTrue(comparator.compare(param4, param4) == 0);

        // Compare across comparison types
        assertTrue(comparator.compare(param7, param1) == 1);
        assertTrue(comparator.compare(param1, param7) == -1);

        assertTrue(comparator.compare(param4, param1) == 1);
        assertTrue(comparator.compare(param1, param4) == -1);

        // 'in' is before all but after equals
        assertTrue(comparator.compare(param9, param4) == -1);
        assertTrue(comparator.compare(param9, param9) == 0);
        assertTrue(comparator.compare(param9, param1) == 1);

        // inverted range is lower rank
        assertTrue(comparator.compare(param10, param1) == 1);
        assertTrue(comparator.compare(param10, param8) == -1);

        // not-in is lower rank
        assertTrue(comparator.compare(param11, param1) == 1);
        assertTrue(comparator.compare(param11, param8) == -1);
    }

    public void testCompareAll() {
        SortedSet<FilterOperator> sorted = new TreeSet<FilterOperator>(comparator);

        for (int i = 0; i < FilterOperator.values().length; i++) {
            FilterOperator op = FilterOperator.values()[i];
            sorted.add(op);
        }

        assertEquals(FilterOperator.EQUAL, sorted.first());
        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, sorted.last());
        assertEquals("[EQUAL, IS, IN_LIST_OF_VALUES, ADVANCED_INDEX, RANGE_OPEN, RANGE_HALF_OPEN, RANGE_HALF_CLOSED, RANGE_CLOSED, LESS, LESS_OR_EQUAL, GREATER_OR_EQUAL, GREATER, NOT_RANGE_CLOSED, NOT_RANGE_HALF_CLOSED, NOT_RANGE_HALF_OPEN, NOT_RANGE_OPEN, NOT_IN_LIST_OF_VALUES, NOT_EQUAL, IS_NOT, BOOLEAN_EXPRESSION]", sorted.toString());

        log.debug(".testCompareAll " + Arrays.toString(sorted.toArray()));
    }

    private static final Logger log = LoggerFactory.getLogger(TestFilterSpecParamComparator.class);
}
