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
 * Use this class to provide an accessor wherein there is no need to write code that generates code,
 */
public class AggregationMultiFunctionAccessorModeManaged implements AggregationMultiFunctionAccessorMode {
    private InjectionStrategy injectionStrategyAggregationAccessorFactory;

    /**
     * Returns the injection strategy for the aggregation accessor factory
     *
     * @return strategy
     */
    public InjectionStrategy getInjectionStrategyAggregationAccessorFactory() {
        return injectionStrategyAggregationAccessorFactory;
    }

    /**
     * Sets the injection strategy for the aggregation accessor factory
     *
     * @param strategy strategy
     * @return itself
     */
    public AggregationMultiFunctionAccessorModeManaged setInjectionStrategyAggregationAccessorFactory(InjectionStrategy strategy) {
        this.injectionStrategyAggregationAccessorFactory = strategy;
        return this;
    }
}
