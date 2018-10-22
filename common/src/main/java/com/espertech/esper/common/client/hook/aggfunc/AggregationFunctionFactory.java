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
package com.espertech.esper.common.client.hook.aggfunc;

/**
 * Interface to implement for factories of aggregation functions.
 */
public interface AggregationFunctionFactory {

    /**
     * Make a new initalized aggregation state.
     *
     * @param ctx contextual information
     * @return initialized aggregator
     */
    public AggregationFunction newAggregator(AggregationFunctionFactoryContext ctx);
}
