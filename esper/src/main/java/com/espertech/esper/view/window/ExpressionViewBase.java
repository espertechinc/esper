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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.variable.VariableChangeCallback;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.DataWindowView;
import com.espertech.esper.view.StoppableView;
import com.espertech.esper.view.ViewSupport;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This view is a moving window extending the into the past until the expression passed to it returns false.
 */
public abstract class ExpressionViewBase extends ViewSupport implements DataWindowView, StoppableView, VariableChangeCallback, StopCallback {

    protected final ViewUpdatedCollection viewUpdatedCollection;
    protected final ExprEvaluator expiryExpression;
    protected final ObjectArrayEventBean builtinEventProps;
    protected final EventBean[] eventsPerStream;
    protected final Set<String> variableNames;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    protected final long scheduleSlot;
    protected final EPStatementHandleCallback scheduleHandle;
    protected final AggregationService aggregationService;
    protected final List<AggregationServiceAggExpressionDesc> aggregateNodes;

    /**
     * Implemented to check the expiry expression.
     */
    public abstract void scheduleCallback();

    public abstract String getViewName();

    public ExpressionViewBase(ViewUpdatedCollection viewUpdatedCollection,
                              ExprEvaluator expiryExpression,
                              AggregationServiceFactoryDesc aggregationServiceFactoryDesc,
                              ObjectArrayEventBean builtinEventProps,
                              Set<String> variableNames,
                              AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.expiryExpression = expiryExpression;
        this.builtinEventProps = builtinEventProps;
        this.eventsPerStream = new EventBean[]{null, builtinEventProps};
        this.variableNames = variableNames;
        this.agentInstanceContext = agentInstanceContext;

        if (variableNames != null && !variableNames.isEmpty()) {
            for (String variable : variableNames) {
                final String variableName = variable;
                final int agentInstanceId = agentInstanceContext.getAgentInstanceId();
                final VariableService variableService = agentInstanceContext.getStatementContext().getVariableService();
                agentInstanceContext.getStatementContext().getVariableService().registerCallback(variable, agentInstanceId, this);
                agentInstanceContext.addTerminationCallback(new StopCallback() {
                    public void stop() {
                        variableService.unregisterCallback(variableName, agentInstanceId, ExpressionViewBase.this);
                    }
                });
            }

            ScheduleHandleCallback callback = new ScheduleHandleCallback() {
                public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qViewScheduledEval(ExpressionViewBase.this, getViewName());
                    }
                    scheduleCallback();
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aViewScheduledEval();
                    }
                }
            };
            scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
            scheduleHandle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
            agentInstanceContext.addTerminationCallback(this);
        } else {
            scheduleSlot = -1;
            scheduleHandle = null;
        }

        if (aggregationServiceFactoryDesc != null) {
            aggregationService = aggregationServiceFactoryDesc.getAggregationServiceFactory().makeService(agentInstanceContext.getAgentInstanceContext(), agentInstanceContext.getAgentInstanceContext().getStatementContext().getEngineImportService(), false, null);
            aggregateNodes = aggregationServiceFactoryDesc.getExpressions();
        } else {
            aggregationService = null;
            aggregateNodes = Collections.emptyList();
        }
    }

    public final EventType getEventType() {
        // The event type is the parent view's event type
        return parent.getEventType();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public void stopView() {
        stopScheduleAndVar();
        agentInstanceContext.removeTerminationCallback(this);
    }

    public void stop() {
        stopScheduleAndVar();
    }

    public void stopScheduleAndVar() {
        if (variableNames != null && !variableNames.isEmpty()) {
            for (String variable : variableNames) {
                agentInstanceContext.getStatementContext().getVariableService().unregisterCallback(variable, agentInstanceContext.getAgentInstanceId(), this);
            }

            if (agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
                agentInstanceContext.getStatementContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
            }
        }
    }

    // Handle variable updates by scheduling a re-evaluation with timers
    public void update(Object newValue, Object oldValue) {
        if (!agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
            agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);
        }
    }

    public ViewUpdatedCollection getViewUpdatedCollection() {
        return viewUpdatedCollection;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }
}
