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

import com.espertech.esper.filterspec.StringRange;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestStringRangeComparator extends TestCase {
    public void testComparator() {
        SortedSet<StringRange> sorted = new TreeSet<StringRange>(new StringRangeComparator());

        final String[][] TEST_SET =
                {
                        {"B", "G"},
                        {"B", "F"},
                        {null, "E"},
                        {"A", "F"},
                        {"A", "G"},
                };

        final int[] EXPECTED_INDEX = {2, 3, 4, 1, 0};

        // Sort
        StringRange ranges[] = new StringRange[TEST_SET.length];
        for (int i = 0; i < TEST_SET.length; i++) {
            ranges[i] = new StringRange(TEST_SET[i][0], TEST_SET[i][1]);
            sorted.add(ranges[i]);
        }
        System.out.println("sorted=" + sorted);

        // Check results
        int count = 0;
        for (Iterator<StringRange> i = sorted.iterator(); i.hasNext(); ) {
            StringRange range = i.next();
            int indexExpected = EXPECTED_INDEX[count];
            StringRange expected = ranges[indexExpected];

            log.debug(".testComparator count=" + count +
                    " range=" + range +
                    " expected=" + expected);

            assertEquals("failed at count " + count, range, expected);
            count++;
        }
        assertEquals(count, TEST_SET.length);
    }

    private static final Logger log = LoggerFactory.getLogger(TestStringRangeComparator.class);
}
