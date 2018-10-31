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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.StatementAgentInstancePreload;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposer;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.List;
import java.util.Map;

public class StatementAgentInstanceFactorySelectResult extends StatementAgentInstanceFactoryResult {

    private final EvalRootState[] patternRoots;
    private final JoinSetComposer joinSetComposer;
    private final Viewable[] topViews;
    private final Viewable[] eventStreamViewables;
    private final ViewableActivationResult[] viewableActivationResults;
    private final ResultSetProcessor resultSetProcessor;

    public StatementAgentInstanceFactorySelectResult(Viewable finalView,
                                                     AgentInstanceStopCallback stopCallback,
                                                     AgentInstanceContext agentInstanceContext,
                                                     AggregationService optionalAggegationService,
                                                     Map<Integer, SubSelectFactoryResult> subselectStrategies,
                                                     PriorEvalStrategy[] priorStrategies,
                                                     PreviousGetterStrategy[] previousGetterStrategies,
                                                     RowRecogPreviousStrategy regexExprPreviousEvalStrategy,
                                                     Map<Integer, ExprTableEvalStrategy> tableAccessStrategies,
                                                     List<StatementAgentInstancePreload> preloadList,
                                                     EvalRootState[] patternRoots,
                                                     JoinSetComposer joinSetComposer,
                                                     Viewable[] topViews,
                                                     Viewable[] eventStreamViewables,
                                                     ViewableActivationResult[] viewableActivationResults,
                                                     ResultSetProcessor resultSetProcessor) {
        super(finalView, stopCallback, agentInstanceContext, optionalAggegationService, subselectStrategies, priorStrategies, previousGetterStrategies, regexExprPreviousEvalStrategy, tableAccessStrategies, preloadList);
        this.topViews = topViews;
        this.patternRoots = patternRoots;
        this.joinSetComposer = joinSetComposer;
        this.eventStreamViewables = eventStreamViewables;
        this.viewableActivationResults = viewableActivationResults;
        this.resultSetProcessor = resultSetProcessor;
    }

    public Viewable[] getTopViews() {
        return topViews;
    }

    public EvalRootState[] getPatternRoots() {
        return patternRoots;
    }

    public Viewable[] getEventStreamViewables() {
        return eventStreamViewables;
    }

    public ViewableActivationResult[] getViewableActivationResults() {
        return viewableActivationResults;
    }

    public JoinSetComposer getJoinSetComposer() {
        return joinSetComposer;
    }

    public ResultSetProcessor getResultSetProcessor() {
        return resultSetProcessor;
    }
}
