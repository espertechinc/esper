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
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;
import com.espertech.esper.epl.table.strategy.ExprTableEvalLockUtil;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public abstract class AggSvcGroupByWTableBase implements AggregationService {
    protected final TableMetadata tableMetadata;
    protected final TableColumnMethodPair[] methodPairs;
    protected final AggregationAccessorSlotPair[] accessors;
    protected final boolean isJoin;
    protected final TableStateInstanceGrouped tableStateInstance;
    protected final int[] targetStates;
    protected final ExprNode[] accessStateExpr;
    private final AggregationAgent[] agents;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    protected AggregationMethod[] currentAggregatorMethods;
    protected AggregationState[] currentAggregatorStates;
    protected Object currentGroupKey;

    public AggSvcGroupByWTableBase(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, boolean join, TableStateInstanceGrouped tableStateInstance, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        this.tableMetadata = tableMetadata;
        this.methodPairs = methodPairs;
        this.accessors = accessors;
        isJoin = join;
        this.tableStateInstance = tableStateInstance;
        this.targetStates = targetStates;
        this.accessStateExpr = accessStateExpr;
        this.agents = agents;
    }

    public abstract void applyEnterInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public abstract void applyLeaveInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
        applyEnterInternal(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
        applyLeaveInternal(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    protected void applyEnterGroupKey(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = tableStateInstance.getCreateRowIntoTable(groupByKey, exprEvaluatorContext);
        AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];

        currentAggregatorMethods = row.getMethods();
        currentAggregatorStates = row.getStates();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, methodPairs.length, targetStates.length, groupByKey);
        }

        for (int j = 0; j < methodPairs.length; j++) {
            TableColumnMethodPair methodPair = methodPairs[j];
            AggregationMethod method = currentAggregatorMethods[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, j, method, methodPair.getAggregationNode());
            }
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            method.enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, j, method);
            }
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = currentAggregatorStates[targetStates[i]];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, state, accessStateExpr[i]);
            }
            agents[i].applyEnter(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, state);
            }
        }

        tableStateInstance.handleRowUpdated(bean);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    protected void applyLeaveGroupKey(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = tableStateInstance.getCreateRowIntoTable(groupByKey, exprEvaluatorContext);
        AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];

        currentAggregatorMethods = row.getMethods();
        currentAggregatorStates = row.getStates();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, methodPairs.length, targetStates.length, groupByKey);
        }

        for (int j = 0; j < methodPairs.length; j++) {
            TableColumnMethodPair methodPair = methodPairs[j];
            AggregationMethod method = currentAggregatorMethods[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, j, method, methodPair.getAggregationNode());
            }
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, false, exprEvaluatorContext);
            method.leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, j, method);
            }
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = currentAggregatorStates[targetStates[i]];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, state, accessStateExpr[i]);
            }
            agents[i].applyLeave(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, state);
            }
        }

        tableStateInstance.handleRowUpdated(bean);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        ObjectArrayBackedEventBean bean = tableStateInstance.getRowForGroupKey(groupByKey);

        if (bean != null) {
            AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        } else {
            currentAggregatorMethods = null;
        }

        this.currentGroupKey = groupByKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (column < currentAggregatorMethods.length) {
            return currentAggregatorMethods[column].getValue();
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getValue(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < currentAggregatorMethods.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getEnumerableEvents(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < currentAggregatorMethods.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getEnumerableScalar(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < currentAggregatorMethods.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getEnumerableEvent(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        // not applicable
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        // not applicable
    }

    public boolean isGrouped() {
        return true;
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return tableStateInstance.getGroupKeys();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        tableStateInstance.clear();
    }

    public void stop() {
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }
}
