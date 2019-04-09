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
import com.espertech.esper.common.internal.epl.table.core.TableInstanceUngrouped;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.Collection;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllWTableImpl implements AggregationService, AggregationServiceTable {
    private final TableInstanceUngrouped tableInstance;
    private final TableColumnMethodPairEval[] methodPairs;
    private final AggregationMultiFunctionAgent[] accessAgents;
    private final int[] accessColumnsZeroOffset;

    public AggSvcGroupAllWTableImpl(TableInstanceUngrouped tableInstance, TableColumnMethodPairEval[] methodPairs, AggregationMultiFunctionAgent[] accessAgents, int[] accessColumnsZeroOffset) {
        this.tableInstance = tableInstance;
        this.methodPairs = methodPairs;
        this.accessAgents = accessAgents;
        this.accessColumnsZeroOffset = accessColumnsZeroOffset;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableInstance.getCreateRowIntoTable(exprEvaluatorContext);
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(event);

        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPairEval methodPair = methodPairs[i];
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            row.enterAgg(methodPair.getColumn(), columnResult);
        }

        for (int i = 0; i < accessAgents.length; i++) {
            accessAgents[i].applyEnter(eventsPerStream, exprEvaluatorContext, row, accessColumnsZeroOffset[i]);
        }

        tableInstance.handleRowUpdated(event);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableInstance.getCreateRowIntoTable(exprEvaluatorContext);
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(event);

        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPairEval methodPair = methodPairs[i];
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, false, exprEvaluatorContext);
            row.leaveAgg(methodPair.getColumn(), columnResult);
        }

        for (int i = 0; i < accessAgents.length; i++) {
            accessAgents[i].applyLeave(eventsPerStream, exprEvaluatorContext, row, accessColumnsZeroOffset[i]);
        }

        tableInstance.handleRowUpdated(event);
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        // no action needed - this implementation does not group and the current row is the single group
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(event);
        return row.getValue(column, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(event);
        return row.getCollectionOfEvents(column, eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(event);
        return row.getCollectionScalar(column, eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(event);
        return row.getEventBean(column, eventsPerStream, isNewData, context);
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        // clear not required
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        // not applicable
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

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }

    public TableInstance getTableInstance() {
        return tableInstance;
    }

    public AggregationRow getAggregationRow(int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }
}