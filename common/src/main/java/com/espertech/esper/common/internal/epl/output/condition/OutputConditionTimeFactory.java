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
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;

/**
 * Output condition that is satisfied at the end
 * of every time interval of a given length.
 */
public class OutputConditionTimeFactory implements OutputConditionFactory {
    private final boolean hasVariable;
    private final TimePeriodCompute timePeriodCompute;
    private final boolean isStartConditionOnCreation;
    private final int scheduleCallbackId;

    public OutputConditionTimeFactory(boolean hasVariable, TimePeriodCompute timePeriodCompute, boolean isStartConditionOnCreation, int scheduleCallbackId) {
        this.hasVariable = hasVariable;
        this.timePeriodCompute = timePeriodCompute;
        this.isStartConditionOnCreation = isStartConditionOnCreation;
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public OutputCondition instantiateOutputCondition(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        return new OutputConditionTime(outputCallback, agentInstanceContext, this, isStartConditionOnCreation);
    }

    public boolean isHasVariable() {
        return hasVariable;
    }

    public TimePeriodCompute getTimePeriodCompute() {
        return timePeriodCompute;
    }

    public boolean isStartConditionOnCreation() {
        return isStartConditionOnCreation;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }
}
