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
import com.espertech.esper.common.internal.epl.output.condition.*;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactory;
import com.espertech.esper.common.internal.epl.output.view.*;
import com.espertech.esper.common.internal.epl.resultset.agggrouped.*;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupReps;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupRepsImpl;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelperImpl;
import com.espertech.esper.common.internal.epl.resultset.rowforall.*;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEvent;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventOutputAllHelper;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventOutputAllHelperImpl;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventOutputLastHelperImpl;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.*;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.*;
import com.espertech.esper.common.internal.epl.resultset.simple.*;
import com.espertech.esper.common.internal.epl.variable.core.Variable;

public class ResultSetProcessorHelperFactoryDefault implements ResultSetProcessorHelperFactory {
    public final static ResultSetProcessorHelperFactoryDefault INSTANCE = new ResultSetProcessorHelperFactoryDefault();

    private ResultSetProcessorHelperFactoryDefault() {
    }

    public ResultSetProcessorRowPerGroupUnboundHelper makeRSRowPerGroupUnboundGroupRep(Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType eventType, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowPerGroupUnboundHelperImpl();
    }

    public ResultSetProcessorGroupedOutputFirstHelper makeRSGroupedOutputFirst(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, OutputConditionPolledFactory optionalOutputFirstConditionFactory, AggregationGroupByRollupDesc optionalGroupByRollupDesc, int optionalRollupLevel, DataInputOutputSerde<Object> serde) {
        return new ResultSetProcessorGroupedOutputFirstHelperImpl();
    }

    public OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(EventType[] eventTypes, AgentInstanceContext agentInstanceContext) {
        return new OutputProcessViewConditionDeltaSetImpl(eventTypes.length > 1);
    }

    public OutputConditionFactory makeOutputConditionTime(boolean hasVariable, TimePeriodCompute timePeriodCompute, boolean isStartConditionOnCreation, int scheduleCallbackId) {
        return new OutputConditionTimeFactory(hasVariable, timePeriodCompute, isStartConditionOnCreation, scheduleCallbackId);
    }

    public ResultSetProcessorRowForAllOutputLastHelper makeRSRowForAllOutputLast(ResultSetProcessorRowForAll processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowForAllOutputLastHelperImpl(processor);
    }

    public ResultSetProcessorRowForAllOutputAllHelper makeRSRowForAllOutputAll(ResultSetProcessorRowForAll processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowForAllOutputAllHelperImpl(processor);
    }

    public ResultSetProcessorSimpleOutputLastHelper makeRSSimpleOutputLast(ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext, EventType[] eventTypes) {
        return new ResultSetProcessorSimpleOutputLastHelperImpl(simple);
    }

    public ResultSetProcessorSimpleOutputAllHelper makeRSSimpleOutputAll(ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext, EventType[] eventTypes) {
        return new ResultSetProcessorSimpleOutputAllHelperImpl(simple);
    }

    public ResultSetProcessorSimpleOutputFirstHelper makeRSSimpleOutputFirst(AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimpleOutputFirstHelperImpl();
    }

    public OutputConditionExpressionFactory makeOutputConditionExpression() {
        return new OutputConditionExpressionFactory();
    }

    public OutputConditionFactory makeOutputConditionCrontab(ExprEvaluator[] crontabAtSchedule, boolean isStartConditionOnCreation, int scheduleCallbackId) {
        return new OutputConditionCrontabFactory(crontabAtSchedule, isStartConditionOnCreation, scheduleCallbackId);
    }

    public OutputConditionFactory makeOutputConditionCount(int rate, Variable variableMetaData) {
        return new OutputConditionCountFactory(rate, variableMetaData);
    }

    public OutputProcessViewAfterState makeOutputConditionAfter(Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, AgentInstanceContext agentInstanceContext) {
        if (afterConditionSatisfied) {
            return OutputProcessViewAfterStateNone.INSTANCE;
        }
        return new OutputProcessViewAfterStateImpl(afterConditionTime, afterConditionNumberOfEvents);
    }

    public ResultSetProcessorRowPerEventOutputLastHelperImpl makeRSRowPerEventOutputLast(ResultSetProcessorRowPerEvent processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowPerEventOutputLastHelperImpl(processor);
    }

    public ResultSetProcessorRowPerEventOutputAllHelper makeRSRowPerEventOutputAll(ResultSetProcessorRowPerEvent processor, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorRowPerEventOutputAllHelperImpl(processor);
    }

    public ResultSetProcessorGroupedOutputAllGroupReps makeRSGroupedOutputAllNoOpt(AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes) {
        return new ResultSetProcessorGroupedOutputAllGroupRepsImpl();
    }

    public ResultSetProcessorRowPerGroupOutputAllHelper makeRSRowPerGroupOutputAllOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes) {
        return new ResultSetProcessorRowPerGroupOutputAllHelperImpl(resultSetProcessorRowPerGroup);
    }

    public ResultSetProcessorRowPerGroupOutputLastHelper makeRSRowPerGroupOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroup resultSetProcessorRowPerGroup, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes) {
        return new ResultSetProcessorRowPerGroupOutputLastHelperImpl(resultSetProcessorRowPerGroup);
    }

    public ResultSetProcessorAggregateGroupedOutputAllHelper makeRSAggregateGroupedOutputAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped processor, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde, EventType[] eventTypes) {
        return new ResultSetProcessorAggregateGroupedOutputAllHelperImpl(processor);
    }

    public ResultSetProcessorAggregateGroupedOutputLastHelper makeRSAggregateGroupedOutputLastOpt(AgentInstanceContext agentInstanceContext, ResultSetProcessorAggregateGrouped processor, Class[] groupKeyTypes, DataInputOutputSerde<Object> serde) {
        return new ResultSetProcessorAggregateGroupedOutputLastHelperImpl(processor);
    }

    public ResultSetProcessorRowPerGroupRollupOutputLastHelper makeRSRowPerGroupRollupLast(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, Class[] groupKeyTypes, EventType[] eventTypes) {
        return new ResultSetProcessorRowPerGroupRollupOutputLastHelperImpl(resultSetProcessorRowPerGroupRollup, resultSetProcessorRowPerGroupRollup.getGroupByRollupDesc().getLevels().length);
    }

    public ResultSetProcessorRowPerGroupRollupOutputAllHelper makeRSRowPerGroupRollupAll(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, Class[] groupKeyTypes, EventType[] eventTypes) {
        return new ResultSetProcessorRowPerGroupRollupOutputAllHelperImpl(resultSetProcessorRowPerGroupRollup, resultSetProcessorRowPerGroupRollup.getGroupByRollupDesc().getLevels().length);
    }

    public ResultSetProcessorRowPerGroupRollupUnboundHelper makeRSRowPerGroupRollupSnapshotUnbound(AgentInstanceContext agentInstanceContext, ResultSetProcessorRowPerGroupRollup resultSetProcessorRowPerGroupRollup, Class[] groupKeyTypes, int numStreams, EventType[] eventTypes) {
        int levelCount = resultSetProcessorRowPerGroupRollup.getGroupByRollupDesc().getLevels().length;
        return new ResultSetProcessorRowPerGroupRollupUnboundHelperImpl(levelCount);
    }
}
