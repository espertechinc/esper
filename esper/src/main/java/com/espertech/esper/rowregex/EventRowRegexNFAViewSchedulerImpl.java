/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.rowregex;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.ExtensionServicesContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.ScheduleSlot;

public class EventRowRegexNFAViewSchedulerImpl implements EventRowRegexNFAViewScheduler
{
    private AgentInstanceContext agentInstanceContext;
    private ScheduleSlot scheduleSlot;
    private EPStatementHandleCallback handle;

    public void setScheduleCallback(AgentInstanceContext agentInstanceContext, final EventRowRegexNFAViewScheduleCallback scheduleCallback) {
        this.agentInstanceContext = agentInstanceContext;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        final ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(ExtensionServicesContext extensionServicesContext)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegExScheduledEval();}
                scheduleCallback.triggered();
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExScheduledEval();}
            }
        };
        this.handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
    }

    public void addSchedule(long msecAfterCurrentTime) {
        agentInstanceContext.getStatementContext().getSchedulingService().add(msecAfterCurrentTime, handle, scheduleSlot);
    }

    public void changeSchedule(long msecAfterCurrentTime) {
        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        agentInstanceContext.getStatementContext().getSchedulingService().add(msecAfterCurrentTime, handle, scheduleSlot);
    }

    public void removeSchedule() {
        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
    }
}
