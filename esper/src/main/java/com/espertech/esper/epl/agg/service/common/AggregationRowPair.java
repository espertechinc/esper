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
 * Pair of aggregation methods and states (first/last/window) data window representations.
 */
public class AggregationRowPair {
    private final AggregationMethod[] methods;
    private final AggregationState[] states;

    /**
     * Ctor.
     *
     * @param methods aggregation methods/state
     * @param states  access is data window representations
     */
    public AggregationRowPair(AggregationMethod[] methods, AggregationState[] states) {
        this.methods = methods;
        this.states = states;
    }

    /**
     * Returns aggregation methods.
     *
     * @return aggregation methods
     */
    public AggregationMethod[] getMethods() {
        return methods;
    }

    /**
     * Returns states to data window state.
     *
     * @return states
     */
    public AggregationState[] getStates() {
        return states;
    }
}
