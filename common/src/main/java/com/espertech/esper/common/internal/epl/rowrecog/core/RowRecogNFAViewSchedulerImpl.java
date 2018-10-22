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
package com.espertech.esper.common.internal.epl.rowrecog.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;

public class RowRecogNFAViewSchedulerImpl implements RowRecogNFAViewScheduler {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "interval";

    private AgentInstanceContext agentInstanceContext;
    private long scheduleSlot;
    private EPStatementHandleCallbackSchedule handle;

    public void setScheduleCallback(AgentInstanceContext agentInstanceContext, final RowRecogNFAViewScheduleCallback scheduleCallback) {
        this.agentInstanceContext = agentInstanceContext;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        final ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.matchrecognize, NAME_AUDITPROVIDER_SCHEDULE);
                agentInstanceContext.getInstrumentationProvider().qRegExScheduledEval();
                scheduleCallback.triggered();
                agentInstanceContext.getInstrumentationProvider().aRegExScheduledEval();
            }
        };
        this.handle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
    }

    public void addSchedule(long timeDelta) {
        agentInstanceContext.getAuditProvider().scheduleAdd(timeDelta, agentInstanceContext, handle, ScheduleObjectType.matchrecognize, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getStatementContext().getSchedulingService().add(timeDelta, handle, scheduleSlot);
    }

    public void changeSchedule(long timeDelta) {
        agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.matchrecognize, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        agentInstanceContext.getAuditProvider().scheduleAdd(timeDelta, agentInstanceContext, handle, ScheduleObjectType.matchrecognize, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getStatementContext().getSchedulingService().add(timeDelta, handle, scheduleSlot);
    }

    public void removeSchedule() {
        agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.matchrecognize, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
    }
}
