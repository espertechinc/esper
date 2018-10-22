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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableChangeCallback;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionExpression extends OutputConditionBase implements OutputCondition, VariableChangeCallback {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "expression";
    private static final Logger log = LoggerFactory.getLogger(OutputConditionExpression.class);
    private final AgentInstanceContext agentInstanceContext;
    private final OutputConditionExpressionFactory parent;

    private long scheduleSlot;
    private boolean isCallbackScheduled;
    private boolean ignoreVariableCallbacks;
    private ObjectArrayEventBean builtinProperties;
    private EventBean[] eventsPerStream;

    // ongoing builtin properties
    private int totalNewEventsCount;
    private int totalOldEventsCount;
    private int totalNewEventsSum;
    private int totalOldEventsSum;
    private Long lastOutputTimestamp;
    private EPStatementHandleCallbackSchedule scheduleHandle;

    public OutputConditionExpression(OutputCallback outputCallback, final AgentInstanceContext agentInstanceContext, OutputConditionExpressionFactory parent) {
        super(outputCallback);
        this.agentInstanceContext = agentInstanceContext;
        this.parent = parent;

        scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.eventsPerStream = new EventBean[1];

        if (parent.getBuiltinPropertiesEventType() != null) {
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), parent.getBuiltinPropertiesEventType());
            lastOutputTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        }

        if (parent.getVariables() != null && parent.getVariables().length > 0) {
            // if using variables, register a callback on the change of the variable
            for (Variable variable : parent.getVariables()) {
                final String theVariableDepId = variable.getDeploymentId();
                final String theVariableName = variable.getMetaData().getVariableName();
                agentInstanceContext.getVariableManagementService().registerCallback(theVariableDepId, theVariableName, agentInstanceContext.getAgentInstanceId(), this);
                agentInstanceContext.addTerminationCallback(new AgentInstanceStopCallback() {
                    public void stop(AgentInstanceStopServices services) {
                        services.getAgentInstanceContext().getVariableManagementService().unregisterCallback(theVariableDepId, theVariableName, agentInstanceContext.getAgentInstanceId(), OutputConditionExpression.this);
                    }
                });
            }
        }

        if (parent.isStartConditionOnCreation()) {
            update(0, 0);
        }
    }

    public void updateOutputCondition(int newEventsCount, int oldEventsCount) {
        this.totalNewEventsCount += newEventsCount;
        this.totalOldEventsCount += oldEventsCount;
        this.totalNewEventsSum += newEventsCount;
        this.totalOldEventsSum += oldEventsCount;

        boolean isOutput = evaluate(parent.getWhenExpressionNodeEval());
        if (isOutput) {
            executeThenAssignments();
            outputCallback.continueOutputProcessing(true, true);
            resetBuiltinProperties();
        }
    }

    public void update(Object newValue, Object oldValue) {
        if (ignoreVariableCallbacks) {
            log.debug(".update Ignoring variable callback");
            return;
        }

        agentInstanceContext.getVariableManagementService().setLocalVersion();
        boolean isOutput = evaluate(parent.getWhenExpressionNodeEval());
        if (isOutput && (!isCallbackScheduled)) {
            scheduleCallback();
        }
    }

    public void stopOutputCondition() {
        if (scheduleHandle != null) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
            agentInstanceContext.getStatementContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
            scheduleHandle = null;
        }
    }

    @Override
    public void terminated() {
        boolean output = true;
        if (parent.getAndWhenTerminatedExpressionNodeEval() != null) {
            output = evaluate(parent.getAndWhenTerminatedExpressionNodeEval());
        }
        if (parent.getVariableReadWritePackageAfterTerminated() != null) {
            if (builtinProperties != null) {
                populateBuiltinProps();
                eventsPerStream[0] = builtinProperties;
            }

            ignoreVariableCallbacks = true;
            try {
                parent.getVariableReadWritePackageAfterTerminated().writeVariables(eventsPerStream, null, agentInstanceContext);
            } finally {
                ignoreVariableCallbacks = false;
            }
        }
        if (output) {
            super.terminated();
        }
    }

    private boolean evaluate(ExprEvaluator evaluator) {
        if (builtinProperties != null) {
            populateBuiltinProps();
            eventsPerStream[0] = builtinProperties;
        }

        boolean result = false;
        Boolean output = (Boolean) evaluator.evaluate(eventsPerStream, true, agentInstanceContext);
        if ((output != null) && output) {
            result = true;
        }

        return result;
    }

    private void scheduleCallback() {
        isCallbackScheduled = true;

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getInstrumentationProvider().qOutputRateConditionScheduledEval();
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
                OutputConditionExpression.this.isCallbackScheduled = false;
                OutputConditionExpression.this.outputCallback.continueOutputProcessing(true, true);
                resetBuiltinProperties();
                agentInstanceContext.getInstrumentationProvider().aOutputRateConditionScheduledEval();
            }
        };
        scheduleHandle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceContext.getAuditProvider().scheduleAdd(0, agentInstanceContext, scheduleHandle, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);

        // execute assignments
        executeThenAssignments();
    }

    private void executeThenAssignments() {
        if (parent.getVariableReadWritePackage() != null) {
            if (builtinProperties != null) {
                populateBuiltinProps();
                eventsPerStream[0] = builtinProperties;
            }

            ignoreVariableCallbacks = true;
            try {
                parent.getVariableReadWritePackage().writeVariables(eventsPerStream, null, agentInstanceContext);
            } finally {
                ignoreVariableCallbacks = false;
            }
        }
    }

    private void resetBuiltinProperties() {
        if (builtinProperties != null) {
            totalNewEventsCount = 0;
            totalOldEventsCount = 0;
            lastOutputTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        }
    }

    private void populateBuiltinProps() {
        OutputConditionExpressionTypeUtil.populate(builtinProperties.getProperties(), totalNewEventsCount, totalOldEventsCount, totalNewEventsSum, totalOldEventsSum, lastOutputTimestamp);
    }
}
