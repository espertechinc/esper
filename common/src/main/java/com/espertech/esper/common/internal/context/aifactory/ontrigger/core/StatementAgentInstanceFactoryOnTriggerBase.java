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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.context.activator.ViewableActivator;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirementSubquery;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceUtil;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewResult;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactory;
import com.espertech.esper.common.internal.view.core.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class StatementAgentInstanceFactoryOnTriggerBase implements StatementAgentInstanceFactory {
    private EventType resultEventType;
    private ViewableActivator activator;
    private Map<Integer, SubSelectFactory> subselects;
    private Map<Integer, ExprTableEvalStrategyFactory> tableAccesses;

    public abstract InfraOnExprBaseViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbacks, boolean isRecoveringReslient);

    public abstract View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView);

    public void setActivator(ViewableActivator activator) {
        this.activator = activator;
    }

    public void setResultEventType(EventType resultEventType) {
        this.resultEventType = resultEventType;
    }

    public void setSubselects(Map<Integer, SubSelectFactory> subselects) {
        this.subselects = subselects;
    }

    public void setTableAccesses(Map<Integer, ExprTableEvalStrategyFactory> tableAccesses) {
        this.tableAccesses = tableAccesses;
    }

    public EventType getStatementEventType() {
        return resultEventType;
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
    }

    public StatementAgentInstanceFactoryOnTriggerResult newContext(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        List<AgentInstanceStopCallback> stopCallbacks = new ArrayList<>();

        View view;
        Map<Integer, SubSelectFactoryResult> subselectActivations;
        AggregationService aggregationService;
        EvalRootState optPatternRoot;
        Map<Integer, ExprTableEvalStrategy> tableAccessEvals;
        final ViewableActivationResult activationResult;

        try {
            InfraOnExprBaseViewResult onExprViewResult = determineOnExprView(agentInstanceContext, stopCallbacks, isRecoveringResilient);
            view = onExprViewResult.getView();
            aggregationService = onExprViewResult.getOptionalAggregationService();

            // attach stream to view
            activationResult = activator.activate(agentInstanceContext, false, isRecoveringResilient);
            activationResult.getViewable().setChild(view);
            stopCallbacks.add(activationResult.getStopCallback());
            optPatternRoot = activationResult.getOptionalPatternRoot();

            // determine final output view
            view = determineFinalOutputView(agentInstanceContext, view);

            // start subselects
            subselectActivations = SubSelectHelperStart.startSubselects(subselects, agentInstanceContext, stopCallbacks, isRecoveringResilient);

            // start table-access
            tableAccessEvals = ExprTableEvalHelperStart.startTableAccess(tableAccesses, agentInstanceContext);
        } catch (RuntimeException ex) {
            AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);
            AgentInstanceUtil.stopSafe(stopCallback, agentInstanceContext);
            throw new EPException(ex.getMessage(), ex);
        }

        AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);
        return new StatementAgentInstanceFactoryOnTriggerResult(view, stopCallback, agentInstanceContext, aggregationService,
                subselectActivations, null, null, null, tableAccessEvals, null, optPatternRoot, activationResult);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        AIRegistryRequirementSubquery[] subqueries = AIRegistryRequirements.getSubqueryRequirements(subselects);
        return new AIRegistryRequirements(null, null, subqueries, tableAccesses == null ? 0 : tableAccesses.size(), false);
    }
}
