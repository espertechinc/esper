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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedFactory implements AggregationServiceFactory {
    protected final ExprEvaluator[] evaluators;
    protected final AggregationMethodFactory[] aggregators;
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;
    protected final AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionMaxAge;
    protected final AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionFrequency;

    public AggSvcGroupByReclaimAgedFactory(AggregationRowStateEvalDesc rowStateEvalDesc, boolean isJoin, AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionMaxAge, AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionFrequency) {
        this.evaluators = rowStateEvalDesc.getMethodEvals();
        this.aggregators = rowStateEvalDesc.getMethodFactories();
        this.accessors = rowStateEvalDesc.getAccessAccessors();
        this.accessAggregations = rowStateEvalDesc.getAccessFactories();
        this.isJoin = isJoin;
        this.evaluationFunctionMaxAge = evaluationFunctionMaxAge;
        this.evaluationFunctionFrequency = evaluationFunctionFrequency;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        AggSvcGroupByReclaimAgedEvalFunc max = evaluationFunctionMaxAge.make(agentInstanceContext);
        AggSvcGroupByReclaimAgedEvalFunc freq = evaluationFunctionFrequency.make(agentInstanceContext);
        return new AggSvcGroupByReclaimAgedImpl(evaluators, aggregators, accessors, accessAggregations, isJoin, max, freq, agentInstanceContext.getStatementContext().getTimeAbacus());
    }
}
