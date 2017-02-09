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
package com.espertech.esper.client.hook;

/**
 * Provides a range as a start and end value, for use as a paramater to the lookup values passed to the {@link VirtualDataWindowLookup} lookup method.
 * <p>
 * Consult {@link VirtualDataWindowLookupOp} for information on the type of range represented (open, closed, inverted etc.) .
 */
public class VirtualDataWindowKeyRange {
    private final Object start;
    private final Object end;

    /**
     * Ctor.
     *
     * @param start range start
     * @param end   range end
     */
    public VirtualDataWindowKeyRange(Object start, Object end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the start value of the range.
     *
     * @return start value
     */
    public Object getStart() {
        return start;
    }

    /**
     * Returns the end value of the range.
     *
     * @return end value
     */
    public Object getEnd() {
        return end;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VirtualDataWindowKeyRange that = (VirtualDataWindowKeyRange) o;

        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        if (start != null ? !start.equals(that.start) : that.start != null) return false;

        return true;
    }

    public int hashCode() {
        int result = start != null ? start.hashCode() : 0;
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "VirtualDataWindowKeyRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
