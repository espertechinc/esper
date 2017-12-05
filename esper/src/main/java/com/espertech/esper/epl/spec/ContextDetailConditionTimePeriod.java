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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.schedule.TimeProvider;

public class ContextDetailConditionTimePeriod implements ContextDetailCondition {
    private static final long serialVersionUID = 5140498109356559324L;
    private ExprTimePeriod timePeriod;
    private boolean immediate;
    private int scheduleCallbackId = -1;

    public ContextDetailConditionTimePeriod(ExprTimePeriod timePeriod, boolean immediate) {
        this.timePeriod = timePeriod;
        this.immediate = immediate;
    }

    public ExprTimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(ExprTimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public Long getExpectedEndTime(ExprEvaluatorContext exprEvaluatorContext, TimeProvider timeProvider) {
        long current = timeProvider.getTime();
        long msec = timePeriod.nonconstEvaluator().deltaAdd(current, null, true, exprEvaluatorContext);
        return current + msec;
    }
}
