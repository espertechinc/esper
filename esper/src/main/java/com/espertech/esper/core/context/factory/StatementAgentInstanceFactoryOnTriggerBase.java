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

package com.espertech.esper.core.context.factory;

import com.espertech.esper.core.context.activator.ViewableActivationResult;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.start.EPStatementStartMethodCreateWindow;
import com.espertech.esper.core.start.EPStatementStartMethodHelperTableAccess;
import com.espertech.esper.core.start.EPStatementStartMethodHelperSubselect;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.spec.StatementSpecCompiled;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class StatementAgentInstanceFactoryOnTriggerBase implements StatementAgentInstanceFactory {
    private static final Log log = LogFactory.getLog(EPStatementStartMethodCreateWindow.class);

    protected final StatementContext statementContext;
    protected final StatementSpecCompiled statementSpec;
    protected final EPServicesContext services;
    private final ViewableActivator activator;
    private final SubSelectStrategyCollection subSelectStrategyCollection;

    public abstract OnExprViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<StopCallback> stopCallbacks);
    public abstract View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView);

    public StatementAgentInstanceFactoryOnTriggerBase(StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, ViewableActivator activator, SubSelectStrategyCollection subSelectStrategyCollection) {
        this.statementContext = statementContext;
        this.statementSpec = statementSpec;
        this.services = services;
        this.activator = activator;
        this.subSelectStrategyCollection = subSelectStrategyCollection;
    }

    public StatementAgentInstanceFactoryOnTriggerResult newContext(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient)
    {
        List<StopCallback> stopCallbacks = new ArrayList<StopCallback>();
        View view;
        Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies;
        AggregationService aggregationService;
        EvalRootState optPatternRoot;
        Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> tableAccessStrategies;

        try {
            if (services.getSchedulableAgentInstanceDirectory() != null) {
                services.getSchedulableAgentInstanceDirectory().add(agentInstanceContext.getEpStatementAgentInstanceHandle());
            }

            OnExprViewResult onExprViewResult = determineOnExprView(agentInstanceContext, stopCallbacks);
            view = onExprViewResult.getOnExprView();
            aggregationService = onExprViewResult.getOptionalAggregationService();

            // attach stream to view
            final ViewableActivationResult activationResult = activator.activate(agentInstanceContext, false, isRecoveringResilient);
            activationResult.getViewable().addView(view);
            stopCallbacks.add(activationResult.getStopCallback());
            optPatternRoot = activationResult.getOptionalPatternRoot();

            // determine final output view
            view = determineFinalOutputView(agentInstanceContext, view);

            // start subselects
            subselectStrategies = EPStatementStartMethodHelperSubselect.startSubselects(services, subSelectStrategyCollection, agentInstanceContext, stopCallbacks);

            // plan table access
            tableAccessStrategies = EPStatementStartMethodHelperTableAccess.attachTableAccess(services, agentInstanceContext, statementSpec.getTableNodes());
        }
        catch (RuntimeException ex) {
            StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
            StatementAgentInstanceUtil.stopSafe(stopCallback, statementContext);
            throw ex;
        }

        log.debug(".start Statement start completed");
        StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
        return new StatementAgentInstanceFactoryOnTriggerResult(view, stopCallback, agentInstanceContext, aggregationService, subselectStrategies, optPatternRoot, tableAccessStrategies);
    }

    public void assignExpressions(StatementAgentInstanceFactoryResult result) {
    }

    public void unassignExpressions() {
    }

    public static class OnExprViewResult {
        private final View onExprView;
        private final AggregationService optionalAggregationService;

        public OnExprViewResult(View onExprView, AggregationService optionalAggregationService) {
            this.onExprView = onExprView;
            this.optionalAggregationService = optionalAggregationService;
        }

        public View getOnExprView() {
            return onExprView;
        }

        public AggregationService getOptionalAggregationService() {
            return optionalAggregationService;
        }
    }
}
