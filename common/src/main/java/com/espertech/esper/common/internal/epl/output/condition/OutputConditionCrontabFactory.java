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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.schedule.ScheduleExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;

/**
 * Output condition handling crontab-at schedule output.
 */
public class OutputConditionCrontabFactory implements OutputConditionFactory {
    protected final ExprEvaluator[] scheduleSpecEvaluators;
    protected final boolean isStartConditionOnCreation;
    private final int scheduleCallbackId;

    public OutputConditionCrontabFactory(ExprEvaluator[] scheduleSpecExpressionList,
                                         boolean isStartConditionOnCreation,
                                         int scheduleCallbackId) {
        this.scheduleSpecEvaluators = scheduleSpecExpressionList;
        this.isStartConditionOnCreation = isStartConditionOnCreation;
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public OutputCondition instantiateOutputCondition(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        ScheduleSpec scheduleSpec = ScheduleExpressionUtil.crontabScheduleBuild(scheduleSpecEvaluators, agentInstanceContext);
        return new OutputConditionCrontab(outputCallback, agentInstanceContext, isStartConditionOnCreation, scheduleSpec);
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }
}
