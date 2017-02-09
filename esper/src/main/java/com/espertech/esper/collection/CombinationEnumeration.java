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

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Provided a 2-dimensional array of values, provide all possible combinations:
 * <pre>
 *     For example, an array { {1}, {"A", "B"}, {"X", "Y"} } provides these combinations:
 *        1 A X
 *        1 A Y
 *        1 B X
 *        1 B Y
 * </pre>.
 * Usage Note: Do not hold on to the returned object array as {@link #nextElement()} returns the same array
 * with changed values for each enumeration.
 * <p>
 * Each array element must be non-null and length 1 or more.
 * </p>
 * <p>
 * Does not detect duplicates in values.
 * </p>
 * <p>
 * Allows any number for the first dimension.
 * </p>
 * <p>
 * The algorithm adds 1 to the right and overflows until done.
 * </p>
 */
public class CombinationEnumeration implements Enumeration<Object[]> {
    private final Object[][] combinations;
    private final Object[] prototype;
    private final int[] current;
    private boolean hasMore = true;

    public CombinationEnumeration(Object[][] combinations) {
        for (Object[] element : combinations) {
            if (element == null || element.length < 1) {
                throw new IllegalArgumentException("Expecting non-null element of minimum length 1");
            }
        }
        this.combinations = combinations;
        this.current = new int[combinations.length];
        this.prototype = new Object[combinations.length];
    }

    public static CombinationEnumeration fromZeroBasedRanges(int[] zeroBasedRanges) {
        Object[][] combinations = new Object[zeroBasedRanges.length][];
        for (int i = 0; i < zeroBasedRanges.length; i++) {
            combinations[i] = new Integer[zeroBasedRanges[i]];
            for (int j = 0; j < zeroBasedRanges[i]; j++) {
                combinations[i][j] = j;
            }
        }
        return new CombinationEnumeration(combinations);
    }

    public boolean hasMoreElements() {
        return hasMore;
    }

    public Object[] nextElement() {
        if (!hasMore) {
            throw new NoSuchElementException();
        }
        populate();
        determineNext();
        return prototype;
    }

    private void determineNext() {
        for (int i = combinations.length - 1; i >= 0; i--) {
            int max = combinations[i].length;
            if (current[i] < max - 1) {
                current[i]++;
                return;
            }
            // overflowing
            current[i] = 0;
        }
        hasMore = false;
    }

    private void populate() {
        for (int i = 0; i < prototype.length; i++) {
            prototype[i] = combinations[i][current[i]];
        }
    }
}
