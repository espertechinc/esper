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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregation service for use when only first/last/window aggregation functions are used an none other.
 */
public class AggSvcGroupByNoReclaimAccessOnlyImpl implements AggregationService, AggregationResultFuture {
    private final Map<Object, AggregationState[]> accessMap;
    private final AggregationAccessorSlotPair[] accessors;
    private final AggregationStateFactory[] accessAggSpecs;
    private final boolean isJoin;

    private AggregationState[] currentAccesses;
    private Object currentGroupKey;

    /**
     * Ctor.
     *
     * @param accessors      accessor definitions
     * @param accessAggSpecs access agg specs
     * @param isJoin         true for join, false for single-stream
     */
    public AggSvcGroupByNoReclaimAccessOnlyImpl(AggregationAccessorSlotPair[] accessors,
                                                AggregationStateFactory[] accessAggSpecs,
                                                boolean isJoin) {
        this.accessMap = new HashMap<Object, AggregationState[]>();
        this.accessors = accessors;
        this.accessAggSpecs = accessAggSpecs;
        this.isJoin = isJoin;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, 0, accessAggSpecs.length, groupKey);
        }
        AggregationState[] row = getAssertRow(exprEvaluatorContext.getAgentInstanceId(), groupKey);
        for (int i = 0; i < row.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, row[i], accessAggSpecs[i].getAggregationExpression());
            }
            row[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, row[i]);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, 0, accessAggSpecs.length, groupKey);
        }
        AggregationState[] row = getAssertRow(exprEvaluatorContext.getAgentInstanceId(), groupKey);
        for (int i = 0; i < row.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, row[i], accessAggSpecs[i].getAggregationExpression());
            }
            row[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, row[i]);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        currentAccesses = getAssertRow(agentInstanceId, groupKey);
        currentGroupKey = groupKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getValue(currentAccesses[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getEnumerableEvents(currentAccesses[pair.getSlot()], eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getEnumerableScalar(currentAccesses[pair.getSlot()], eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getEnumerableEvent(currentAccesses[pair.getSlot()], eventsPerStream, isNewData, context);
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        accessMap.clear();
    }

    private AggregationState[] getAssertRow(int agentInstanceId, Object groupKey) {
        AggregationState[] row = accessMap.get(groupKey);
        if (row != null) {
            return row;
        }

        row = AggSvcGroupByUtil.newAccesses(agentInstanceId, isJoin, accessAggSpecs, groupKey, null);
        accessMap.put(groupKey, row);
        return row;
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(accessMap.size(), accessMap);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(accessMap.size());
        for (Map.Entry<Object, AggregationState[]> entry : accessMap.entrySet()) {
            visitor.visitGroup(entry.getKey(), entry.getValue());
        }
    }

    public boolean isGrouped() {
        return true;
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return accessMap.keySet();
    }

    public void stop() {
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }
}
