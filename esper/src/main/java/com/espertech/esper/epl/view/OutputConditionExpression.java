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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.variable.VariableChangeCallback;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.StopCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionExpression extends OutputConditionBase implements OutputCondition, VariableChangeCallback, StopCallback {
    private static final Logger log = LoggerFactory.getLogger(OutputConditionExpression.class);
    private final AgentInstanceContext agentInstanceContext;
    private final OutputConditionExpressionFactory parent;

    private final long scheduleSlot;
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
    private EPStatementHandleCallback scheduleHandle;

    public OutputConditionExpression(OutputCallback outputCallback, final AgentInstanceContext agentInstanceContext, OutputConditionExpressionFactory parent, boolean isStartConditionOnCreation) {
        super(outputCallback);
        this.agentInstanceContext = agentInstanceContext;
        this.parent = parent;

        scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.eventsPerStream = new EventBean[1];

        if (parent.getBuiltinPropertiesEventType() != null) {
            builtinProperties = new ObjectArrayEventBean(OutputConditionExpressionTypeUtil.getOAPrototype(), parent.getBuiltinPropertiesEventType());
            lastOutputTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        }

        if (parent.getVariableNames() != null) {
            // if using variables, register a callback on the change of the variable
            for (String variableName : parent.getVariableNames()) {
                final String theVariableName = variableName;
                agentInstanceContext.getStatementContext().getVariableService().registerCallback(variableName, agentInstanceContext.getAgentInstanceId(), this);
                agentInstanceContext.addTerminationCallback(new StopCallback() {
                    public void stop() {
                        agentInstanceContext.getStatementContext().getVariableService().unregisterCallback(theVariableName, agentInstanceContext.getAgentInstanceId(), OutputConditionExpression.this);
                    }
                });
            }
        }

        if (isStartConditionOnCreation) {
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

        agentInstanceContext.getStatementContext().getVariableService().setLocalVersion();
        boolean isOutput = evaluate(parent.getWhenExpressionNodeEval());
        if (isOutput && (!isCallbackScheduled)) {
            scheduleCallback();
        }
    }

    public void stop() {
        if (scheduleHandle != null) {
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
                parent.getVariableReadWritePackageAfterTerminated().writeVariables(agentInstanceContext.getStatementContext().getVariableService(), eventsPerStream, null, agentInstanceContext);
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
        long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".scheduleCallback Scheduled new callback for " +
                    " afterMsec=" + 0 +
                    " now=" + current);
        }

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOutputRateConditionScheduledEval();
                }
                OutputConditionExpression.this.isCallbackScheduled = false;
                OutputConditionExpression.this.outputCallback.continueOutputProcessing(true, true);
                resetBuiltinProperties();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOutputRateConditionScheduledEval();
                }
            }
        };
        scheduleHandle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);
        agentInstanceContext.addTerminationCallback(this);

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
                parent.getVariableReadWritePackage().writeVariables(agentInstanceContext.getStatementContext().getVariableService(), eventsPerStream, null, agentInstanceContext);
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
