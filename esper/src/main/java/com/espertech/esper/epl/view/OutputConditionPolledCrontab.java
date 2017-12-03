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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.schedule.ScheduleComputeHelper;
import com.espertech.esper.util.ExecutionPathDebugLog;
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
        EngineImportService engineImportService = agentInstanceContext.getEngineImportService();
        if (state.getCurrentReferencePoint() == null) {
            state.setCurrentReferencePoint(currentTime);
            state.setNextScheduledTime(ScheduleComputeHelper.computeNextOccurance(state.getScheduleSpec(), currentTime, engineImportService.getTimeZone(), engineImportService.getTimeAbacus()));
            output = true;
        }

        if (state.getNextScheduledTime() <= currentTime) {
            state.setNextScheduledTime(ScheduleComputeHelper.computeNextOccurance(state.getScheduleSpec(), currentTime, engineImportService.getTimeZone(), engineImportService.getTimeAbacus()));
            output = true;
        }

        return output;
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionPolledCrontab.class);
}
