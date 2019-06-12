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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.internal.filterspec.DoubleRange;

import java.util.Comparator;

/**
 * Comparator for DoubleRange values.
 * <p>Sorts double ranges as this:     sort by min asc, max asc.
 * I.e. same minimum value sorts maximum value ascending.
 */
public final class DoubleRangeComparator implements Comparator<DoubleRange> {
    public final static DoubleRangeComparator INSTANCE = new DoubleRangeComparator();

    private DoubleRangeComparator() {
    }

    public final int compare(DoubleRange r1, DoubleRange r2) {
        double minOne = r1.getMin();
        double minTwo = r2.getMin();
        double maxOne = r1.getMax();
        double maxTwo = r2.getMax();

        int minCompared = Double.compare(minOne, minTwo);
        if (minCompared != 0) {
            return minCompared;
        }
        return Double.compare(maxOne, maxTwo);
    }
}
