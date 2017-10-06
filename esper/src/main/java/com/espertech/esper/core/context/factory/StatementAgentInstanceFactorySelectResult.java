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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.core.context.activator.ViewableActivationResult;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.Viewable;

import java.util.List;
import java.util.Map;

public class StatementAgentInstanceFactorySelectResult extends StatementAgentInstanceFactoryResult {

    private final EvalRootState[] patternRoots;
    private final StatementAgentInstancePostLoad optionalPostLoadJoin;
    private final Viewable[] topViews;
    private final Viewable[] eventStreamViewables;
    private final ViewableActivationResult[] viewableActivationResults;

    public StatementAgentInstanceFactorySelectResult(Viewable finalView,
                                                     StopCallback stopCallback,
                                                     AgentInstanceContext agentInstanceContext,
                                                     AggregationService optionalAggegationService,
                                                     Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies,
                                                     Map<ExprPriorNode, ExprPriorEvalStrategy> priorNodeStrategies,
                                                     Map<ExprPreviousNode, ExprPreviousEvalStrategy> previousNodeStrategies,
                                                     RegexExprPreviousEvalStrategy regexExprPreviousEvalStrategy,
                                                     Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> tableAccessStrategies,
                                                     List<StatementAgentInstancePreload> preloadList,
                                                     EvalRootState[] patternRoots,
                                                     StatementAgentInstancePostLoad optionalPostLoadJoin,
                                                     Viewable[] topViews,
                                                     Viewable[] eventStreamViewables,
                                                     ViewableActivationResult[] viewableActivationResults) {
        super(finalView, stopCallback, agentInstanceContext, optionalAggegationService, subselectStrategies, priorNodeStrategies, previousNodeStrategies, regexExprPreviousEvalStrategy, tableAccessStrategies, preloadList);
        this.topViews = topViews;
        this.patternRoots = patternRoots;
        this.optionalPostLoadJoin = optionalPostLoadJoin;
        this.eventStreamViewables = eventStreamViewables;
        this.viewableActivationResults = viewableActivationResults;
    }

    public Viewable[] getTopViews() {
        return topViews;
    }

    public EvalRootState[] getPatternRoots() {
        return patternRoots;
    }

    public StatementAgentInstancePostLoad getOptionalPostLoadJoin() {
        return optionalPostLoadJoin;
    }

    public Viewable[] getEventStreamViewables() {
        return eventStreamViewables;
    }

    public ViewableActivationResult[] getViewableActivationResults() {
        return viewableActivationResults;
    }
}
