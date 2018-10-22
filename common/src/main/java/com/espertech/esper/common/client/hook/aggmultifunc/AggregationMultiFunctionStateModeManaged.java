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
package com.espertech.esper.common.client.hook.aggmultifunc;

import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategy;

/**
 * Use this class to provide an state factory wherein there is no need to write code that generates code,
 */
public class AggregationMultiFunctionStateModeManaged implements AggregationMultiFunctionStateMode {
    private InjectionStrategy injectionStrategyAggregationStateFactory;
    private boolean hasHA;
    private Class serde;

    /**
     * Returns the injection strategy for the aggregation state factory
     *
     * @return strategy
     */
    public InjectionStrategy getInjectionStrategyAggregationStateFactory() {
        return injectionStrategyAggregationStateFactory;
    }

    /**
     * Sets the injection strategy for the aggregation state factory
     *
     * @param strategy strategy
     * @return itself
     */
    public AggregationMultiFunctionStateModeManaged setInjectionStrategyAggregationStateFactory(InjectionStrategy strategy) {
        this.injectionStrategyAggregationStateFactory = strategy;
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
    public AggregationMultiFunctionStateModeManaged setHasHA(boolean hasHA) {
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
     * @return ifself
     */
    public AggregationMultiFunctionStateModeManaged setSerde(Class serde) {
        this.serde = serde;
        return this;
    }
}
