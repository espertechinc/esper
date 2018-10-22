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
package com.espertech.esper.common.internal.context.aifactory.createcontext;

import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.StatementAgentInstancePreload;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.List;
import java.util.Map;

public class StatementAgentInstanceFactoryCreateContextResult extends StatementAgentInstanceFactoryResult {

    private final ContextManagerRealization contextManagerRealization;

    public StatementAgentInstanceFactoryCreateContextResult(Viewable finalView, AgentInstanceStopCallback stopCallback, AgentInstanceContext agentInstanceContext, AggregationService optionalAggegationService, Map<Integer, SubSelectFactoryResult> subselectStrategies, PriorEvalStrategy[] priorStrategies, PreviousGetterStrategy[] previousGetterStrategies, RowRecogPreviousStrategy regexExprPreviousEvalStrategy, Map<Integer, ExprTableEvalStrategy> tableAccessStrategies, List<StatementAgentInstancePreload> preloadList, ContextManagerRealization contextManagerRealization) {
        super(finalView, stopCallback, agentInstanceContext, optionalAggegationService, subselectStrategies, priorStrategies, previousGetterStrategies, regexExprPreviousEvalStrategy, tableAccessStrategies, preloadList);
        this.contextManagerRealization = contextManagerRealization;
    }

    public ContextManagerRealization getContextManagerRealization() {
        return contextManagerRealization;
    }
}
