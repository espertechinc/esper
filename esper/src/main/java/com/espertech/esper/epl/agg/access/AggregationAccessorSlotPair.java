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
package com.espertech.esper.epl.agg.access;

/**
 * For handling access aggregation functions "first, last, window" a pair of slow and accessor.
 */
public class AggregationAccessorSlotPair {
    private final int slot;
    private final AggregationAccessor accessor;

    /**
     * Ctor.
     *
     * @param slot     number of accessor
     * @param accessor accessor
     */
    public AggregationAccessorSlotPair(int slot, AggregationAccessor accessor) {
        this.slot = slot;
        this.accessor = accessor;
    }

    /**
     * Returns the slot.
     *
     * @return slow
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns the accessor.
     *
     * @return accessor
     */
    public AggregationAccessor getAccessor() {
        return accessor;
    }
}
