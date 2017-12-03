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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestDoubleRangeComparator extends TestCase {
    public void testComparator() {
        SortedSet<DoubleRange> sorted = new TreeSet<DoubleRange>(new DoubleRangeComparator());

        final double[][] TEST_SET =
                {{10, 20},         // 4
                        {10, 15},         // 2
                        {10, 25},         // 5
                        {5, 20},          // 0
                        {5, 25},          // 1
                        {15, 20},         // 6
                        {10, 16}};       // 3

        final int[] EXPECTED_INDEX = {3, 4, 1, 6, 0, 2, 5};

        // Sort
        DoubleRange ranges[] = new DoubleRange[TEST_SET.length];
        for (int i = 0; i < TEST_SET.length; i++) {
            ranges[i] = new DoubleRange(TEST_SET[i][0], TEST_SET[i][1]);
            sorted.add(ranges[i]);
        }

        // Check results
        int count = 0;
        for (Iterator<DoubleRange> i = sorted.iterator(); i.hasNext(); ) {
            DoubleRange range = i.next();
            int indexExpected = EXPECTED_INDEX[count];
            DoubleRange expected = ranges[indexExpected];

            log.debug(".testComparator count=" + count +
                    " range=" + range +
                    " expected=" + expected);

            assertEquals(range, expected);
            count++;
        }
        assertEquals(count, TEST_SET.length);
    }

    private static final Logger log = LoggerFactory.getLogger(TestDoubleRangeComparator.class);
}
