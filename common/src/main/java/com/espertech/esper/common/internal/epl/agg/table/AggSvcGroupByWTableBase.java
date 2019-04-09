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
package com.espertech.esper.common.internal.epl.agg.table;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgent;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableColumnMethodPairEval;
import com.espertech.esper.common.internal.epl.table.core.TableEvalLockUtil;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.core.TableInstanceGrouped;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Collection;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public abstract class AggSvcGroupByWTableBase implements AggregationService, AggregationServiceTable {
    protected final TableInstanceGrouped tableInstance;
    protected final TableColumnMethodPairEval[] methodPairs;
    private final AggregationMultiFunctionAgent[] accessAgents;
    private final int[] accessColumnsZeroOffset;

    protected AggregationRow currentAggregationRow;
    protected Object currentGroupKey;

    public AggSvcGroupByWTableBase(TableInstanceGrouped tableInstance, TableColumnMethodPairEval[] methodPairs, AggregationMultiFunctionAgent[] accessAgents, int[] accessColumnsZeroOffset) {
        this.tableInstance = tableInstance;
        this.methodPairs = methodPairs;
        this.accessAgents = accessAgents;
        this.accessColumnsZeroOffset = accessColumnsZeroOffset;
    }

    public abstract void applyEnterInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public abstract void applyLeaveInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire tableInstance-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
        applyEnterInternal(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire tableInstance-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
        applyLeaveInternal(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    void applyEnterGroupKey(EventBean[] eventsPerStream, Object groupByKeyUntransformed, ExprEvaluatorContext exprEvaluatorContext) {
        Object groupByKey = tableInstance.getTable().getPrimaryKeyIntoTableTransform().from(groupByKeyUntransformed);
        applyEnterTableKey(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    void applyLeaveGroupKey(EventBean[] eventsPerStream, Object groupByKeyUntransformed, ExprEvaluatorContext exprEvaluatorContext) {
        Object groupByKey = tableInstance.getTable().getPrimaryKeyIntoTableTransform().from(groupByKeyUntransformed);
        applyLeaveTableKey(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    public void setCurrentAccess(Object groupByKeyUntransformed, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        Object groupByKey = tableInstance.getTable().getPrimaryKeyIntoTableTransform().from(groupByKeyUntransformed);
        ObjectArrayBackedEventBean bean = tableInstance.getRowForGroupKey(groupByKey);
        if (bean != null) {
            currentAggregationRow = (AggregationRow) bean.getProperties()[0];
        } else {
            currentAggregationRow = null;
        }
        this.currentGroupKey = groupByKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return currentAggregationRow.getValue(column, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return currentAggregationRow.getCollectionOfEvents(column, eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return currentAggregationRow.getCollectionScalar(column, eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return currentAggregationRow.getEventBean(column, eventsPerStream, isNewData, context);
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
        return tableInstance.getGroupKeys();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        // clear not required
    }

    public void stop() {
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }

    public TableInstance getTableInstance() {
        return tableInstance;
    }

    public AggregationRow getAggregationRow(int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    protected void applyEnterTableKey(EventBean[] eventsPerStream, Object tableKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = tableInstance.getCreateRowIntoTable(tableKey, exprEvaluatorContext);
        currentAggregationRow = (AggregationRow) bean.getProperties()[0];

        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qAggregationGroupedApplyEnterLeave(true, methodPairs.length, accessAgents.length, tableKey);

        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPairEval methodPair = methodPairs[i];
            instrumentationCommon.qAggNoAccessEnterLeave(true, i, null, null);
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            currentAggregationRow.enterAgg(methodPair.getColumn(), columnResult);
            instrumentationCommon.aAggNoAccessEnterLeave(true, i, null);
        }

        for (int i = 0; i < accessAgents.length; i++) {
            instrumentationCommon.qAggAccessEnterLeave(true, i, null);
            accessAgents[i].applyEnter(eventsPerStream, exprEvaluatorContext, currentAggregationRow, accessColumnsZeroOffset[i]);
            instrumentationCommon.aAggAccessEnterLeave(true, i);
        }

        tableInstance.handleRowUpdated(bean);

        instrumentationCommon.aAggregationGroupedApplyEnterLeave(true);
    }

    protected void applyLeaveTableKey(EventBean[] eventsPerStream, Object tableKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = tableInstance.getCreateRowIntoTable(tableKey, exprEvaluatorContext);
        currentAggregationRow = (AggregationRow) bean.getProperties()[0];

        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qAggregationGroupedApplyEnterLeave(false, methodPairs.length, accessAgents.length, tableKey);

        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPairEval methodPair = methodPairs[i];
            instrumentationCommon.qAggNoAccessEnterLeave(false, i, null, null);
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, false, exprEvaluatorContext);
            currentAggregationRow.leaveAgg(methodPair.getColumn(), columnResult);
            instrumentationCommon.aAggNoAccessEnterLeave(false, i, null);
        }

        for (int i = 0; i < accessAgents.length; i++) {
            instrumentationCommon.qAggAccessEnterLeave(false, i, null);
            accessAgents[i].applyLeave(eventsPerStream, exprEvaluatorContext, currentAggregationRow, accessColumnsZeroOffset[i]);
            instrumentationCommon.aAggAccessEnterLeave(false, i);
        }

        tableInstance.handleRowUpdated(bean);

        instrumentationCommon.aAggregationGroupedApplyEnterLeave(false);
    }
}
