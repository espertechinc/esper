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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableChangeCallback;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.DataWindowView;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewSupport;

/**
 * This view is a moving window extending the into the past until the expression passed to it returns false.
 */
public abstract class ExpressionViewBase extends ViewSupport implements DataWindowView, AgentInstanceStopCallback, VariableChangeCallback {

    protected final ExpressionViewFactoryBase factory;
    protected final ViewUpdatedCollection viewUpdatedCollection;
    protected final ObjectArrayEventBean builtinEventProps;
    protected final EventBean[] eventsPerStream;
    protected final AgentInstanceContext agentInstanceContext;
    protected final long scheduleSlot;
    protected final EPStatementHandleCallbackSchedule scheduleHandle;
    protected final AggregationService aggregationService;

    /**
     * Implemented to check the expiry expression.
     */
    public abstract void scheduleCallback();

    public ExpressionViewBase(ExpressionViewFactoryBase factory,
                              ViewUpdatedCollection viewUpdatedCollection,
                              ObjectArrayEventBean builtinEventProps,
                              AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.factory = factory;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.builtinEventProps = builtinEventProps;
        this.eventsPerStream = new EventBean[]{null, builtinEventProps};
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();

        if (factory.getVariables() != null && factory.getVariables().length > 0) {
            for (Variable variable : factory.getVariables()) {
                final String variableDepId = variable.getDeploymentId();
                final String variableName = variable.getMetaData().getVariableName();
                final int agentInstanceId = agentInstanceContext.getAgentInstanceId();
                agentInstanceContext.getStatementContext().getVariableManagementService().registerCallback(variable.getDeploymentId(), variableName, agentInstanceId, this);
                agentInstanceContext.getAgentInstanceContext().addTerminationCallback(new AgentInstanceStopCallback() {
                    public void stop(AgentInstanceStopServices services) {
                        services.getAgentInstanceContext().getVariableManagementService().unregisterCallback(variableDepId, variableName, agentInstanceId, ExpressionViewBase.this);
                    }
                });
            }

            ScheduleHandleCallback callback = new ScheduleHandleCallback() {
                public void scheduledTrigger() {
                    agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext.getAgentInstanceContext(), ScheduleObjectType.view, factory.getViewName());
                    agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                    scheduleCallback();
                    agentInstanceContext.getInstrumentationProvider().aViewScheduledEval();
                }
            };
            scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
            scheduleHandle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        } else {
            scheduleSlot = -1;
            scheduleHandle = null;
        }

        if (factory.getAggregationServiceFactory() != null) {
            aggregationService = factory.getAggregationServiceFactory().makeService(agentInstanceContext.getAgentInstanceContext(), agentInstanceContext.getClasspathImportService(), false, null, null);
        } else {
            aggregationService = null;
        }
    }

    public final EventType getEventType() {
        // The event type is the parent view's event type
        return parent.getEventType();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public void stop(AgentInstanceStopServices services) {
        stopScheduleAndVar();
        agentInstanceContext.removeTerminationCallback(this);
    }

    private void stopScheduleAndVar() {
        if (factory.getVariables() != null && factory.getVariables().length > 0) {
            for (Variable variable : factory.getVariables()) {
                agentInstanceContext.getStatementContext().getVariableManagementService().unregisterCallback(variable.getDeploymentId(), variable.getMetaData().getVariableName(), agentInstanceContext.getAgentInstanceId(), this);
            }

            if (agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
                agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getStatementContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
            }
        }
    }

    // Handle variable updates by scheduling a re-evaluation with timers
    public void update(Object newValue, Object oldValue) {
        if (!agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
            agentInstanceContext.getAuditProvider().scheduleAdd(0, agentInstanceContext, scheduleHandle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);
        }
    }

    public ViewUpdatedCollection getViewUpdatedCollection() {
        return viewUpdatedCollection;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
