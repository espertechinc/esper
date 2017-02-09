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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

/**
 * Aggregation service for use when only first/last/window aggregation functions are used an none other.
 */
public class AggSvcGroupAllAccessOnlyImpl implements AggregationService, AggregationResultFuture {
    private final AggregationAccessorSlotPair[] accessors;
    protected final AggregationState[] states;
    private final AggregationStateFactory[] accessAggSpecs;

    public AggSvcGroupAllAccessOnlyImpl(AggregationAccessorSlotPair[] accessors, AggregationState[] states, AggregationStateFactory[] accessAggSpecs) {
        this.accessors = accessors;
        this.states = states;
        this.accessAggSpecs = accessAggSpecs;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(true, 0, accessAggSpecs.length);
        }
        for (int i = 0; i < states.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, states[i], accessAggSpecs[i].getAggregationExpression());
            }
            states[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, states[i]);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(false, 0, accessAggSpecs.length);
        }
        for (int i = 0; i < states.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, states[i], accessAggSpecs[i].getAggregationExpression());
            }
            states[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, states[i]);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        // no implementation required
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getValue(states[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getEnumerableEvent(states[pair.getSlot()], eventsPerStream, isNewData, context);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getEnumerableEvents(states[pair.getSlot()], eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationAccessorSlotPair pair = accessors[column];
        return pair.getAccessor().getEnumerableScalar(states[pair.getSlot()], eventsPerStream, isNewData, context);
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        for (AggregationState state : states) {
            state.clear();
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(1, states);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
    }

    public boolean isGrouped() {
        return false;
    }

    public Object getGroupKey(int agentInstanceId) {
        return null;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public void stop() {
    }
}