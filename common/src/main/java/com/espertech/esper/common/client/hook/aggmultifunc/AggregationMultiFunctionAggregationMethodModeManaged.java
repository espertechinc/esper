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
 * Use this class to provide a aggregation method wherein there is no need to write code that generates code,
 */
public class AggregationMultiFunctionAggregationMethodModeManaged implements AggregationMultiFunctionAggregationMethodMode {
    private InjectionStrategy injectionStrategyAggregationMethodFactory;

    /**
     * Returns the injection strategy for the aggregation table reader factory
     *
     * @return strategy
     */
    public InjectionStrategy getInjectionStrategyAggregationMethodFactory() {
        return injectionStrategyAggregationMethodFactory;
    }

    /**
     * Sets the injection strategy for the aggregation table reader factory
     *
     * @param strategy strategy
     * @return itself
     */
    public AggregationMultiFunctionAggregationMethodModeManaged setInjectionStrategyAggregationMethodFactory(InjectionStrategy strategy) {
        this.injectionStrategyAggregationMethodFactory = strategy;
        return this;
    }
}
