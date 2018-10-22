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
import com.espertech.esper.common.internal.schedule.*;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output condition handling crontab-at schedule output.
 */
public final class OutputConditionCrontab extends OutputConditionBase implements OutputCondition {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "crontab";

    private static final boolean DO_OUTPUT = true;
    private static final boolean FORCE_UPDATE = true;

    private final AgentInstanceContext context;
    private final ScheduleSpec scheduleSpec;

    private final long scheduleSlot;
    private Long currentReferencePoint;
    private boolean isCallbackScheduled;

    public OutputConditionCrontab(OutputCallback outputCallback, AgentInstanceContext context, boolean isStartConditionOnCreation, ScheduleSpec scheduleSpec) {
        super(outputCallback);
        this.context = context;
        this.scheduleSpec = scheduleSpec;
        scheduleSlot = context.getStatementContext().getScheduleBucket().allocateSlot();
        if (isStartConditionOnCreation) {
            updateOutputCondition(0, 0);
        }
    }

    public final void updateOutputCondition(int newEventsCount, int oldEventsCount) {
        if (currentReferencePoint == null) {
            currentReferencePoint = context.getStatementContext().getSchedulingService().getTime();
        }

        // Schedule the next callback if there is none currently scheduled
        if (!isCallbackScheduled) {
            scheduleCallback();
        }
    }

    public final String toString() {
        return this.getClass().getName() +
                " spec=" + scheduleSpec;
    }

    private void scheduleCallback() {
        isCallbackScheduled = true;

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                context.getInstrumentationProvider().qOutputRateConditionScheduledEval();
                context.getAuditProvider().scheduleFire(context, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
                OutputConditionCrontab.this.isCallbackScheduled = false;
                OutputConditionCrontab.this.outputCallback.continueOutputProcessing(DO_OUTPUT, FORCE_UPDATE);
                scheduleCallback();
                context.getInstrumentationProvider().aOutputRateConditionScheduledEval();
            }
        };
        EPStatementHandleCallbackSchedule handle = new EPStatementHandleCallbackSchedule(context.getEpStatementAgentInstanceHandle(), callback);
        SchedulingService schedulingService = context.getStatementContext().getSchedulingService();
        ClasspathImportServiceRuntime classpathImportService = context.getStatementContext().getClasspathImportServiceRuntime();
        long nextScheduledTime = ScheduleComputeHelper.computeDeltaNextOccurance(scheduleSpec, schedulingService.getTime(), classpathImportService.getTimeZone(), classpathImportService.getTimeAbacus());
        context.getAuditProvider().scheduleAdd(nextScheduledTime, context, handle, ScheduleObjectType.outputratelimiting, NAME_AUDITPROVIDER_SCHEDULE);
        schedulingService.add(nextScheduledTime, handle, scheduleSlot);
    }

    public void terminated() {
        outputCallback.continueOutputProcessing(true, true);
    }

    public void stopOutputCondition() {
        // no action required
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionCrontab.class);
}
