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

import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategy;

/**
 * Use this class to provide an aggregation function wherein there is no need to write code that generates code,
 * wherein the aggregation function takes a multiple parameters and
 * wherein the compiler does not handle filtering and distinct.
 */
public class AggregationFunctionModeMultiParam implements AggregationFunctionMode {
    private InjectionStrategy injectionStrategyAggregationFunctionFactory;
    private boolean hasHA;
    private Class serde;

    /**
     * Returns the injection strategy for the aggregation function factory
     *
     * @return strategy
     */
    public InjectionStrategy getInjectionStrategyAggregationFunctionFactory() {
        return injectionStrategyAggregationFunctionFactory;
    }

    /**
     * Sets the injection strategy for the aggregation function factory
     *
     * @param strategy strategy
     * @return itself
     */
    public AggregationFunctionModeMultiParam setInjectionStrategyAggregationFunctionFactory(InjectionStrategy strategy) {
        this.injectionStrategyAggregationFunctionFactory = strategy;
        return this;
    }

    /**
     * Returns indicator whether a serializer-deserialize to provide read and write methods is provided by {@link #getSerde}
     *
     * @return ha-indicator
     */
    public boolean isHasHA() {
        return hasHA;
    }

    /**
     * Sets indicator whether a serializer-deserialize to provide read and write methods is provided by {@link #getSerde}
     *
     * @param hasHA ha-indicator
     * @return itself
     */
    public AggregationFunctionModeMultiParam setHasHA(boolean hasHA) {
        this.hasHA = hasHA;
        return this;
    }

    /**
     * Returns the class providing the serde
     *
     * @return serde class
     */
    public Class getSerde() {
        return serde;
    }

    /**
     * Sets the class providing the serde
     *
     * @param serde serde class
     * @return itself
     */
    public AggregationFunctionModeMultiParam setSerde(Class serde) {
        this.serde = serde;
        return this;
    }
}
