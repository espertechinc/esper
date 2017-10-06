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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.core.resultset.agggrouped.ResultSetProcessorAggregateGrouped;
import com.espertech.esper.epl.core.resultset.agggrouped.ResultSetProcessorAggregateGroupedOutputAllHelper;
import com.espertech.esper.epl.core.resultset.agggrouped.ResultSetProcessorAggregateGroupedOutputLastHelper;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupReps;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.epl.core.resultset.handthru.ResultSetProcessorSimple;
import com.espertech.esper.epl.core.resultset.handthru.ResultSetProcessorSimpleOutputAllHelper;
import com.espertech.esper.epl.core.resultset.handthru.ResultSetProcessorSimpleOutputLastHelper;
import com.espertech.esper.epl.core.resultset.rowforall.ResultSetProcessorRowForAll;
import com.espertech.esper.epl.core.resultset.rowforall.ResultSetProcessorRowForAllOutputAllHelper;
import com.espertech.esper.epl.core.resultset.rowforall.ResultSetProcessorRowForAllOutputLastHelper;
import com.espertech.esper.epl.core.resultset.rowperevent.ResultSetProcessorRowPerEvent;
import com.espertech.esper.epl.core.resultset.rowperevent.ResultSetProcessorRowPerEventOutputAllHelper;
import com.espertech.esper.epl.core.resultset.rowperevent.ResultSetProcessorRowPerEventOutputLastHelper;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroup;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroupOutputAllHelper;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroupOutputLastHelper;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroupUnboundHelper;
import com.espertech.esper.epl.core.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollup;
import com.espertech.esper.epl.core.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupOutputAllHelper;
import com.espertech.esper.epl.core.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupOutputLastHelper;
import com.espertech.esper.epl.core.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupUnboundHelper;
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

    ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext, int numStreams);

    ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext, int numStreams);

    ResultSetProcessorRowPerEventOutputLastHelper makeRSRowPerEventOutputLast(ResultSetProcessorRowPerEvent processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowPerEventOutputAllHelper makeRSRowPerEventOutputAll(ResultSetProcessorRowPerEvent processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowForAllOutputLastHelper makeRSRowForAllOutputLast(ResultSetProcessorRowForAll processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowForAllOutputAllHelper makeRSRowForAllOutputAll(ResultSetProcessorRowForAll processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorGroupedOutputAllGroupReps makeRSGroupedOutputAllNoOpt(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorRowPerGroupUnboundHelper makeRSRowPerGroupUnboundGroupRep(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes);

    ResultSetProcessorAggregateGroupedOutputAllHelper makeRSAggregateGroupedOutputAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped resultSetProcessorAggregateGrouped, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorAggregateGroupedOutputLastHelper makeRSAggregateGroupedOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped resultSetProcessorAggregateGrouped, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorGroupedOutputFirstHelper makeRSGroupedOutputFirst(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, OutputConditionPolledFactory optionalOutputFirstConditionFactory, AggregationGroupByRollupDesc optionalGroupByRollupDesc, int optionalRollupLevel);

    ResultSetProcessorRowPerGroupRollupOutputLastHelper makeRSRowPerGroupRollupLast(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorRowPerGroupRollupOutputAllHelper makeRSRowPerGroupRollupAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, Class[] groupKeyTypes, int numStreams);

    ResultSetProcessorRowPerGroupRollupUnboundHelper makeRSRowPerGroupRollupSnapshotUnbound(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, Class[] groupKeyTypes, int numStreams);
}
