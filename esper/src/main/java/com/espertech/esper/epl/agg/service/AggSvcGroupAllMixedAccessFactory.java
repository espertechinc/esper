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
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllMixedAccessFactory extends AggregationServiceFactoryBase
{
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;

    public AggSvcGroupAllMixedAccessFactory(ExprEvaluator evaluators[], AggregationMethodFactory aggregators[], Object groupKeyBinding, AggregationAccessorSlotPair[] accessors, AggregationStateFactory[] accessAggregations, boolean join) {
        super(evaluators, aggregators, groupKeyBinding);
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        isJoin = join;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, MethodResolutionService methodResolutionService, boolean isSubquery, Integer subqueryNumber) {
        AggregationState[] states = AggSvcGroupByUtil.newAccesses(agentInstanceContext.getAgentInstanceId(), isJoin, accessAggregations, null, null);
        AggregationMethod[] aggregatorsAgentInstance = AggSvcGroupByUtil.newAggregators(super.aggregators);
        return new AggSvcGroupAllMixedAccessImpl(evaluators, aggregatorsAgentInstance, accessors, states, aggregators, accessAggregations);
    }
}