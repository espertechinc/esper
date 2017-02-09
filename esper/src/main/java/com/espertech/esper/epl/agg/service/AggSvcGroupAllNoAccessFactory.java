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
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllNoAccessFactory extends AggregationServiceFactoryBase {
    public AggSvcGroupAllNoAccessFactory(ExprEvaluator[] evaluators, AggregationMethodFactory[] aggregators) {
        super(evaluators, aggregators);
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {

        AggregationMethod[] aggregatorsAgentInstance = AggSvcGroupByUtil.newAggregators(super.aggregators);
        return new AggSvcGroupAllNoAccessImpl(evaluators, aggregatorsAgentInstance, aggregators);
    }
}
