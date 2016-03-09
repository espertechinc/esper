/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByRefcountedWAccessFactory extends AggregationServiceFactoryBase
{
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;

    /**
     * Ctor.
     * @param evaluators - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     * aggregation states for each group
     * @param accessors accessor definitions
     * @param accessAggregations access aggs
     * @param isJoin true for join, false for single-stream
     */
    public AggSvcGroupByRefcountedWAccessFactory(ExprEvaluator evaluators[],
                                                 AggregationMethodFactory prototypes[],
                                                 Object groupKeyBinding,
                                                 AggregationAccessorSlotPair[] accessors,
                                                 AggregationStateFactory[] accessAggregations,
                                                 boolean isJoin)
    {
        super(evaluators, prototypes, groupKeyBinding);
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        this.isJoin = isJoin;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, MethodResolutionService methodResolutionService, boolean isSubquery, Integer subqueryNumber) {
        return new AggSvcGroupByRefcountedWAccessImpl(evaluators, aggregators, groupKeyBinding, methodResolutionService, accessors, accessAggregations, isJoin);
    }
}
