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
package com.espertech.esper.type;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a range of numbers as a parameter.
 */
public class RangeParameter implements NumberSetParameter {
    private int low;
    private int high;
    private static final long serialVersionUID = 8495531153029613902L;

    public RangeParameter() {
    }

    /**
     * Ctor.
     *
     * @param low  - start of range
     * @param high - end of range
     */
    public RangeParameter(int low, int high) {
        this.low = low;
        this.high = high;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    /**
     * Returns start of range.
     *
     * @return start of range
     */
    public int getLow() {
        return low;
    }

    /**
     * Returns end of range.
     *
     * @return end of range
     */
    public int getHigh() {
        return high;
    }

    public boolean isWildcard(int min, int max) {
        if ((min >= low) && (max <= high)) {
            return true;
        }
        return false;
    }

    public Set<Integer> getValuesInRange(int min, int max) {
        Set<Integer> values = new HashSet<Integer>();

        int start = (min > low) ? min : low;
        int end = (max > high) ? high : max;

        while (start <= end) {
            values.add(start);
            start++;
        }

        return values;
    }

    public boolean containsPoint(int point) {
        return low <= point && point <= high;
    }

    public String formatted() {
        return Integer.toString(low) + "-" + Integer.toString(high);
    }
}
