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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class TestPermutationEnumeration extends TestCase {
    public void testInvalid() {
        try {
            new PermutationEnumeration(0);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testNext() {
        final int[][] expectedValues4 = new int[][]{
                {0, 1, 2, 3},     // 0
                {0, 1, 3, 2},
                {0, 2, 1, 3},
                {0, 2, 3, 1},
                {0, 3, 1, 2},
                {0, 3, 2, 1},     // 5

                {1, 0, 2, 3},     // 6
                {1, 0, 3, 2},     // 7
                {1, 2, 0, 3},     // 8
                {1, 2, 3, 0},
                {1, 3, 0, 2},
                {1, 3, 2, 0},     // 11

                {2, 0, 1, 3},     // 12
                {2, 0, 3, 1},
                {2, 1, 0, 3},
                {2, 1, 3, 0},
                {2, 3, 0, 1},
                {2, 3, 1, 0},     // 17

                {3, 0, 1, 2},     // 18
                {3, 0, 2, 1},
                {3, 1, 0, 2},
                {3, 1, 2, 0},     // 21
                {3, 2, 0, 1},
                {3, 2, 1, 0}};
        tryPermutation(4, expectedValues4);

        final int[][] expectedValues3 = new int[][]{
                {0, 1, 2},
                {0, 2, 1},
                {1, 0, 2},
                {1, 2, 0},
                {2, 0, 1},
                {2, 1, 0}};
        tryPermutation(3, expectedValues3);

        final int[][] expectedValues2 = new int[][]{
                {0, 1},
                {1, 0}};
        tryPermutation(2, expectedValues2);

        final int[][] expectedValues1 = new int[][]{
                {0}};
        tryPermutation(1, expectedValues1);
    }

    private void tryPermutation(int numElements, int[][] expectedValues) {
        /*
        Total: 4 * 3 * 2 = 24 = 6!  (6 faculty)

        Example:8
        n / 6 = first number        == index 1, total {1}, remains {0, 2, 3}
        remainder 8 - 1 * 6         == 2
        n / 2 = second number       == index 1, total {1, 2}, remain {0, 3}
        remainder 2 - 1 * 2         == 0
                                    == total {1, 2, 0, 3}

        Example:21   out {0, 1, 2, 3}
        21 / 6                      == index 3 -> in {3}, out {0, 1, 2}
        remainder 21 - 3 * 6        == 3
        3 / 2 = second number       == index 1 -> in {3, 1}, remain {0, 2}
        remainder 3 - 1 * 2         == 1
                                    == index 1 -> in {3, 1, 2} out {0}
        */
        PermutationEnumeration enumeration = new PermutationEnumeration(numElements);

        int count = 0;
        while (enumeration.hasMoreElements()) {
            int[] result = enumeration.nextElement();
            int[] expected = expectedValues[count];

            log.debug(".tryPermutation result=" + Arrays.toString(result));
            log.debug(".tryPermutation expected=" + Arrays.toString(result));

            count++;
            assertTrue("Mismatch in count=" + count, Arrays.equals(result, expected));
        }
        assertEquals(count, expectedValues.length);

        try {
            enumeration.nextElement();
            fail();
        } catch (NoSuchElementException ex) {
            // Expected
        }
    }

    public static void testGetPermutation() {
        int[] factors = PermutationEnumeration.getFactors(4);
        int[] result = PermutationEnumeration.getPermutation(4, 21, factors);

        log.debug(".testGetPermutation result=" + Arrays.toString(result));
        assertTrue(Arrays.equals(result, new int[]{3, 1, 2, 0}));
    }

    public static void testGetFactors() {
        int[] factors = PermutationEnumeration.getFactors(5);
        assertTrue(Arrays.equals(factors, new int[]{24, 6, 2, 1, 0}));

        factors = PermutationEnumeration.getFactors(4);
        assertTrue(Arrays.equals(factors, new int[]{6, 2, 1, 0}));

        factors = PermutationEnumeration.getFactors(3);
        assertTrue(Arrays.equals(factors, new int[]{2, 1, 0}));

        factors = PermutationEnumeration.getFactors(2);
        assertTrue(Arrays.equals(factors, new int[]{1, 0}));

        factors = PermutationEnumeration.getFactors(1);
        assertTrue(Arrays.equals(factors, new int[]{0}));

        //log.debug(".testGetFactors " + Arrays.toString(factors));
    }

    public static void testFaculty() {
        assertEquals(0, PermutationEnumeration.faculty(0));
        assertEquals(1, PermutationEnumeration.faculty(1));
        assertEquals(2, PermutationEnumeration.faculty(2));
        assertEquals(6, PermutationEnumeration.faculty(3));
        assertEquals(24, PermutationEnumeration.faculty(4));
        assertEquals(120, PermutationEnumeration.faculty(5));
        assertEquals(720, PermutationEnumeration.faculty(6));
    }

    private final static Logger log = LoggerFactory.getLogger(TestPermutationEnumeration.class);
}
