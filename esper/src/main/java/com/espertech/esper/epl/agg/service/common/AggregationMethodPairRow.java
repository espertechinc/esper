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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

/**
 * A row in aggregation state.
 */
public class AggregationMethodPairRow {
    private long refcount;
    private final AggregationMethod[] methods;
    private final AggregationState[] states;

    /**
     * Ctor.
     *
     * @param refcount number of items in state
     * @param methods  aggregations
     * @param states   for first/last/window type access
     */
    public AggregationMethodPairRow(long refcount, AggregationMethod[] methods, AggregationState[] states) {
        this.refcount = refcount;
        this.methods = methods;
        this.states = states;
    }

    /**
     * Returns number of data points.
     *
     * @return data points
     */
    public long getRefcount() {
        return refcount;
    }

    /**
     * Returns aggregation state.
     *
     * @return state
     */
    public AggregationMethod[] getMethods() {
        return methods;
    }

    /**
     * Increase number of data points by one.
     */
    public void increaseRefcount() {
        refcount++;
    }

    /**
     * Decrease number of data points by one.
     */
    public void decreaseRefcount() {
        refcount--;
    }

    /**
     * Returns the states for first/last/window aggregation functions.
     *
     * @return states
     */
    public AggregationState[] getStates() {
        return states;
    }
}