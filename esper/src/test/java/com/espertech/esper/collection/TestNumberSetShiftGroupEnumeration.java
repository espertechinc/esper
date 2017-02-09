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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.TreeSet;

public class TestNumberSetShiftGroupEnumeration extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(TestNumberSetShiftGroupEnumeration.class);

    public void testGen() {

        assertEquals(29, countEnumeration(new int[]{1, 2, 3, 4, 5, 6}));
        assertEquals(31, countEnumeration(new int[]{1, 2, 3, 4, 5, 6, 7, 8}));

        int[] set = new int[]{1, 2, 3, 4, 5, 6, 7};

        final int[][] expectedValues = new int[][]{
                {1, 2, 3, 4, 5, 6, 7},
                {2, 3, 4, 5, 6, 7, 1},
                {3, 4, 5, 6, 7, 1, 2},
                {4, 5, 6, 7, 1, 2, 3},
                {5, 6, 7, 1, 2, 3, 4},
                {6, 7, 1, 2, 3, 4, 5},
                {7, 1, 2, 3, 4, 5, 6},
                {1, 5, 2, 6, 4, 3, 7},
                {1, 5, 3, 7, 2, 6, 4},
                {1, 5, 3, 7, 4, 2, 6},
                {1, 5, 4, 2, 6, 3, 7},
                {1, 5, 4, 3, 7, 2, 6},
                {2, 6, 1, 5, 3, 7, 4},
                {2, 6, 1, 5, 4, 3, 7},
                {2, 6, 3, 7, 1, 5, 4},
                {2, 6, 3, 7, 4, 1, 5},
                {2, 6, 4, 1, 5, 3, 7},
                {2, 6, 4, 3, 7, 1, 5},
                {3, 7, 1, 5, 2, 6, 4},
                {3, 7, 1, 5, 4, 2, 6},
                {3, 7, 2, 6, 1, 5, 4},
                {3, 7, 2, 6, 4, 1, 5},
                {3, 7, 4, 1, 5, 2, 6},
                {3, 7, 4, 2, 6, 1, 5},
                {4, 1, 5, 2, 6, 3, 7},
                {4, 1, 5, 3, 7, 2, 6},
                {4, 2, 6, 1, 5, 3, 7},
                {4, 2, 6, 3, 7, 1, 5},
                {4, 3, 7, 1, 5, 2, 6},
                {4, 3, 7, 2, 6, 1, 5},
        };

        /** Comment in here to print
         NumberSetShiftGroupEnumeration enumeration = new NumberSetShiftGroupEnumeration(set);
         while(enumeration.hasMoreElements()) {
         System.out.println(Arrays.toString(enumeration.nextElement()));
         }
         */

        tryPermutation(set, expectedValues);
    }

    private int countEnumeration(int[] numberSet) {
        NumberSetShiftGroupEnumeration enumeration = new NumberSetShiftGroupEnumeration(numberSet);
        int count = 0;
        while (enumeration.hasMoreElements()) {
            int[] result = enumeration.nextElement();
            assertSet(numberSet, result);
            count++;
        }
        return count;
    }

    private void tryPermutation(int[] numberSet, int[][] expectedValues) {
        NumberSetShiftGroupEnumeration enumeration = new NumberSetShiftGroupEnumeration(numberSet);

        int count = 0;
        while (enumeration.hasMoreElements()) {
            log.debug(".tryPermutation count=" + count);

            int[] result = enumeration.nextElement();
            int[] expected = expectedValues[count];

            log.debug(".tryPermutation result=" + Arrays.toString(result));
            log.debug(".tryPermutation expected=" + Arrays.toString(expected));

            assertSet(expected, result);

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

    private void assertSet(int[] expected, int[] result) {
        TreeSet<Integer> treeExp = getTreeSet(expected);
        TreeSet<Integer> treeRes = getTreeSet(result);
        EPAssertionUtil.assertEqualsExactOrder(getArr(treeRes), getArr(treeExp));
    }

    private int[] getArr(TreeSet<Integer> set) {
        int[] arr = new int[set.size()];
        int count = 0;
        for (int val : set) {
            arr[count++] = val;
        }
        return arr;
    }

    private TreeSet<Integer> getTreeSet(int[] set) {

        TreeSet<Integer> treeSet = new TreeSet<Integer>();
        for (int aSet : set) {
            treeSet.add(aSet);
        }
        return treeSet;
    }
}
