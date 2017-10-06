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
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByNoReclaimMixedAccessImpl extends AggregationServiceBaseGrouped {
    private final AggregationAccessorSlotPair[] accessorsFactory;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;

    // maintain for each group a row of aggregator states that the expression node canb pull the data from via index
    protected Map<Object, AggregationRowPair> aggregatorsPerGroup;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    private AggregationRowPair currentAggregatorRow;
    private Object currentGroupKey;

    /**
     * Ctor.
     *
     * @param evaluators         - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes         - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     *                           aggregation states for each group
     * @param accessorsFactory   accessor definitions
     * @param accessAggregations access aggs
     * @param isJoin             true for join, false for single-stream
     */
    public AggSvcGroupByNoReclaimMixedAccessImpl(ExprEvaluator[] evaluators,
                                                 AggregationMethodFactory[] prototypes,
                                                 AggregationAccessorSlotPair[] accessorsFactory,
                                                 AggregationStateFactory[] accessAggregations,
                                                 boolean isJoin) {
        super(evaluators, prototypes);
        this.accessorsFactory = accessorsFactory;
        this.accessAggregations = accessAggregations;
        this.isJoin = isJoin;
        this.aggregatorsPerGroup = new HashMap<Object, AggregationRowPair>();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        aggregatorsPerGroup.clear();
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, aggregators.length, accessAggregations.length, groupByKey);
        }
        AggregationRowPair groupAggregators = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        if (groupAggregators == null) {
            AggregationMethod[] methods = AggSvcGroupByUtil.newAggregators(aggregators);
            AggregationState[] states = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            groupAggregators = new AggregationRowPair(methods, states);
            aggregatorsPerGroup.put(groupByKey, groupAggregators);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorRow = groupAggregators;
        AggregationMethod[] groupAggMethods = groupAggregators.getMethods();
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, groupAggMethods[i], aggregators[i].getAggregationExpression());
            }
            Object columnResult = evaluators[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            groupAggMethods[i].enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, groupAggMethods[i]);
            }
        }

        AggregationState[] states = currentAggregatorRow.getStates();
        for (int i = 0; i < states.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, states[i], accessAggregations[i].getAggregationExpression());
            }
            states[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, states[i]);
            }
        }

        internalHandleUpdated(groupByKey, groupAggregators);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, aggregators.length, accessAggregations.length, groupByKey);
        }
        AggregationRowPair groupAggregators = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        if (groupAggregators == null) {
            AggregationMethod[] methods = AggSvcGroupByUtil.newAggregators(aggregators);
            AggregationState[] states = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            groupAggregators = new AggregationRowPair(methods, states);
            aggregatorsPerGroup.put(groupByKey, groupAggregators);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorRow = groupAggregators;
        AggregationMethod[] groupAggMethods = groupAggregators.getMethods();
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, groupAggMethods[i], aggregators[i].getAggregationExpression());
            }
            Object columnResult = evaluators[i].evaluate(eventsPerStream, false, exprEvaluatorContext);
            groupAggMethods[i].leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, groupAggMethods[i]);
            }
        }

        AggregationState[] states = currentAggregatorRow.getStates();
        for (int i = 0; i < states.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, states[i], accessAggregations[i].getAggregationExpression());
            }
            states[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, states[i]);
            }
        }

        internalHandleUpdated(groupByKey, groupAggregators);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        currentAggregatorRow = aggregatorsPerGroup.get(groupByKey);
        this.currentGroupKey = groupByKey;

        if (currentAggregatorRow == null) {
            AggregationMethod[] methods = AggSvcGroupByUtil.newAggregators(aggregators);
            AggregationState[] states = AggSvcGroupByUtil.newAccesses(agentInstanceId, isJoin, accessAggregations, groupByKey, null);
            currentAggregatorRow = new AggregationRowPair(methods, states);
            aggregatorsPerGroup.put(groupByKey, currentAggregatorRow);
        }
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (column < aggregators.length) {
            return currentAggregatorRow.getMethods()[column].getValue();
        } else {
            AggregationAccessorSlotPair pair = accessorsFactory[column - aggregators.length];
            return pair.getAccessor().getValue(currentAggregatorRow.getStates()[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessorsFactory[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvents(currentAggregatorRow.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessorsFactory[column - aggregators.length];
            return pair.getAccessor().getEnumerableScalar(currentAggregatorRow.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessorsFactory[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvent(currentAggregatorRow.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void internalHandleUpdated(Object groupByKey, AggregationRowPair groupAggregators) {
        // no action required
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(aggregatorsPerGroup.size(), aggregatorsPerGroup);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(aggregatorsPerGroup.size());
        for (Map.Entry<Object, AggregationRowPair> entry : aggregatorsPerGroup.entrySet()) {
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
        return aggregatorsPerGroup.keySet();
    }
}