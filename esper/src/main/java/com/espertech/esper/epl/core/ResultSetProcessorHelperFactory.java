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
import com.espertech.esper.epl.view.OutputConditionFactory;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;
import com.espertech.esper.epl.view.OutputProcessViewAfterState;
import com.espertech.esper.epl.view.OutputProcessViewConditionDeltaSet;

import java.util.List;

public interface ResultSetProcessorHelperFactory {
    OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(boolean isJoin, AgentInstanceContext agentInstanceContext);

    OutputConditionFactory makeOutputConditionTime(ExprTimePeriod timePeriodExpr, boolean isStartConditionOnCreation);

    OutputConditionFactory makeOutputConditionExpression(ExprNode whenExpressionNode, List<OnTriggerSetAssignment> thenExpressions, StatementContext statementContext, ExprNode andAfterTerminateExpr, List<OnTriggerSetAssignment> andAfterTerminateThenExpressions, boolean isStartConditionOnCreation) throws ExprValidationException;

    OutputConditionFactory makeOutputConditionCrontab(List<ExprNode> crontabAtSchedule, StatementContext statementContext, boolean isStartConditionOnCreation) throws ExprValidationException;

    OutputConditionFactory makeOutputConditionCount(int rate, VariableMetaData variableMetaData, StatementContext statementContext);

    OutputProcessViewAfterState makeOutputConditionAfter(Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple resultSetProcessorSimple, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorAggregateAllOutputLastHelper makeRSAggregateAllOutputLast(ResultSetProcessorAggregateAll processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorAggregateAllOutputAllHelper makeRSAggregateAllOutputAll(ResultSetProcessorAggregateAll processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowForAllOutputLastHelper makeRSRowForAllOutputLast(ResultSetProcessorRowForAll processor, ResultSetProcessorRowForAllFactory prototype, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowForAllOutputAllHelper makeRSRowForAllOutputAll(ResultSetProcessorRowForAll processor, ResultSetProcessorRowForAllFactory prototype, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorGroupedOutputAllGroupReps makeRSGroupedOutputAllNoOpt(AgentInstanceContext agentInstanceContext, ExprEvaluator[] groupKeyExpressions, int numStreams);

    ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, ResultSetProcessorRowPerGroupFactory prototype);

    ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, ResultSetProcessorRowPerGroupFactory prototype);

    ResultSetProcessorRowPerGroupUnboundGroupRep makeRSRowPerGroupUnboundGroupRep(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupFactory prototype);

    ResultSetProcessorAggregateGroupedOutputAllHelper makeRSAggregateGroupedOutputAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped resultSetProcessorAggregateGrouped, ResultSetProcessorAggregateGroupedFactory prototype);

    ResultSetProcessorAggregateGroupedOutputLastHelper makeRSAggregateGroupedOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped resultSetProcessorAggregateGrouped, ResultSetProcessorAggregateGroupedFactory prototype);

    ResultSetProcessorGroupedOutputFirstHelper makeRSGroupedOutputFirst(AgentInstanceContext agentInstanceContext, ExprEvaluator[] groupKeyNodes, OutputConditionPolledFactory optionalOutputFirstConditionFactory, AggregationGroupByRollupDesc optionalGroupByRollupDesc, int optionalRollupLevel);

    ResultSetProcessorRowPerGroupRollupOutputLastHelper makeRSRowPerGroupRollupLast(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, ResultSetProcessorRowPerGroupRollupFactory prototype);

    ResultSetProcessorRowPerGroupRollupOutputAllHelper makeRSRowPerGroupRollupAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, ResultSetProcessorRowPerGroupRollupFactory prototype);

    ResultSetProcessorRowPerGroupRollupUnboundHelper makeRSRowPerGroupRollupSnapshotUnbound(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollupFactory prototype);
}
