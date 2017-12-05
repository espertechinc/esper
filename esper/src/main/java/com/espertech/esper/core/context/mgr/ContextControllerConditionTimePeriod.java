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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.core.service.StatementAgentInstanceFilterVersion;
import com.espertech.esper.epl.spec.ContextDetailConditionTimePeriod;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class ContextControllerConditionTimePeriod implements ContextControllerCondition {

    private static final Logger log = LoggerFactory.getLogger(ContextControllerConditionTimePeriod.class);

    private final String contextName;
    private final AgentInstanceContext agentInstanceContext;
    private final long scheduleSlot;
    private final ContextDetailConditionTimePeriod spec;
    private final ContextControllerConditionCallback callback;
    private final ContextInternalFilterAddendum filterAddendum;

    private EPStatementHandleCallback scheduleHandle;

    public ContextControllerConditionTimePeriod(String contextName, AgentInstanceContext agentInstanceContext, long scheduleSlot, ContextDetailConditionTimePeriod spec, ContextControllerConditionCallback callback, ContextInternalFilterAddendum filterAddendum) {
        this.contextName = contextName;
        this.agentInstanceContext = agentInstanceContext;
        this.scheduleSlot = scheduleSlot;
        this.spec = spec;
        this.callback = callback;
        this.filterAddendum = filterAddendum;
    }

    public void activate(EventBean optionalTriggerEvent, MatchedEventMap priorMatches, long timeOffset, boolean isRecoveringResilient) {
        startContextCallback(timeOffset);
    }

    public void deactivate() {
        endContextCallback();
    }

    public boolean isRunning() {
        return scheduleHandle != null;
    }

    public boolean isImmediate() {
        return spec.isImmediate();
    }

    private void startContextCallback(long timeOffset) {
        ScheduleHandleCallback scheduleCallback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qContextScheduledEval(ContextControllerConditionTimePeriod.this.agentInstanceContext.getStatementContext().getContextDescriptor());
                }
                scheduleHandle = null;  // terminates automatically unless scheduled again
                callback.rangeNotification(Collections.<String, Object>emptyMap(), ContextControllerConditionTimePeriod.this, null, null, null, filterAddendum);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aContextScheduledEval();
                }
            }
        };
        EPStatementAgentInstanceHandle agentHandle = new EPStatementAgentInstanceHandle(agentInstanceContext.getStatementContext().getEpStatementHandle(), agentInstanceContext.getStatementContext().getDefaultAgentInstanceLock(), -1, new StatementAgentInstanceFilterVersion(), agentInstanceContext.getStatementContext().getFilterFaultHandlerFactory());
        scheduleHandle = new EPStatementHandleCallback(agentHandle, scheduleCallback);

        long timeDelta = spec.getTimePeriod().nonconstEvaluator().deltaUseEngineTime(null, agentInstanceContext, agentInstanceContext.getTimeProvider()) - timeOffset;
        agentInstanceContext.getStatementContext().getSchedulingService().add(timeDelta, scheduleHandle, scheduleSlot);
    }

    private void endContextCallback() {
        if (scheduleHandle != null) {
            agentInstanceContext.getStatementContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
        }
        scheduleHandle = null;
    }

    public Long getExpectedEndTime() {
        return spec.getExpectedEndTime(agentInstanceContext, agentInstanceContext.getTimeProvider());
    }
}
