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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignmentsImpl;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactory;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;

import java.lang.annotation.Annotation;
import java.util.*;

public class FAFQueryMethodSelectExecUtil {

    static Collection<EventBean> snapshot(ExprEvaluator filter, FireAndForgetInstance processorInstance, QueryGraph queryGraph, Annotation[] annotations) {
        Collection<EventBean> coll = processorInstance.snapshotBestEffort(queryGraph, annotations);
        if (filter != null) {
            coll = filtered(coll, filter, processorInstance.getAgentInstanceContext());
        }
        return coll;
    }

    static ResultSetProcessor processorWithAssign(ResultSetProcessorFactoryProvider processorProvider, AgentInstanceContext agentInstanceContext, FAFQueryMethodAssignerSetter assignerSetter, Map<Integer, ExprTableEvalStrategyFactory> tableAccesses, Map<Integer, SubSelectFactory> subselects) {
        // start table-access
        Map<Integer, ExprTableEvalStrategy> tableAccessEvals = ExprTableEvalHelperStart.startTableAccess(tableAccesses, agentInstanceContext);

        // get RSP
        Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(processorProvider, agentInstanceContext, false, null);

        // start subselects
        List<AgentInstanceStopCallback> subselectStopCallbacks = new ArrayList<>(2);
        Map<Integer, SubSelectFactoryResult> subselectActivations = SubSelectHelperStart.startSubselects(subselects, agentInstanceContext, subselectStopCallbacks, false);

        // assign
        assignerSetter.assign(new StatementAIFactoryAssignmentsImpl(pair.getSecond(), null, null, subselectActivations, tableAccessEvals, null));

        return pair.getFirst();
    }

    static Collection<EventBean> filtered(Collection<EventBean> snapshot, ExprEvaluator filterExpressions, AgentInstanceContext agentInstanceContext) {
        ArrayDeque<EventBean> deque = new ArrayDeque<>(Math.min(snapshot.size(), 16));
        ExprNodeUtilityEvaluate.applyFilterExpressionIterable(snapshot.iterator(), filterExpressions, agentInstanceContext, deque);
        return deque;
    }

    static EPPreparedQueryResult processedNonJoin(ResultSetProcessor resultSetProcessor, Collection<EventBean> events, EventPropertyValueGetter distinctKeyGetter) {
        EventBean[] rows = events.toArray(new EventBean[events.size()]);
        UniformPair<EventBean[]> results = resultSetProcessor.processViewResult(rows, null, true);

        EventBean[] distinct;
        if (distinctKeyGetter == null) {
            distinct = results.getFirst();
        } else {
            distinct = EventBeanUtility.getDistinctByProp(results.getFirst(), distinctKeyGetter);
        }

        return new EPPreparedQueryResult(resultSetProcessor.getResultEventType(), distinct);
    }
}
