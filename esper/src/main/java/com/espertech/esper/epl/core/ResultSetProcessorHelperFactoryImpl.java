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
    public ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimpleOutputLastHelperImpl(simple);
    }

    public ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple resultSetProcessorSimple, AgentInstanceContext agentInstanceContext, int numStreams) {
        return new ResultSetProcessorSimpleOutputAllHelperImpl(resultSetProcessorSimple);
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

    public ResultSetProcessorAggregateAllOutputLastHelperImpl makeRSAggregateAllOutputLast(ResultSetProcessorAggregateAll processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorAggregateAllOutputLastHelperImpl(processor);
    }

    public ResultSetProcessorAggregateAllOutputAllHelper makeRSAggregateAllOutputAll(ResultSetProcessorAggregateAll processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorAggregateAllOutputAllHelperImpl(processor);
    }

    public ResultSetProcessorRowForAllOutputLastHelper makeRSRowForAllOutputLast(ResultSetProcessorRowForAll processor, ResultSetProcessorRowForAllFactory prototype, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowForAllOutputLastHelperImpl(processor);
    }

    public ResultSetProcessorRowForAllOutputAllHelper makeRSRowForAllOutputAll(ResultSetProcessorRowForAll processor, ResultSetProcessorRowForAllFactory prototype, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowForAllOutputAllHelperImpl(processor);
    }

    public ResultSetProcessorRowPerGroupOutputAllGroupReps makeRSRowPerGroupOutputAllNoOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupOutputAllGroupRepsImpl();
    }

    public ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupOutputAllHelperImpl(resultSetProcessorRowPerGroup);
    }

    public ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupOutputLastHelperImpl(resultSetProcessorRowPerGroup);
    }

    public ResultSetProcessorRowPerGroupOutputFirstHelper makeRSRowPerGroupOutputFirst(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupOutputFirstHelperImpl();
    }
}
