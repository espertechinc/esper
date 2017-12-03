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
package com.espertech.esper.filterspec;


/**
 * Holds a range of double values with a minimum (start) value and a maximum (end) value.
 */
public final class DoubleRange implements Range {
    private Double min;
    private Double max;
    private int hashCode;

    /**
     * Constructor - takes range endpoints.
     *
     * @param min is the low endpoint
     * @param max is the high endpoint
     */
    public DoubleRange(Double min, Double max) {
        this.min = min;
        this.max = max;

        if ((min != null) && (max != null)) {
            if (min > max) {
                this.max = min;
                this.min = max;
            }
        }

        hashCode = 7;
        if (min != null) {
            hashCode = 31 * hashCode;
            hashCode ^= min.hashCode();
        }
        if (max != null) {
            hashCode = 31 * hashCode;
            hashCode ^= max.hashCode();
        }
    }

    /**
     * Returns low endpoint.
     *
     * @return low endpoint
     */
    public final Double getMin() {
        return min;
    }

    public Object getHighEndpoint() {
        return max;
    }

    public Object getLowEndpoint() {
        return min;
    }

    /**
     * Returns high endpoint.
     *
     * @return high endpoint
     */
    public final Double getMax() {
        return max;
    }

    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DoubleRange)) {
            return false;
        }
        DoubleRange otherRange = (DoubleRange) other;

        return (otherRange.max.doubleValue() == this.max) && (otherRange.min.doubleValue() == this.min);
    }

    public final int hashCode() {
        return hashCode;
    }

    public final String toString() {
        return "DoubleRange" +
                " min=" + min +
                " max=" + max;
    }
}
