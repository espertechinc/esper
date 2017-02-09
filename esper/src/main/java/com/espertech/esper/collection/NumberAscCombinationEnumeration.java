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
 * Provides an enumeration of each combination of numbers between zero and N-1
 * with N must be at least 1,
 * with each combination cannot have duplicates,
 * with each combination at least one element,
 * with the longest combination gets returned first and the least long combination of the highest N-1 value last.
 * <p>
 * For example, for N=3:
 * </p>
 * <pre>
 *         {0, 1, 2}
 *         {0, 1}
 *         {0, 2}
 *         {1, 2}
 *         {0}
 *         {1}
 *         {2}
 *     </pre>
 */
public class NumberAscCombinationEnumeration implements Enumeration<int[]> {
    private final int n;
    private int level;
    private int[] current;

    public NumberAscCombinationEnumeration(int n) {
        if (n < 1) {
            throw new IllegalArgumentException();
        }
        this.n = n;
        this.level = n;
        this.current = levelCurrent(n);
    }

    public boolean hasMoreElements() {
        return current != null;
    }

    public int[] nextElement() {
        if (!hasMoreElements()) {
            throw new NoSuchElementException();
        }
        int[] item = copyCurrent(current);
        computeNext();
        return item;
    }

    private void computeNext() {

        // determine whether there is a next for the outermost
        int last = current.length - 1;
        if (current[last] + 1 < n) {
            current[last]++;
            return;
        }

        // overflow
        int currOverflowedLevel = last - 1;
        while (currOverflowedLevel >= 0) {
            int maxAtPosition = n - level + currOverflowedLevel;
            if (current[currOverflowedLevel] < maxAtPosition) {
                current[currOverflowedLevel]++;
                for (int i = currOverflowedLevel + 1; i < current.length; i++) {
                    current[i] = current[i - 1] + 1;
                }
                return;
            }
            currOverflowedLevel--;
        }

        // bump level down
        level--;
        if (level == 0) {
            current = null;
        } else {
            this.current = levelCurrent(level);
        }
    }

    private static int[] levelCurrent(int level) {
        int[] current = new int[level];
        for (int i = 0; i < level; i++) {
            current[i] = i;
        }
        return current;
    }

    private int[] copyCurrent(int[] current) {
        int[] updated = new int[current.length];
        System.arraycopy(current, 0, updated, 0, updated.length);
        return updated;
    }
}
