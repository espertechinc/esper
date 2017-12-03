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
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.core.service.StatementAgentInstanceFilterVersion;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.spec.ContextDetailConditionCrontab;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.schedule.ScheduleComputeHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.SchedulingService;

import java.util.Collections;

public class ContextControllerConditionCrontab implements ContextControllerCondition {

    private final StatementContext statementContext;
    private final long scheduleSlot;
    private final ContextDetailConditionCrontab spec;
    private final ContextControllerConditionCallback callback;
    private final ContextInternalFilterAddendum filterAddendum;

    private EPStatementHandleCallback scheduleHandle;

    public ContextControllerConditionCrontab(StatementContext statementContext, long scheduleSlot, ContextDetailConditionCrontab spec, ContextControllerConditionCallback callback, ContextInternalFilterAddendum filterAddendum) {
        this.statementContext = statementContext;
        this.scheduleSlot = scheduleSlot;
        this.spec = spec;
        this.callback = callback;
        this.filterAddendum = filterAddendum;
    }

    public void activate(EventBean optionalTriggerEvent, MatchedEventMap priorMatches, long timeOffset, boolean isRecoveringResilient) {
        startContextCallback();
    }

    public void deactivate() {
        endContextCallback();
    }

    public boolean isRunning() {
        return scheduleHandle != null;
    }

    private void startContextCallback() {
        ScheduleHandleCallback scheduleCallback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qContextScheduledEval(ContextControllerConditionCrontab.this.statementContext.getContextDescriptor());
                }
                scheduleHandle = null;  // terminates automatically unless scheduled again
                callback.rangeNotification(Collections.<String, Object>emptyMap(), ContextControllerConditionCrontab.this, null, null, null, filterAddendum);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aContextScheduledEval();
                }
            }
        };
        EPStatementAgentInstanceHandle agentHandle = new EPStatementAgentInstanceHandle(statementContext.getEpStatementHandle(), statementContext.getDefaultAgentInstanceLock(), -1, new StatementAgentInstanceFilterVersion(), statementContext.getFilterFaultHandlerFactory());
        scheduleHandle = new EPStatementHandleCallback(agentHandle, scheduleCallback);
        SchedulingService schedulingService = statementContext.getSchedulingService();
        EngineImportService engineImportService = statementContext.getEngineImportService();
        long nextScheduledTime = ScheduleComputeHelper.computeDeltaNextOccurance(spec.getSchedule(), schedulingService.getTime(), engineImportService.getTimeZone(), engineImportService.getTimeAbacus());
        statementContext.getSchedulingService().add(nextScheduledTime, scheduleHandle, scheduleSlot);
    }

    private void endContextCallback() {
        if (scheduleHandle != null) {
            statementContext.getSchedulingService().remove(scheduleHandle, scheduleSlot);
        }
        scheduleHandle = null;
    }

    public Long getExpectedEndTime() {
        EngineImportService engineImportService = statementContext.getEngineImportService();
        return ScheduleComputeHelper.computeNextOccurance(spec.getSchedule(), statementContext.getTimeProvider().getTime(), engineImportService.getTimeZone(), engineImportService.getTimeAbacus());
    }

    public boolean isImmediate() {
        return spec.isImmediate();
    }
}
