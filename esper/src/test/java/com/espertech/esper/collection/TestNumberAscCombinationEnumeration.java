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
package com.espertech.esper.collection;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class TestNumberAscCombinationEnumeration extends TestCase {

    public void testNumberAscCombinationEnumeration() {
        compare(new int[][]{{0}}, 1);
        compare(new int[][]{{0, 1}, {0}, {1}}, 2);
        compare(new int[][]{{0, 1, 2}, {0, 1}, {0, 2}, {1, 2}, {0}, {1}, {2}}, 3);
        compare(new int[][]{{0, 1, 2, 3},
                {0, 1, 2}, {0, 1, 3}, {0, 2, 3}, {1, 2, 3},
                {0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3},
                {0}, {1}, {2}, {3}}, 4);
        compare(new int[][]{{0, 1, 2, 3, 4},
                {0, 1, 2, 3}, {0, 1, 2, 4}, {0, 1, 3, 4}, {0, 2, 3, 4}, {1, 2, 3, 4},
                {0, 1, 2}, {0, 1, 3}, {0, 1, 4}, {0, 2, 3}, {0, 2, 4}, {0, 3, 4},
                {1, 2, 3}, {1, 2, 4}, {1, 3, 4},
                {2, 3, 4},
                {0, 1}, {0, 2}, {0, 3}, {0, 4}, {1, 2}, {1, 3}, {1, 4}, {2, 3}, {2, 4}, {3, 4},
                {0}, {1}, {2}, {3}, {4}}, 5);

        try {
            new NumberAscCombinationEnumeration(0);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private static void compare(int[][] expected, int n) {
        NumberAscCombinationEnumeration e = new NumberAscCombinationEnumeration(n);
        int count = 0;
        while (count < expected.length) {
            assertTrue(e.hasMoreElements());
            int[] next = e.nextElement();
            int[] expectedArr = expected[count];
            if (!Arrays.equals(expectedArr, next)) {
                fail("Expected " + Arrays.toString(expectedArr) + " Received " + Arrays.toString(next) + " at index " + count);
            }
            count++;
        }

        assertFalse(e.hasMoreElements());
        try {
            e.nextElement();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }
    }
}
