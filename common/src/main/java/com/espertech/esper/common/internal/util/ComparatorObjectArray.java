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
package com.espertech.esper.common.internal.util;

import java.util.Comparator;

import static com.espertech.esper.common.internal.util.CollectionUtil.compareValues;

/**
 * A comparator on multikeys. The multikeys must contain the same number of values.
 */
public final class ComparatorObjectArray implements Comparator<Object[]> {
    private final boolean[] isDescendingValues;

    /**
     * Ctor.
     *
     * @param isDescendingValues - each value is true if the corresponding (same index)
     *                           entry in the multi-keys is to be sorted in descending order. The multikeys
     *                           to be compared must have the same number of values as this array.
     */
    public ComparatorObjectArray(boolean[] isDescendingValues) {
        this.isDescendingValues = isDescendingValues;
    }

    public final int compare(Object[] firstValues, Object[] secondValues) {
        if (firstValues.length != isDescendingValues.length || secondValues.length != isDescendingValues.length) {
            throw new IllegalArgumentException("Incompatible size MultiKey sizes for comparison");
        }

        for (int i = 0; i < firstValues.length; i++) {
            Object valueOne = firstValues[i];
            Object valueTwo = secondValues[i];
            boolean isDescending = isDescendingValues[i];

            int comparisonResult = compareValues(valueOne, valueTwo, isDescending);
            if (comparisonResult != 0) {
                return comparisonResult;
            }
        }

        // Make the comparator compatible with equals
        if (!firstValues.equals(secondValues)) {
            return -1;
        } else {
            return 0;
        }
    }
}
