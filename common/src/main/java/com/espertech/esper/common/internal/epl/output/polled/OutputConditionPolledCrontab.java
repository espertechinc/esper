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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.schedule.ScheduleComputeHelper;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output condition handling crontab-at schedule output.
 */
public final class OutputConditionPolledCrontab implements OutputConditionPolled {
    private final AgentInstanceContext agentInstanceContext;
    private final OutputConditionPolledCrontabState state;

    public OutputConditionPolledCrontab(AgentInstanceContext agentInstanceContext, OutputConditionPolledCrontabState state) {
        this.agentInstanceContext = agentInstanceContext;
        this.state = state;
    }

    public OutputConditionPolledState getState() {
        return state;
    }

    public final boolean updateOutputCondition(int newEventsCount, int oldEventsCount) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".updateOutputCondition, " +
                    "  newEventsCount==" + newEventsCount +
                    "  oldEventsCount==" + oldEventsCount);
        }

        boolean output = false;
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        ClasspathImportServiceRuntime classpathImportService = agentInstanceContext.getClasspathImportServiceRuntime();
        if (state.getCurrentReferencePoint() == null) {
            state.setCurrentReferencePoint(currentTime);
            state.setNextScheduledTime(ScheduleComputeHelper.computeNextOccurance(state.getScheduleSpec(), currentTime, classpathImportService.getTimeZone(), classpathImportService.getTimeAbacus()));
            output = true;
        }

        if (state.getNextScheduledTime() <= currentTime) {
            state.setNextScheduledTime(ScheduleComputeHelper.computeNextOccurance(state.getScheduleSpec(), currentTime, classpathImportService.getTimeZone(), classpathImportService.getTimeAbacus()));
            output = true;
        }

        return output;
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionPolledCrontab.class);
}
