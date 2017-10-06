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
package com.espertech.esper.epl.agg.service.groupbylocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByColumn;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByLevel;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public abstract class AggSvcGroupLocalGroupByBase implements AggregationService {
    protected final boolean isJoin;
    protected final AggregationLocalGroupByPlan localGroupByPlan;

    // state
    protected AggregationMethod[] aggregatorsTopLevel;
    protected AggregationState[] statesTopLevel;
    protected Map<Object, AggregationMethodPairRow>[] aggregatorsPerLevelAndGroup;
    protected List<Pair<Integer, Object>> removedKeys;

    protected abstract Object computeGroupKey(AggregationLocalGroupByLevel level, Object groupKey, ExprEvaluator[] partitionEval, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext);

    public AggSvcGroupLocalGroupByBase(boolean isJoin,
                                       AggregationLocalGroupByPlan localGroupByPlan) {
        this.isJoin = isJoin;
        this.localGroupByPlan = localGroupByPlan;

        this.aggregatorsPerLevelAndGroup = new Map[localGroupByPlan.getAllLevels().length];
        for (int i = 0; i < localGroupByPlan.getAllLevels().length; i++) {
            this.aggregatorsPerLevelAndGroup[i] = new HashMap<Object, AggregationMethodPairRow>();
        }
        removedKeys = new ArrayList<Pair<Integer, Object>>();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        clearResults(aggregatorsPerLevelAndGroup, aggregatorsTopLevel, statesTopLevel);
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKeyProvided, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, localGroupByPlan.getNumMethods(), localGroupByPlan.getNumAccess(), groupByKeyProvided);
        }
        handleRemovedKeys();

        if (localGroupByPlan.getOptionalLevelTop() != null) {
            if (aggregatorsTopLevel == null) {
                aggregatorsTopLevel = AggSvcGroupByUtil.newAggregators(localGroupByPlan.getOptionalLevelTop().getMethodFactories());
                statesTopLevel = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, localGroupByPlan.getOptionalLevelTop().getStateFactories(), null, null);
            }
            aggregateIntoEnter(localGroupByPlan.getOptionalLevelTop(), aggregatorsTopLevel, statesTopLevel, eventsPerStream, exprEvaluatorContext);
            internalHandleUpdatedTop();
        }

        for (int levelNum = 0; levelNum < localGroupByPlan.getAllLevels().length; levelNum++) {
            AggregationLocalGroupByLevel level = localGroupByPlan.getAllLevels()[levelNum];
            ExprEvaluator[] partitionEval = level.getPartitionEvaluators();
            Object groupByKey = computeGroupKey(level, groupByKeyProvided, partitionEval, eventsPerStream, true, exprEvaluatorContext);
            AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[levelNum].get(groupByKey);
            if (row == null) {
                AggregationMethod[] rowAggregators = AggSvcGroupByUtil.newAggregators(level.getMethodFactories());
                AggregationState[] rowStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, level.getStateFactories(), groupByKey, null);
                row = new AggregationMethodPairRow(1, rowAggregators, rowStates);
                aggregatorsPerLevelAndGroup[levelNum].put(groupByKey, row);
            } else {
                row.increaseRefcount();
            }

            aggregateIntoEnter(level, row.getMethods(), row.getStates(), eventsPerStream, exprEvaluatorContext);
            internalHandleUpdatedGroup(levelNum, groupByKey, row);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKeyProvided, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, localGroupByPlan.getNumMethods(), localGroupByPlan.getNumAccess(), groupByKeyProvided);
        }
        if (localGroupByPlan.getOptionalLevelTop() != null) {
            if (aggregatorsTopLevel == null) {
                aggregatorsTopLevel = AggSvcGroupByUtil.newAggregators(localGroupByPlan.getOptionalLevelTop().getMethodFactories());
                statesTopLevel = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, localGroupByPlan.getOptionalLevelTop().getStateFactories(), null, null);
            }
            aggregateIntoLeave(localGroupByPlan.getOptionalLevelTop(), aggregatorsTopLevel, statesTopLevel, eventsPerStream, exprEvaluatorContext);
            internalHandleUpdatedTop();
        }

        for (int levelNum = 0; levelNum < localGroupByPlan.getAllLevels().length; levelNum++) {
            AggregationLocalGroupByLevel level = localGroupByPlan.getAllLevels()[levelNum];
            ExprEvaluator[] partitionEval = level.getPartitionEvaluators();
            Object groupByKey = computeGroupKey(level, groupByKeyProvided, partitionEval, eventsPerStream, true, exprEvaluatorContext);
            AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[levelNum].get(groupByKey);
            if (row == null) {
                AggregationMethod[] rowAggregators = AggSvcGroupByUtil.newAggregators(level.getMethodFactories());
                AggregationState[] rowStates = AggSvcGroupByUtil.newAccesses(exprEvaluatorContext.getAgentInstanceId(), isJoin, level.getStateFactories(), groupByKey, null);
                row = new AggregationMethodPairRow(1, rowAggregators, rowStates);
                aggregatorsPerLevelAndGroup[levelNum].put(groupByKey, row);
            } else {
                row.decreaseRefcount();
                if (row.getRefcount() <= 0) {
                    removedKeys.add(new Pair<Integer, Object>(levelNum, groupByKey));
                }
            }
            aggregateIntoLeave(level, row.getMethods(), row.getStates(), eventsPerStream, exprEvaluatorContext);
            internalHandleUpdatedGroup(levelNum, groupByKey, row);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationLocalGroupByColumn col = localGroupByPlan.getColumns()[column];
        if (col.getPartitionEvaluators().length == 0) {
            return col.getPair().getAccessor().getEnumerableEvents(statesTopLevel[col.getPair().getSlot()], eventsPerStream, isNewData, context);
        }
        Object groupByKey = computeGroupKey(col.getPartitionEvaluators(), eventsPerStream, isNewData, context);
        AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[col.getLevelNum()].get(groupByKey);
        return col.getPair().getAccessor().getEnumerableEvents(row.getStates()[col.getPair().getSlot()], eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationLocalGroupByColumn col = localGroupByPlan.getColumns()[column];
        if (col.getPartitionEvaluators().length == 0) {
            return col.getPair().getAccessor().getEnumerableScalar(statesTopLevel[col.getPair().getSlot()], eventsPerStream, isNewData, context);
        }
        Object groupByKey = computeGroupKey(col.getPartitionEvaluators(), eventsPerStream, isNewData, context);
        AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[col.getLevelNum()].get(groupByKey);
        return col.getPair().getAccessor().getEnumerableScalar(row.getStates()[col.getPair().getSlot()], eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        AggregationLocalGroupByColumn col = localGroupByPlan.getColumns()[column];
        if (col.getPartitionEvaluators().length == 0) {
            return col.getPair().getAccessor().getEnumerableEvent(statesTopLevel[col.getPair().getSlot()], eventsPerStream, isNewData, context);
        }
        Object groupByKey = computeGroupKey(col.getPartitionEvaluators(), eventsPerStream, isNewData, context);
        AggregationMethodPairRow row = aggregatorsPerLevelAndGroup[col.getLevelNum()].get(groupByKey);
        return col.getPair().getAccessor().getEnumerableEvent(row.getStates()[col.getPair().getSlot()], eventsPerStream, isNewData, context);
    }

    public boolean isGrouped() {
        return true;
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(getNumGroups(), aggregatorsTopLevel, statesTopLevel, aggregatorsPerLevelAndGroup);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(getNumGroups());
        if (aggregatorsTopLevel != null) {
            visitor.visitGroup(null, aggregatorsTopLevel, statesTopLevel);
        }
        for (int i = 0; i < localGroupByPlan.getAllLevels().length; i++) {
            for (Map.Entry<Object, AggregationMethodPairRow> entry : aggregatorsPerLevelAndGroup[i].entrySet()) {
                visitor.visitGroup(entry.getKey(), entry.getValue());
            }
        }
    }

    public void internalHandleUpdatedGroup(int level, Object groupByKey, AggregationMethodPairRow row) {
        // no action required
    }

    public void internalHandleUpdatedTop() {
        // no action required
    }

    public void internalHandleGroupRemove(Pair<Integer, Object> groupByKey) {
        // no action required
    }

    public void handleRemovedKeys() {
        // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty methodFactories temporarily
        if (!removedKeys.isEmpty()) {
            for (Pair<Integer, Object> removedKey : removedKeys) {
                aggregatorsPerLevelAndGroup[removedKey.getFirst()].remove(removedKey.getSecond());
                internalHandleGroupRemove(removedKey);
            }
            removedKeys.clear();
        }
    }

    public Object getGroupKey(int agentInstanceId) {
        return null;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public static Object computeGroupKey(ExprEvaluator[] partitionEval, EventBean[] eventsPerStream, boolean b, ExprEvaluatorContext exprEvaluatorContext) {
        if (partitionEval.length == 1) {
            return partitionEval[0].evaluate(eventsPerStream, true, exprEvaluatorContext);
        }
        Object[] keys = new Object[partitionEval.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = partitionEval[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
        }
        return new MultiKeyUntyped(keys);
    }

    public static void aggregateIntoEnter(AggregationLocalGroupByLevel level, AggregationMethod[] methods, AggregationState[] states, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        for (int i = 0; i < level.getMethodEvaluators().length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, methods[i], level.getMethodFactories()[i].getAggregationExpression());
            }
            Object value = level.getMethodEvaluators()[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            methods[i].enter(value);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, methods[i]);
            }
        }
        for (int i = 0; i < states.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, states[i], level.getStateFactories()[i].getAggregationExpression());
            }
            states[i].applyEnter(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, states[i]);
            }
        }
    }

    public static void aggregateIntoLeave(AggregationLocalGroupByLevel level, AggregationMethod[] methods, AggregationState[] states, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        for (int i = 0; i < level.getMethodEvaluators().length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, methods[i], level.getMethodFactories()[i].getAggregationExpression());
            }
            Object value = level.getMethodEvaluators()[i].evaluate(eventsPerStream, false, exprEvaluatorContext);
            methods[i].leave(value);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, methods[i]);
            }
        }
        for (int i = 0; i < states.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, states[i], level.getStateFactories()[i].getAggregationExpression());
            }
            states[i].applyLeave(eventsPerStream, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, states[i]);
            }
        }
    }

    protected static void clearResults(Map<Object, AggregationMethodPairRow>[] aggregatorsPerLevelAndGroup, AggregationMethod[] aggregatorsTopLevel, AggregationState[] statesTopLevel) {
        for (Map<Object, AggregationMethodPairRow> aggregatorsPerGroup : aggregatorsPerLevelAndGroup) {
            aggregatorsPerGroup.clear();
        }
        if (aggregatorsTopLevel != null) {
            for (AggregationMethod method : aggregatorsTopLevel) {
                method.clear();
            }
            for (AggregationState state : statesTopLevel) {
                state.clear();
            }
        }
    }

    public AggregationMethod[] getAggregatorsTopLevel() {
        return aggregatorsTopLevel;
    }

    public void setAggregatorsTopLevel(AggregationMethod[] aggregatorsTopLevel) {
        this.aggregatorsTopLevel = aggregatorsTopLevel;
    }

    public AggregationState[] getStatesTopLevel() {
        return statesTopLevel;
    }

    public void setStatesTopLevel(AggregationState[] statesTopLevel) {
        this.statesTopLevel = statesTopLevel;
    }

    public Map<Object, AggregationMethodPairRow>[] getAggregatorsPerLevelAndGroup() {
        return aggregatorsPerLevelAndGroup;
    }

    public void setAggregatorsPerLevelAndGroup(Map<Object, AggregationMethodPairRow>[] aggregatorsPerLevelAndGroup) {
        this.aggregatorsPerLevelAndGroup = aggregatorsPerLevelAndGroup;
    }

    public List<Pair<Integer, Object>> getRemovedKeys() {
        return removedKeys;
    }

    public void setRemovedKeys(List<Pair<Integer, Object>> removedKeys) {
        this.removedKeys = removedKeys;
    }

    public void stop() {
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }

    private int getNumGroups() {
        int size = aggregatorsTopLevel != null ? 1 : 0;
        for (int i = 0; i < localGroupByPlan.getAllLevels().length; i++) {
            size += aggregatorsPerLevelAndGroup[i].size();
        }
        return size;
    }
}
