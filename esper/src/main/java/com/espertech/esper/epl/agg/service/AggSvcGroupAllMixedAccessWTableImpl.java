/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceUngrouped;
import com.espertech.esper.epl.table.strategy.ExprTableEvalLockUtil;
import com.espertech.esper.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllMixedAccessWTableImpl implements AggregationService
{
    private final TableStateInstanceUngrouped tableStateInstance;
    private final TableColumnMethodPair[] methodPairs;
    private final AggregationAccessorSlotPair[] accessors;
    private final int[] targetStates;
    private final ExprNode[] accessStateExpr;
    private final AggregationAgent[] agents;

    public AggSvcGroupAllMixedAccessWTableImpl(TableStateInstanceUngrouped tableStateInstance, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        this.tableStateInstance = tableStateInstance;
        this.methodPairs = methodPairs;
        this.accessors = accessors;
        this.targetStates = targetStates;
        this.accessStateExpr = accessStateExpr;
        this.agents = agents;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext)
    {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getCreateRowIntoTable(null, exprEvaluatorContext);
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(true, row.getMethods().length, row.getStates().length);}
        for (int i = 0; i < methodPairs.length; i++)
        {
            TableColumnMethodPair methodPair = methodPairs[i];
            AggregationMethod method = row.getMethods()[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, method, methodPair.getAggregationNode());}
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            method.enter(columnResult);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, method);}
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = row.getStates()[targetStates[i]];
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggAccessEnterLeave(true, i, state, accessStateExpr[i]);}
            agents[i].applyEnter(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggAccessEnterLeave(true, i, state);}
        }

        tableStateInstance.handleRowUpdated(event);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(true);}
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext)
    {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getCreateRowIntoTable(null, exprEvaluatorContext);
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(false, row.getMethods().length, row.getStates().length);}

        for (int i = 0; i < methodPairs.length; i++)
        {
            TableColumnMethodPair methodPair = methodPairs[i];
            AggregationMethod method = row.getMethods()[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, method, methodPair.getAggregationNode());}
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, false, exprEvaluatorContext);
            method.leave(columnResult);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, method);}
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = row.getStates()[targetStates[i]];
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggAccessEnterLeave(false, i, state, accessStateExpr[i]);}
            agents[i].applyLeave(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggAccessEnterLeave(false, i, state);}
        }

        tableStateInstance.handleRowUpdated(event);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(false);}
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel)
    {
        // no action needed - this implementation does not group and the current row is the single group
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext)
    {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();
        if (column < aggregators.length) {
            return aggregators[column].getValue();
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getValue(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();
        if (column < aggregators.length) {
            return null;
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvents(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();
        if (column < aggregators.length) {
            return null;
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableScalar(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();

        if (column < aggregators.length) {
            return null;
        }
        else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvent(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext)
    {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();

        for (AggregationState state : row.getStates()) {
            state.clear();
        }
        for (AggregationMethod aggregator : aggregators) {
            aggregator.clear();
        }
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
}