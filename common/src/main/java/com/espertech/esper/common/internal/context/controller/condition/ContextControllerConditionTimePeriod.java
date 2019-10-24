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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;

import java.util.Map;

public class ContextControllerConditionTimePeriod implements ContextControllerConditionNonHA {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "context-condition time-period";

    private final long scheduleSlot;
    private final ContextConditionDescriptorTimePeriod timePeriod;
    private final IntSeqKey conditionPath;
    private final ContextControllerConditionCallback callback;
    private final ContextController controller;

    private EPStatementHandleCallbackSchedule scheduleHandle;

    public ContextControllerConditionTimePeriod(long scheduleSlot, ContextConditionDescriptorTimePeriod timePeriod, IntSeqKey conditionPath, ContextControllerConditionCallback callback, ContextController controller) {
        this.scheduleSlot = scheduleSlot;
        this.timePeriod = timePeriod;
        this.conditionPath = conditionPath;
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
                callback.rangeNotification(conditionPath, ContextControllerConditionTimePeriod.this, null, null, null, null);

                agentInstanceContext.getInstrumentationProvider().aContextScheduledEval();
            }
        };
        AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();
        scheduleHandle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), scheduleCallback);
        long timeDelta = timePeriod.getTimePeriodCompute().deltaUseRuntimeTime(null, agentInstanceContext, agentInstanceContext.getTimeProvider());
        agentInstanceContext.getAuditProvider().scheduleAdd(timeDelta, agentInstanceContext, scheduleHandle, ScheduleObjectType.context, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getSchedulingService().add(timeDelta, scheduleHandle, scheduleSlot);
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
        return timePeriod.isImmediate();
    }

    public boolean isRunning() {
        return scheduleHandle != null;
    }

    public Long getExpectedEndTime() {
        return timePeriod.getExpectedEndTime(controller.getRealization());
    }

    public ContextConditionDescriptor getDescriptor() {
        return timePeriod;
    }
}
