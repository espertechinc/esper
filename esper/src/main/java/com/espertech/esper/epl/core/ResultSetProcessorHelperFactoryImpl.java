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

package com.espertech.esper.epl.core;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.view.*;

import java.util.List;

public class ResultSetProcessorHelperFactoryImpl implements ResultSetProcessorHelperFactory {
    public ResultSetProcessorSimpleOutputLastHelper makeSimpleAndLast(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimpleOutputLastHelperImpl(simple);
    }

    public OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(boolean isJoin, AgentInstanceContext agentInstanceContext) {
        return new OutputProcessViewConditionDeltaSetImpl(isJoin);
    }

    public OutputConditionFactory makeOutputConditionTime(ExprTimePeriod timePeriodExpr, boolean isStartConditionOnCreation) {
        return new OutputConditionTimeFactory(timePeriodExpr, isStartConditionOnCreation);
    }

    public OutputConditionFactory makeOutputConditionExpression(ExprNode whenExpressionNode, List<OnTriggerSetAssignment> thenExpressions, StatementContext statementContext, ExprNode andAfterTerminateExpr, List<OnTriggerSetAssignment> andAfterTerminateThenExpressions, boolean isStartConditionOnCreation) throws ExprValidationException {
        return new OutputConditionExpressionFactory(whenExpressionNode, thenExpressions, statementContext, andAfterTerminateExpr, andAfterTerminateThenExpressions, isStartConditionOnCreation);
    }

    public OutputConditionFactory makeOutputConditionCrontab(List<ExprNode> crontabAtSchedule, StatementContext statementContext, boolean isStartConditionOnCreation) throws ExprValidationException {
        return new OutputConditionCrontabFactory(crontabAtSchedule, statementContext, isStartConditionOnCreation);
    }

    public OutputProcessViewAfterState makeOutputConditionAfter(Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, AgentInstanceContext agentInstanceContext) {
        if (afterConditionSatisfied) {
            return OutputProcessViewAfterStateNone.INSTANCE;
        }
        return new OutputProcessViewAfterStateImpl(afterConditionTime, afterConditionNumberOfEvents);
    }

    public ResultSetProcessorAggregateAllOutputLastHelperImpl getAggregateAllOutputLastHelper(ResultSetProcessorAggregateAll processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorAggregateAllOutputLastHelperImpl(processor);
    }

    public ResultSetProcessorAggregateAllOutputAllHelper getAggregateAllOutputAllHelper(ResultSetProcessorAggregateAll processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorAggregateAllOutputAllHelperImpl(processor);
    }
}
