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
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.util.EPLScheduleExpressionUtil;
import com.espertech.esper.schedule.ScheduleSpec;

import java.util.List;

/**
 * Output condition handling crontab-at schedule output.
 */
public class OutputConditionCrontabFactory implements OutputConditionFactory {
    protected final ExprEvaluator[] scheduleSpecEvaluators;
    protected final boolean isStartConditionOnCreation;

    public OutputConditionCrontabFactory(List<ExprNode> scheduleSpecExpressionList,
                                         StatementContext statementContext,
                                         boolean isStartConditionOnCreation)
            throws ExprValidationException {
        this.scheduleSpecEvaluators = EPLScheduleExpressionUtil.crontabScheduleValidate(ExprNodeOrigin.OUTPUTLIMIT, scheduleSpecExpressionList, statementContext, false);
        this.isStartConditionOnCreation = isStartConditionOnCreation;
    }

    public OutputCondition make(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        ScheduleSpec scheduleSpec = EPLScheduleExpressionUtil.crontabScheduleBuild(scheduleSpecEvaluators, agentInstanceContext);
        return new OutputConditionCrontab(outputCallback, agentInstanceContext, isStartConditionOnCreation, scheduleSpec);
    }
}
