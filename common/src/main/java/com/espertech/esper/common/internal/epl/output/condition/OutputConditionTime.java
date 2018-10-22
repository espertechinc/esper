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

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodDeltaResult;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output condition that is satisfied at the end
 * of every time interval of a given length.
 */
public final class OutputConditionTime extends OutputConditionBase implements OutputCondition {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "time";
    private static final boolean DO_OUTPUT = true;
    private static final boolean FORCE_UPDATE = true;

    private final AgentInstanceContext context;
    private final OutputConditionTimeFactory parent;

    private final long scheduleSlot;
    private Long currentReferencePoint;
    private boolean isCallbackScheduled;
    private EPStatementHandleCallbackSchedule handle;
    private long currentScheduledTime;

    public OutputConditionTime(OutputCallback outputCallback, AgentInstanceContext context, OutputConditionTimeFactory outputConditionTimeFactory, boolean isStartConditionOnCreation) {
        super(outputCallback);
        this.context = context;
        this.parent = outputConditionTimeFactory;

        this.scheduleSlot = context.getStatementContext().getScheduleBucket().allocateSlot();
        if (isStartConditionOnCreation) {
            updateOutputCondition(0, 0);
        }
    }

    public final void updateOutputCondition(int newEventsCount, int oldEventsCount) {
        if (currentReferencePoint == null) {
            currentReferencePoint = context.getStatementContext().getSchedulingService().getTime();
        }

        // If we pull the interval from a variable, then we may need to reschedule
        if (parent.isHasVariable()) {
            long now = context.getStatementContext().getSchedulingService().getTime();
            TimePeriodDeltaResult delta = parent.getTimePeriodCompute().deltaAddWReference(now, currentReferencePoint, null, true, context);
            if (delta.getDelta() != currentScheduledTime) {
                if (isCallbackScheduled) {
                    // reschedule
                    context.getAuditProvider().scheduleRemove(context, handle, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
                    context.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                    scheduleCallback();
                }
            }
        }

        // Schedule the next callback if there is none currently scheduled
        if (!isCallbackScheduled) {
            scheduleCallback();
        }
    }

    public final String toString() {
        return this.getClass().getName();
    }

    private void scheduleCallback() {
        isCallbackScheduled = true;
        long current = context.getStatementContext().getSchedulingService().getTime();
        TimePeriodDeltaResult delta = parent.getTimePeriodCompute().deltaAddWReference(current, currentReferencePoint, null, true, context);
        long deltaTime = delta.getDelta();
        currentReferencePoint = delta.getLastReference();
        currentScheduledTime = deltaTime;

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".scheduleCallback Scheduled new callback for " +
                    " afterMsec=" + deltaTime +
                    " now=" + current +
                    " currentReferencePoint=" + currentReferencePoint);
        }

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                context.getInstrumentationProvider().qOutputRateConditionScheduledEval();
                context.getAuditProvider().scheduleFire(context, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
                OutputConditionTime.this.isCallbackScheduled = false;
                OutputConditionTime.this.outputCallback.continueOutputProcessing(DO_OUTPUT, FORCE_UPDATE);
                scheduleCallback();
                context.getInstrumentationProvider().aOutputRateConditionScheduledEval();
            }
        };
        handle = new EPStatementHandleCallbackSchedule(context.getEpStatementAgentInstanceHandle(), callback);
        context.getAuditProvider().scheduleAdd(deltaTime, context, handle, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
        context.getStatementContext().getSchedulingService().add(deltaTime, handle, scheduleSlot);
    }

    public void stopOutputCondition() {
        if (handle != null) {
            context.getAuditProvider().scheduleRemove(context, handle, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
            context.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionTime.class);
}
