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

import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.List;

public class ContextConditionDescriptorTimePeriod implements ContextConditionDescriptor {
    private TimePeriodCompute timePeriodCompute;
    private boolean immediate;
    private int scheduleCallbackId = -1;

    public TimePeriodCompute getTimePeriodCompute() {
        return timePeriodCompute;
    }

    public void setTimePeriodCompute(TimePeriodCompute timePeriodCompute) {
        this.timePeriodCompute = timePeriodCompute;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public void addFilterSpecActivatable(List<FilterSpecActivatable> activatables) {
        // none here
    }

    public boolean isImmediate() {
        return immediate;
    }

    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    public Long getExpectedEndTime(ContextManagerRealization realization) {
        AgentInstanceContext agentInstanceContext = realization.getAgentInstanceContextCreate();
        long current = agentInstanceContext.getSchedulingService().getTime();
        long msec = timePeriodCompute.deltaAdd(current, null, true, agentInstanceContext);
        return current + msec;
    }
}
