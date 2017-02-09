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
package com.espertech.esper.plugin;

import com.espertech.esper.epl.agg.access.AggregationState;

/**
 * State factory responsible for allocating a state object for each group when used with group-by.
 */
public interface PlugInAggregationMultiFunctionStateFactory {
    /**
     * Return a new aggregation state holder for a given group (or ungrouped if there is no group-by).
     *
     * @param stateContext context includes group key
     * @return state holder, cannot be a null value
     */
    public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext);
}
