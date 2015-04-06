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
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.core.MethodResolutionService;

/**
 * Aggregation service for use when only first/last/window aggregation functions are used an none other.
 */
public class AggSvcGroupByAccessOnlyFactory implements AggregationServiceFactory
{
    private final AggregationAccessorSlotPair[] accessors;
    private final AggregationStateFactory[] accessAggSpecs;
    private final Object groupKeyBinding;
    private final boolean isJoin;

    /**
     * Ctor.
     * @param accessors accessor definitions
     * @param accessAggSpecs access aggregations
     * @param isJoin true for join, false for single-stream
     */
    public AggSvcGroupByAccessOnlyFactory(AggregationAccessorSlotPair[] accessors,
                                          AggregationStateFactory[] accessAggSpecs,
                                          Object groupKeyBinding,
                                          boolean isJoin)
    {
        this.accessors = accessors;
        this.accessAggSpecs = accessAggSpecs;
        this.groupKeyBinding = groupKeyBinding;
        this.isJoin = isJoin;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, MethodResolutionService methodResolutionService) {
        return new AggSvcGroupByAccessOnlyImpl(methodResolutionService, groupKeyBinding, accessors, accessAggSpecs, isJoin);
    }
}
