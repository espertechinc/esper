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

import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodFactory;

/**
 * Use this class to provide a fully code-generated aggregation function by implementing the internal
 * {@link AggregatorMethodFactory} interface.
 */
public class AggregationFunctionModeCodeGenerated implements AggregationFunctionMode {
    private AggregatorMethodFactory aggregatorMethodFactory;

    /**
     * Returns the aggregation method factory.
     *
     * @return factory
     */
    public AggregatorMethodFactory getAggregatorMethodFactory() {
        return aggregatorMethodFactory;
    }

    /**
     * Sets the aggregation method factory
     *
     * @param aggregatorMethodFactory factory
     * @return itself
     */
    public AggregationFunctionModeCodeGenerated setAggregatorMethodFactory(AggregatorMethodFactory aggregatorMethodFactory) {
        this.aggregatorMethodFactory = aggregatorMethodFactory;
        return this;
    }
}
