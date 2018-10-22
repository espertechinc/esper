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
 * Use this class to provide an agent wherein there is no need to write code that generates code,
 */
public class AggregationMultiFunctionAgentModeManaged implements AggregationMultiFunctionAgentMode {
    private InjectionStrategy injectionStrategyAggregationAgentFactory;

    /**
     * Returns the injection strategy for the aggregation agent factory
     *
     * @return strategy
     */
    public InjectionStrategy getInjectionStrategyAggregationAgentFactory() {
        return injectionStrategyAggregationAgentFactory;
    }

    /**
     * Sets the injection strategy for the aggregation agent factory
     *
     * @param strategy strategy
     * @return itself
     */
    public AggregationMultiFunctionAgentModeManaged setInjectionStrategyAggregationAgentFactory(InjectionStrategy strategy) {
        this.injectionStrategyAggregationAgentFactory = strategy;
        return this;
    }
}
