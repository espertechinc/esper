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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionExpressionFactory;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionFactory;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactory;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewAfterState;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewConditionDeltaSet;
import com.espertech.esper.common.internal.epl.resultset.agggrouped.ResultSetProcessorAggregateGrouped;
import com.espertech.esper.common.internal.epl.resultset.agggrouped.ResultSetProcessorAggregateGroupedOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.agggrouped.ResultSetProcessorAggregateGroupedOutputLastHelper;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupReps;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAll;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAllOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAllOutputLastHelper;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEvent;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventOutputLastHelper;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroup;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupOutputLastHelper;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupUnboundHelper;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollup;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupOutputLastHelper;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupUnboundHelper;
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimple;
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimpleOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimpleOutputFirstHelper;
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimpleOutputLastHelper;
import com.espertech.esper.common.internal.epl.variable.core.Variable;

public interface ResultSetProcessorHelperFactory {
    ResultSetProcessorRowPerGroupUnboundHelper makeRSRowPerGroupUnboundGroupRep(Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType eventType, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorGroupedOutputFirstHelper makeRSGroupedOutputFirst(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, OutputConditionPolledFactory optionalOutputFirstConditionFactory, AggregationGroupByRollupDesc optionalGroupByRollupDesc, int optionalRollupLevel, DataInputOutputSerde<Object> serde);

    OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(EventType[] eventTypes, AgentInstanceContext agentInstanceContext);

    OutputConditionFactory makeOutputConditionTime(boolean hasVariable, TimePeriodCompute timePeriodCompute, boolean isStartConditionOnCreation, int scheduleCallbackId);

    ResultSetProcessorRowForAllOutputAllHelper makeRSRowForAllOutputAll(ResultSetProcessorRowForAll processor, AgentInstanceContext agentInstanceContext);

    OutputConditionExpressionFactory makeOutputConditionExpression();

    OutputConditionFactory makeOutputConditionCrontab(ExprEvaluator[] crontabAtSchedule, boolean isStartConditionOnCreation, int scheduleCallbackId);

    OutputConditionFactory makeOutputConditionCount(int rate, Variable variableMetaData);

    OutputProcessViewAfterState makeOutputConditionAfter(Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext, EventType[] eventTypes);

    ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext, EventType[] eventTypes);

    ResultSetProcessorSimpleOutputFirstHelper makeRSSimpleOutputFirst(AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowPerEventOutputLastHelper makeRSRowPerEventOutputLast(ResultSetProcessorRowPerEvent processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowPerEventOutputAllHelper makeRSRowPerEventOutputAll(ResultSetProcessorRowPerEvent processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorRowForAllOutputLastHelper makeRSRowForAllOutputLast(ResultSetProcessorRowForAll processor, AgentInstanceContext agentInstanceContext);

    ResultSetProcessorGroupedOutputAllGroupReps makeRSGroupedOutputAllNoOpt(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes);

    ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup processor, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes);

    ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup processor, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes);

    ResultSetProcessorAggregateGroupedOutputAllHelper makeRSAggregateGroupedOutputAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped processor, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes);

    ResultSetProcessorAggregateGroupedOutputLastHelper makeRSAggregateGroupedOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped processor, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde);

    ResultSetProcessorRowPerGroupRollupOutputLastHelper makeRSRowPerGroupRollupLast(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup processor, Class[] groupKeyTypes, EventType[] eventTypes);

    ResultSetProcessorRowPerGroupRollupOutputAllHelper makeRSRowPerGroupRollupAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup processor, Class[] groupKeyTypes, EventType[] eventTypes);

    ResultSetProcessorRowPerGroupRollupUnboundHelper makeRSRowPerGroupRollupSnapshotUnbound(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup processor, Class[] groupKeyTypes, int numStreams, EventType[] eventTypes);
}
