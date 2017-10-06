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
 * For handling access aggregation functions "first, last, window" a pair of slow and accessorForge.
 */
public class AggregationAccessorSlotPairForge {
    private final int slot;
    private final AggregationAccessorForge accessorForge;

    /**
     * Ctor.
     *
     * @param slot     number of accessorForge
     * @param accessorForge accessorForge
     */
    public AggregationAccessorSlotPairForge(int slot, AggregationAccessorForge accessorForge) {
        this.slot = slot;
        this.accessorForge = accessorForge;
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
     * Returns the accessorForge.
     *
     * @return accessorForge
     */
    public AggregationAccessorForge getAccessorForge() {
        return accessorForge;
    }
}
