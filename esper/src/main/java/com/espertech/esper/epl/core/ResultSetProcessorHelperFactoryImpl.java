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
package com.espertech.esper.epl.core;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.view.*;

import java.util.List;

public class ResultSetProcessorHelperFactoryImpl implements ResultSetProcessorHelperFactory {
    public ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimpleOutputLastHelperImpl(simple);
    }

    public ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple resultSetProcessorSimple, AgentInstanceContext agentInstanceContext) {
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

    public OutputConditionFactory makeOutputConditionCount(int rate, VariableMetaData variableMetaData, StatementContext statementContext) {
        return new OutputConditionCountFactory(rate, variableMetaData);
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

    public ResultSetProcessorGroupedOutputAllGroupReps makeRSGroupedOutputAllNoOpt(AgentInstanceContext agentInstanceContext, ExprEvaluator[] groupKeyExpressions, int numStreams) {
        return new ResultSetProcessorGroupedOutputAllGroupRepsImpl();
    }

    public ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupOutputAllHelperImpl(resultSetProcessorRowPerGroup);
    }

    public ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupOutputLastHelperImpl(resultSetProcessorRowPerGroup);
    }

    public ResultSetProcessorGroupedOutputFirstHelper makeRSGroupedOutputFirst(AgentInstanceContext agentInstanceContext, ExprEvaluator[] groupKeyNodes, OutputConditionPolledFactory optionalOutputFirstConditionFactory, AggregationGroupByRollupDesc optionalGroupByRollupDesc, int optionalRollupLevel) {
        return new ResultSetProcessorGroupedOutputFirstHelperImpl();
    }

    public ResultSetProcessorRowPerGroupUnboundGroupRep makeRSRowPerGroupUnboundGroupRep(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupFactory prototype) {
        return new ResultSetProcessorRowPerGroupUnboundGroupRepImpl();
    }

    public ResultSetProcessorAggregateGroupedOutputAllHelper makeRSAggregateGroupedOutputAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped processor, ResultSetProcessorAggregateGroupedFactory prototype) {
        return new ResultSetProcessorAggregateGroupedOutputAllHelperImpl(processor);
    }

    public ResultSetProcessorAggregateGroupedOutputLastHelper makeRSAggregateGroupedOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped resultSetProcessorAggregateGrouped, ResultSetProcessorAggregateGroupedFactory prototype) {
        return new ResultSetProcessorAggregateGroupedOutputLastHelperImpl(resultSetProcessorAggregateGrouped);
    }

    public ResultSetProcessorRowPerGroupRollupOutputLastHelper makeRSRowPerGroupRollupLast(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, ResultSetProcessorRowPerGroupRollupFactory prototype) {
        return new ResultSetProcessorRowPerGroupRollupOutputLastHelperImpl(resultSetProcessorRowPerGroupRollup, prototype.getGroupByRollupDesc().getLevels().length);
    }

    public ResultSetProcessorRowPerGroupRollupOutputAllHelper makeRSRowPerGroupRollupAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, ResultSetProcessorRowPerGroupRollupFactory prototype) {
        return new ResultSetProcessorRowPerGroupRollupOutputAllHelperImpl(resultSetProcessorRowPerGroupRollup, prototype.getGroupByRollupDesc().getLevels().length);
    }

    public ResultSetProcessorRowPerGroupRollupUnboundHelper makeRSRowPerGroupRollupSnapshotUnbound(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollupFactory prototype) {
        int levelCount = prototype.getGroupByRollupDesc().getLevels().length;
        return new ResultSetProcessorRowPerGroupRollupUnboundHelperImpl(levelCount);
    }
}
