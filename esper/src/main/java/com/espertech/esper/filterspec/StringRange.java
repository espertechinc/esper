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
public final class StringRange implements Range {
    private String min;
    private String max;
    private int hashCode;

    /**
     * Constructor - takes range endpoints.
     *
     * @param min is the low endpoint
     * @param max is the high endpoint
     */
    public StringRange(String min, String max) {
        this.min = min;
        this.max = max;

        if ((min != null) && (max != null)) {
            if (min.compareTo(max) > 0) {
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

    public Object getLowEndpoint() {
        return min;
    }

    public Object getHighEndpoint() {
        return max;
    }

    /**
     * Returns low endpoint.
     *
     * @return low endpoint
     */
    public final String getMin() {
        return min;
    }

    /**
     * Returns high endpoint.
     *
     * @return high endpoint
     */
    public final String getMax() {
        return max;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringRange that = (StringRange) o;

        if (hashCode != that.hashCode) return false;
        if (max != null ? !max.equals(that.max) : that.max != null) return false;
        if (min != null ? !min.equals(that.min) : that.min != null) return false;

        return true;
    }

    public int hashCode() {
        return hashCode;
    }

    public final String toString() {
        return "StringRange" +
                " min=" + min +
                " max=" + max;
    }
}
