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

public class TestNumberSetPermutationEnumeration extends TestCase {
    public void testInvalid() {
        try {
            new PermutationEnumeration(0);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testNext() {
        final int[] numberSet = new int[]{10, 11, 12};
        final int[][] expectedValues = new int[][]{
                {10, 11, 12},
                {10, 12, 11},
                {11, 10, 12},
                {11, 12, 10},
                {12, 10, 11},
                {12, 11, 10}};
        tryPermutation(numberSet, expectedValues);
    }

    private void tryPermutation(int[] numberSet, int[][] expectedValues) {
        NumberSetPermutationEnumeration enumeration = new NumberSetPermutationEnumeration(numberSet);

        int count = 0;
        while (enumeration.hasMoreElements()) {
            int[] result = enumeration.nextElement();
            int[] expected = expectedValues[count];

            log.debug(".tryPermutation result=" + Arrays.toString(result));
            log.debug(".tryPermutation expected=" + Arrays.toString(expected));

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

    private final static Logger log = LoggerFactory.getLogger(TestNumberSetPermutationEnumeration.class);
}
