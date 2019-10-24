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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.controller.initterm.ContextControllerInitTermUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;

import java.util.Map;

public class ContextControllerConditionCrontabImpl implements ContextControllerConditionNonHA, ContextControllerConditionCrontab {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "context-condition crontab";

    private final IntSeqKey conditionPath;
    private final long scheduleSlot;
    private final ScheduleSpec[] scheduleSpecs;
    private final ContextConditionDescriptorCrontab crontab;
    private final ContextControllerConditionCallback callback;
    private final ContextController controller;

    private EPStatementHandleCallbackSchedule scheduleHandle;

    public ContextControllerConditionCrontabImpl(IntSeqKey conditionPath, long scheduleSlot, ScheduleSpec[] scheduleSpecs, ContextConditionDescriptorCrontab crontab, ContextControllerConditionCallback callback, ContextController controller) {
        this.conditionPath = conditionPath;
        this.scheduleSlot = scheduleSlot;
        this.scheduleSpecs = scheduleSpecs;
        this.crontab = crontab;
        this.callback = callback;
        this.controller = controller;
    }

    public boolean activate(EventBean optionalTriggeringEvent, ContextControllerEndConditionMatchEventProvider endConditionMatchEventProvider, Map<String, Object> optionalTriggeringPattern) {
        ScheduleHandleCallback scheduleCallback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();
                agentInstanceContext.getInstrumentationProvider().qContextScheduledEval(agentInstanceContext.getStatementContext().getContextRuntimeDescriptor());

                scheduleHandle = null;  // terminates automatically unless scheduled again
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.context, NAME_AUDITPROVIDER_SCHEDULE);
                callback.rangeNotification(conditionPath, ContextControllerConditionCrontabImpl.this, null, null, null, null);

                agentInstanceContext.getInstrumentationProvider().aContextScheduledEval();
            }
        };
        AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();
        scheduleHandle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), scheduleCallback);
        long nextScheduledTime = ContextControllerInitTermUtil.computeScheduleMinimumDelta(scheduleSpecs, agentInstanceContext.getTimeProvider().getTime(), agentInstanceContext.getClasspathImportServiceRuntime());
        agentInstanceContext.getAuditProvider().scheduleAdd(nextScheduledTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.context, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getSchedulingService().add(nextScheduledTime, scheduleHandle, scheduleSlot);
        return false;
    }

    public void deactivate() {
        if (scheduleHandle != null) {
            AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.context, NAME_AUDITPROVIDER_SCHEDULE);
            agentInstanceContext.getSchedulingService().remove(scheduleHandle, scheduleSlot);
        }
        scheduleHandle = null;
    }

    public boolean isImmediate() {
        return crontab.isImmediate();
    }

    public boolean isRunning() {
        return scheduleHandle != null;
    }

    public Long getExpectedEndTime() {
        return crontab.getExpectedEndTime(controller.getRealization(), scheduleSpecs);
    }

    public ContextConditionDescriptor getDescriptor() {
        return crontab;
    }

    public ScheduleSpec[] getSchedules() {
        return scheduleSpecs;
    }
}
