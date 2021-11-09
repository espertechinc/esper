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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
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
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimpleOutputLastHelper;
import com.espertech.esper.common.internal.epl.variable.core.Variable;

public interface ResultSetProcessorHelperFactory {
    EPTypeClass EPTYPE = new EPTypeClass(ResultSetProcessorHelperFactory.class);

    ResultSetProcessorRowPerGroupUnboundHelper makeRSRowPerGroupUnboundGroupRep(EPType[] groupKeyTypes, DataInputOutputSerde serde, EventType eventType, StateMgmtSetting stateMgmtSettings, ExprEvaluatorContext exprEvaluatorContext);

    ResultSetProcessorGroupedOutputFirstHelper makeRSGroupedOutputFirst(ExprEvaluatorContext exprEvaluatorContext, EPType[] groupKeyTypes, OutputConditionPolledFactory optionalOutputFirstConditionFactory, AggregationGroupByRollupDesc optionalGroupByRollupDesc, int optionalRollupLevel, DataInputOutputSerde serde, StateMgmtSetting stateMgmtSettings);

    OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(EventType[] eventTypes, ExprEvaluatorContext exprEvaluatorContext, StateMgmtSetting stateMgmtSettings);

    OutputConditionFactory makeOutputConditionTime(boolean hasVariable, TimePeriodCompute timePeriodCompute, boolean isStartConditionOnCreation, int scheduleCallbackId, StateMgmtSetting stateMgmtSetting);

    ResultSetProcessorRowForAllOutputAllHelper makeRSRowForAllOutputAll(ResultSetProcessorRowForAll processor, ExprEvaluatorContext exprEvaluatorContext, StateMgmtSetting stateMgmtSettings);

    OutputConditionExpressionFactory makeOutputConditionExpression();

    OutputConditionFactory makeOutputConditionCrontab(ExprEvaluator[] crontabAtSchedule, boolean isStartConditionOnCreation, int scheduleCallbackId);

    OutputConditionFactory makeOutputConditionCount(int rate, Variable variableMetaData, StateMgmtSetting stateMgmtSetting);

    OutputProcessViewAfterState makeOutputConditionAfter(Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, ExprEvaluatorContext exprEvaluatorContext);

    ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimple simple, ExprEvaluatorContext exprEvaluatorContext, EventType[] eventTypes, StateMgmtSetting stateMgmtSetting);

    ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimple simple, ExprEvaluatorContext exprEvaluatorContext, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorStraightOutputFirstHelper makeRSStraightOutputFirst(ExprEvaluatorContext exprEvaluatorContext, StateMgmtSetting stateMgmtSetting);

    ResultSetProcessorRowPerEventOutputLastHelper makeRSRowPerEventOutputLast(ResultSetProcessorRowPerEvent processor, ExprEvaluatorContext exprEvaluatorContext, StateMgmtSetting stateMgmtSetting);

    ResultSetProcessorRowPerEventOutputAllHelper makeRSRowPerEventOutputAll(ResultSetProcessorRowPerEvent processor, ExprEvaluatorContext exprEvaluatorContext, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorRowForAllOutputLastHelper makeRSRowForAllOutputLast(ResultSetProcessorRowForAll processor, ExprEvaluatorContext exprEvaluatorContext, StateMgmtSetting stateMgmtSetting);

    ResultSetProcessorGroupedOutputAllGroupReps makeRSGroupedOutputAllNoOpt(ExprEvaluatorContext exprEvaluatorContext, EPType[] groupKeyTypes, DataInputOutputSerde serde, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorRowPerGroup processor, EPType[] groupKeyTypes, DataInputOutputSerde serde, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorRowPerGroup processor, EPType[] groupKeyTypes, DataInputOutputSerde serde, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorAggregateGroupedOutputAllHelper makeRSAggregateGroupedOutputAll(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorAggregateGrouped processor, EPType[] groupKeyTypes, DataInputOutputSerde serde, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorAggregateGroupedOutputLastHelper makeRSAggregateGroupedOutputLastOpt(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorAggregateGrouped processor, EPType[] groupKeyTypes, DataInputOutputSerde serde, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorRowPerGroupRollupOutputLastHelper makeRSRowPerGroupRollupLast(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorRowPerGroupRollup processor, EPType[] groupKeyTypes, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorRowPerGroupRollupOutputAllHelper makeRSRowPerGroupRollupAll(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorRowPerGroupRollup processor, EPType[] groupKeyTypes, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);

    ResultSetProcessorRowPerGroupRollupUnboundHelper makeRSRowPerGroupRollupSnapshotUnbound(ExprEvaluatorContext exprEvaluatorContext, ResultSetProcessorRowPerGroupRollup processor, EPType[] groupKeyTypes, int numStreams, EventType[] eventTypes, StateMgmtSetting stateMgmtSettings);
}
