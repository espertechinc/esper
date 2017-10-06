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
package com.espertech.esper.epl.agg.service.groupbyrollup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;

import java.util.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByRollupImpl extends AggregationServiceBaseGrouped {
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;
    protected final AggregationGroupByRollupDesc rollupLevelDesc;

    // maintain for each group a row of aggregator states that the expression node can pull the data from via index
    protected Map<Object, AggregationMethodPairRow>[] aggregatorsPerGroup;
    protected AggregationMethodPairRow aggregatorTopGroup;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    private AggregationMethod[] currentAggregatorMethods;
    private AggregationState[] currentAggregatorStates;
    private Object currentGroupKey;

    protected final Object[] methodParameterValues;
    protected boolean hasRemovedKey;
    protected final List<Object>[] removedKeys;

    /**
     * Ctor.
     *
     * @param evaluators          - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes          - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     *                            aggregation states for each group
     * @param accessors           accessor definitions
     * @param accessAggregations  access aggs
     * @param isJoin              true for join, false for single-stream
     * @param rollupLevelDesc     rollup info
     * @param topGroupAggregators methodFactories for top group
     * @param topGroupStates      states for top group
     */
    public AggSvcGroupByRollupImpl(ExprEvaluator[] evaluators,
                                   AggregationMethodFactory[] prototypes,
                                   AggregationAccessorSlotPair[] accessors,
                                   AggregationStateFactory[] accessAggregations,
                                   boolean isJoin,
                                   AggregationGroupByRollupDesc rollupLevelDesc,
                                   AggregationMethod[] topGroupAggregators,
                                   AggregationState[] topGroupStates) {
        super(evaluators, prototypes);

        this.aggregatorsPerGroup = (Map<Object, AggregationMethodPairRow>[]) new Map[rollupLevelDesc.getNumLevelsAggregation()];
        this.removedKeys = (List<Object>[]) new ArrayList[rollupLevelDesc.getNumLevelsAggregation()];
        for (int i = 0; i < rollupLevelDesc.getNumLevelsAggregation(); i++) {
            this.aggregatorsPerGroup[i] = new HashMap<Object, AggregationMethodPairRow>();
            this.removedKeys[i] = new ArrayList<Object>(2);
        }
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        this.isJoin = isJoin;
        this.rollupLevelDesc = rollupLevelDesc;
        this.aggregatorTopGroup = new AggregationMethodPairRow(0, topGroupAggregators, topGroupStates);
        this.methodParameterValues = new Object[evaluators.length];
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        for (AggregationState state : aggregatorTopGroup.getStates()) {
            state.clear();
        }
        for (AggregationMethod aggregator : aggregatorTopGroup.getMethods()) {
            aggregator.clear();
        }
        for (int i = 0; i < rollupLevelDesc.getNumLevelsAggregation(); i++) {
            aggregatorsPerGroup[i].clear();
        }
    }

    public void applyEnter(EventBean[] eventsPerStream, Object compositeGroupKey, ExprEvaluatorContext exprEvaluatorContext) {
        handleRemovedKeys();

        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggregationGroupedRollupEvalParam(true, methodParameterValues.length);
            }
            methodParameterValues[i] = evaluators[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggregationGroupedRollupEvalParam(methodParameterValues[i]);
            }
        }

        Object[] groupKeyPerLevel = (Object[]) compositeGroupKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = rollupLevelDesc.getLevels()[i];
            Object groupKey = groupKeyPerLevel[i];

            AggregationMethodPairRow row;
            if (!level.isAggregationTop()) {
                row = aggregatorsPerGroup[level.getAggregationOffset()].get(groupKey);
            } else {
                row = aggregatorTopGroup;
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, aggregators.length, accessAggregations.length, groupKey);
            }

            // The methodFactories for this group do not exist, need to create them from the prototypes
            AggregationMethod[] groupAggregators;
            AggregationState[] groupStates;
            if (row == null) {
                groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
                groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupKey, null);
                row = new AggregationMethodPairRow(1, groupAggregators, groupStates);
                if (!level.isAggregationTop()) {
                    aggregatorsPerGroup[level.getAggregationOffset()].put(groupKey, row);
                }
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
                groupAggregators[j].enter(methodParameterValues[j]);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aAggNoAccessEnterLeave(true, j, groupAggregators[j]);
                }
            }

            for (int j = 0; j < currentAggregatorStates.length; j++) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qAggAccessEnterLeave(true, j, currentAggregatorStates[j], accessAggregations[j].getAggregationExpression());
                }
                currentAggregatorStates[j].applyEnter(eventsPerStream, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aAggAccessEnterLeave(true, j, currentAggregatorStates[j]);
                }
            }

            internalHandleGroupUpdate(groupKey, row, level);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
            }
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object compositeGroupKey, ExprEvaluatorContext exprEvaluatorContext) {
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggregationGroupedRollupEvalParam(false, methodParameterValues.length);
            }
            methodParameterValues[i] = evaluators[i].evaluate(eventsPerStream, false, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggregationGroupedRollupEvalParam(methodParameterValues[i]);
            }
        }

        Object[] groupKeyPerLevel = (Object[]) compositeGroupKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = rollupLevelDesc.getLevels()[i];
            Object groupKey = groupKeyPerLevel[i];

            AggregationMethodPairRow row;
            if (!level.isAggregationTop()) {
                row = aggregatorsPerGroup[level.getAggregationOffset()].get(groupKey);
            } else {
                row = aggregatorTopGroup;
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, aggregators.length, accessAggregations.length, groupKey);
            }

            // The methodFactories for this group do not exist, need to create them from the prototypes
            AggregationMethod[] groupAggregators;
            AggregationState[] groupStates;
            if (row != null) {
                groupAggregators = row.getMethods();
                groupStates = row.getStates();
            } else {
                groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
                groupStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, accessAggregations, groupKey, null);
                row = new AggregationMethodPairRow(1, groupAggregators, groupStates);
                if (!level.isAggregationTop()) {
                    aggregatorsPerGroup[level.getAggregationOffset()].put(groupKey, row);
                }
            }

            // For this row, evaluate sub-expressions, enter result
            currentAggregatorMethods = groupAggregators;
            currentAggregatorStates = groupStates;
            for (int j = 0; j < evaluators.length; j++) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qAggNoAccessEnterLeave(false, j, groupAggregators[j], aggregators[j].getAggregationExpression());
                }
                groupAggregators[j].leave(methodParameterValues[j]);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aAggNoAccessEnterLeave(false, j, groupAggregators[j]);
                }
            }

            for (int j = 0; j < currentAggregatorStates.length; j++) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qAggAccessEnterLeave(false, j, currentAggregatorStates[j], accessAggregations[j].getAggregationExpression());
                }
                currentAggregatorStates[j].applyLeave(eventsPerStream, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aAggAccessEnterLeave(false, j, currentAggregatorStates[j]);
                }
            }

            row.decreaseRefcount();
            if (row.getRefcount() <= 0) {
                hasRemovedKey = true;
                if (!level.isAggregationTop()) {
                    removedKeys[level.getAggregationOffset()].add(groupKey);
                }
            }

            internalHandleGroupUpdate(groupKey, row, level);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
            }
        }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        AggregationMethodPairRow row;
        if (rollupLevel.isAggregationTop()) {
            row = aggregatorTopGroup;
        } else {
            row = aggregatorsPerGroup[rollupLevel.getAggregationOffset()].get(groupByKey);
        }

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

    public void internalHandleGroupUpdate(Object groupByKey, AggregationMethodPairRow row, AggregationGroupByRollupLevel groupByRollupLevel) {
        // no action required
    }

    public void internalHandleGroupRemove(Object groupByKey, AggregationGroupByRollupLevel groupByRollupLevel) {
        // no action required
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(getGroupKeyCount(), aggregatorsPerGroup);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(getGroupKeyCount());
        for (Map<Object, AggregationMethodPairRow> anAggregatorsPerGroup : aggregatorsPerGroup) {
            for (Map.Entry<Object, AggregationMethodPairRow> entry : anAggregatorsPerGroup.entrySet()) {
                visitor.visitGroup(entry.getKey(), entry.getValue());
            }
        }
        visitor.visitGroup(CollectionUtil.OBJECTARRAY_EMPTY, aggregatorTopGroup);
    }

    public boolean isGrouped() {
        return true;
    }

    protected void handleRemovedKeys() {
        if (!hasRemovedKey) {
            return;
        }
        hasRemovedKey = false;
        for (int i = 0; i < removedKeys.length; i++) {
            if (removedKeys[i].isEmpty()) {
                continue;
            }
            for (Object removedKey : removedKeys[i]) {
                aggregatorsPerGroup[i].remove(removedKey);
                internalHandleGroupRemove(removedKey, rollupLevelDesc.getLevels()[i]);
            }
            removedKeys[i].clear();
        }
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    private int getGroupKeyCount() {
        int count = 1;
        for (Map<Object, AggregationMethodPairRow> anAggregatorsPerGroup : aggregatorsPerGroup) {
            count += anAggregatorsPerGroup.size();
        }
        return count;
    }
}
