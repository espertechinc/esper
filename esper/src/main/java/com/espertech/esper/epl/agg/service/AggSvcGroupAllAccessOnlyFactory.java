/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.agg.service;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.core.MethodResolutionService;

/**
 * Aggregation service for use when only first/last/window aggregation functions are used an none other.
 */
public class AggSvcGroupAllAccessOnlyFactory implements AggregationServiceFactory
{
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggSpecs;
    protected final boolean isJoin;

    public AggSvcGroupAllAccessOnlyFactory(AggregationAccessorSlotPair[] accessors, AggregationStateFactory[] accessAggSpecs, boolean join) {
        this.accessors = accessors;
        this.accessAggSpecs = accessAggSpecs;
        isJoin = join;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, MethodResolutionService methodResolutionService) {
        AggregationState[] states = methodResolutionService.newAccesses(agentInstanceContext.getAgentInstanceId(), isJoin, accessAggSpecs);
        return new AggSvcGroupAllAccessOnlyImpl(accessors, states, accessAggSpecs);
    }
}