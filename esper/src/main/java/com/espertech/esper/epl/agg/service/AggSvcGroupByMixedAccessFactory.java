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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByMixedAccessFactory extends AggregationServiceFactoryBase {
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;

    /**
     * Ctor.
     *
     * @param evaluators         - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes         - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     *                           aggregation states for each group
     * @param accessors          accessor definitions
     * @param accessAggregations access aggs
     * @param isJoin             true for join, false for single-stream
     */
    public AggSvcGroupByMixedAccessFactory(ExprEvaluator[] evaluators,
                                           AggregationMethodFactory[] prototypes,
                                           AggregationAccessorSlotPair[] accessors,
                                           AggregationStateFactory[] accessAggregations,
                                           boolean isJoin) {
        super(evaluators, prototypes);
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        this.isJoin = isJoin;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        return new AggSvcGroupByMixedAccessImpl(evaluators, aggregators, accessors, accessAggregations, isJoin);
    }
}