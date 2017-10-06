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

import java.util.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByRefcountedWAccessImpl extends AggregationServiceBaseGrouped {
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;

    // maintain for each group a row of aggregator states that the expression node canb pull the data from via index
    protected Map<Object, AggregationMethodPairRow> aggregatorsPerGroup;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    private AggregationMethod[] currentAggregatorMethods;
    private AggregationState[] currentAggregatorStates;
    private Object currentGroupKey;

    protected List<Object> removedKeys;

    /**
     * Ctor.
     *
     * @param evaluators         - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes         - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     *                           aggregation states for each group
     * @param accessors          accessor definitions
     * @param accessAggregations access aggs
     * @param isJoin             true for join, false for single-stream
     */
    public AggSvcGroupByRefcountedWAccessImpl(ExprEvaluator[] evaluators,
                                              AggregationMethodFactory[] prototypes,
                                              AggregationAccessorSlotPair[] accessors,
                                              AggregationStateFactory[] accessAggregations,
                                              boolean isJoin) {
        super(evaluators, prototypes);
        this.aggregatorsPerGroup = new HashMap<>();
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        this.isJoin = isJoin;
        removedKeys = new ArrayList<>();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        aggregatorsPerGroup.clear();
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, aggregators.length, accessAggregations.length, groupByKey);
        }
        handleRemovedKeys();

        AggregationMethodPairRow row = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        AggregationState[] groupStates;
        if (row == null) {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            row = new AggregationMethodPairRow(1, groupAggregators, groupStates);
            aggregatorsPerGroup.put(groupByKey, row);
        } else {
            groupAggregators = row.getMethods();
            groupStates = row.getStates();
            row.increaseRefcount();
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorMethods = groupAggregators;
        currentAggregatorStates = groupStates;
        for (int j = 0; j < evaluators.length; j++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, j, groupAggregators[j], aggregators[j].getAggregationExpression());
            }
            Object columnResult = evaluators[j].evaluate(eventsPerStream, true, exprEvaluatorContext);
            groupAggregators[j].enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, j, groupAggregators[j]);
            }
        }

        for (int i = 0; i < currentAggregatorStates.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, currentAggregatorStates[i], accessAggregations[i].getAggregationExpression());
            }
            currentAggregatorStates[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, currentAggregatorStates[i]);
            }
        }

        internalHandleGroupUpdate(groupByKey, row);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, aggregators.length, accessAggregations.length, groupByKey);
        }
        AggregationMethodPairRow row = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        AggregationState[] groupStates;
        if (row != null) {
            groupAggregators = row.getMethods();
            groupStates = row.getStates();
        } else {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupByKey, null);
            row = new AggregationMethodPairRow(1, groupAggregators, groupStates);
            aggregatorsPerGroup.put(groupByKey, row);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorMethods = groupAggregators;
        currentAggregatorStates = groupStates;
        for (int j = 0; j < evaluators.length; j++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, j, groupAggregators[j], aggregators[j].getAggregationExpression());
            }
            Object columnResult = evaluators[j].evaluate(eventsPerStream, false, exprEvaluatorContext);
            groupAggregators[j].leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, j, groupAggregators[j]);
            }
        }

        for (int i = 0; i < currentAggregatorStates.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, currentAggregatorStates[i], accessAggregations[i].getAggregationExpression());
            }
            currentAggregatorStates[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, currentAggregatorStates[i]);
            }
        }

        row.decreaseRefcount();
        if (row.getRefcount() <= 0) {
            removedKeys.add(groupByKey);
        }

        internalHandleGroupUpdate(groupByKey, row);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        AggregationMethodPairRow row = aggregatorsPerGroup.get(groupByKey);

        if (row != null) {
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        } else {
            currentAggregatorMethods = null;
        }

        if (currentAggregatorMethods == null) {
            currentAggregatorMethods = AggSvcGroupByUtil.newAggregators(aggregators);
            currentAggregatorStates = AggSvcGroupByUtil.newAccesses(agentInstanceId, isJoin, accessAggregations, groupByKey, null);
        }

        this.currentGroupKey = groupByKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (column < aggregators.length) {
            return currentAggregatorMethods[column].getValue();
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getValue(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvents(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableScalar(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvent(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void internalHandleGroupUpdate(Object groupByKey, AggregationMethodPairRow row) {
        // no action required
    }

    public void internalHandleGroupRemove(Object groupByKey) {
        // no action required
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(aggregatorsPerGroup.size(), aggregatorsPerGroup);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(aggregatorsPerGroup.size());
        for (Map.Entry<Object, AggregationMethodPairRow> entry : aggregatorsPerGroup.entrySet()) {
            visitor.visitGroup(entry.getKey(), entry.getValue());
        }
    }

    public boolean isGrouped() {
        return true;
    }

    protected void handleRemovedKeys() {
        // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty methodFactories temporarily
        if (!removedKeys.isEmpty()) {
            for (Object removedKey : removedKeys) {
                aggregatorsPerGroup.remove(removedKey);
                internalHandleGroupRemove(removedKey);
            }
            removedKeys.clear();
        }
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        handleRemovedKeys();
        return aggregatorsPerGroup.keySet();
    }
}
