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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.fafquery.FireAndForgetQueryExec;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.table.strategy.ExprTableEvalLockUtil;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class FireAndForgetInstanceTable extends FireAndForgetInstance {
    private final TableStateInstance instance;

    public FireAndForgetInstanceTable(TableStateInstance instance) {
        this.instance = instance;
    }

    public EventBean[] processInsert(EPPreparedExecuteIUDSingleStreamExecInsert insert) {
        ExprTableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().writeLock(), insert.getServices().getTableService().getTableExprEvaluatorContext());
        EventBean theEvent = insert.getInsertHelper().process(new EventBean[0], true, true, insert.getExprEvaluatorContext());
        AggregationRowPair aggs = instance.getTableMetadata().getRowFactory().makeAggs(insert.getExprEvaluatorContext().getAgentInstanceId(), null, null, instance.getAggregationServicePassThru());
        ((Object[]) theEvent.getUnderlying())[0] = aggs;
        instance.addEvent(theEvent);
        return CollectionUtil.EVENTBEANARRAY_EMPTY;
    }

    public EventBean[] processDelete(EPPreparedExecuteIUDSingleStreamExecDelete delete) {
        ExprTableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().writeLock(), delete.getServices().getTableService().getTableExprEvaluatorContext());

        if (delete.getOptionalWhereClause() == null) {
            instance.clearInstance();
            return CollectionUtil.EVENTBEANARRAY_EMPTY;
        }

        Collection<EventBean> found = snapshotAndApplyFilter(delete.getQueryGraph(), delete.getAnnotations(), delete.getOptionalWhereClause(), instance.getAgentInstanceContext());
        for (EventBean event : found) {
            instance.deleteEvent(event);
        }
        return CollectionUtil.EVENTBEANARRAY_EMPTY;
    }

    public EventBean[] processUpdate(EPPreparedExecuteIUDSingleStreamExecUpdate update) {
        ExprTableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().writeLock(), update.getServices().getTableService().getTableExprEvaluatorContext());
        Collection<EventBean> events = snapshotAndApplyFilter(update.getQueryGraph(), update.getAnnotations(), update.getOptionalWhereClause(), instance.getAgentInstanceContext());

        if (events != null && events.isEmpty()) {
            return CollectionUtil.EVENTBEANARRAY_EMPTY;
        }

        EventBean[] eventsPerStream = new EventBean[3];
        if (events == null) {
            update.getTableUpdateStrategy().updateTable(instance.getEventCollection(), instance, eventsPerStream, instance.getAgentInstanceContext());
        } else {
            update.getTableUpdateStrategy().updateTable(events, instance, eventsPerStream, instance.getAgentInstanceContext());
        }
        return CollectionUtil.EVENTBEANARRAY_EMPTY;
    }

    public Collection<EventBean> snapshotBestEffort(EPPreparedExecuteMethodQuery query, QueryGraph queryGraph, Annotation[] annotations) {
        ExprTableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().readLock(), query.getAgentInstanceContext());
        Collection<EventBean> events = snapshotNullWhenNoIndex(queryGraph, annotations, null, null);
        if (events != null) {
            return events;
        }
        return instance.getEventCollection();
    }

    private Collection<EventBean> snapshotAndApplyFilter(QueryGraph queryGraph, Annotation[] annotations, ExprNode filterExpr, AgentInstanceContext agentInstanceContext) {
        Collection<EventBean> indexedResult = snapshotNullWhenNoIndex(queryGraph, annotations, null, null);
        if (indexedResult != null) {
            if (indexedResult.isEmpty() || filterExpr == null) {
                return indexedResult;
            }
            ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(Math.min(indexedResult.size(), 16));
            ExprNodeUtilityCore.applyFilterExpressionIterable(indexedResult.iterator(), filterExpr.getForge().getExprEvaluator(), agentInstanceContext, deque);
            return deque;
        }

        // fall back to window operator if snapshot doesn't resolve successfully
        Collection<EventBean> sourceCollection = instance.getEventCollection();
        Iterator<EventBean> it = sourceCollection.iterator();
        if (!it.hasNext()) {
            return Collections.EMPTY_LIST;
        }
        ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(sourceCollection.size());
        if (filterExpr != null) {
            ExprNodeUtilityCore.applyFilterExpressionIterable(sourceCollection.iterator(), filterExpr.getForge().getExprEvaluator(), agentInstanceContext, deque);
        } else {
            while (it.hasNext()) {
                deque.add(it.next());
            }
        }
        return deque;
    }

    /**
     * Returns null when a filter cannot be applied, and a collection iterator must be used instead.
     * Returns best-effort matching events otherwise which should still be run through any filter expressions.
     */
    private Collection<EventBean> snapshotNullWhenNoIndex(QueryGraph queryGraph, Annotation[] annotations, ExprNode optionalWhereClause, AgentInstanceContext agentInstanceContext) {
        // return null when filter cannot be applies
        return FireAndForgetQueryExec.snapshot(queryGraph, annotations, null,
                instance.getIndexRepository(), instance.getTableMetadata().isQueryPlanLogging(),
                TableServiceImpl.getQueryPlanLog(), instance.getTableMetadata().getTableName(),
                instance.getAgentInstanceContext());
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return instance.getAgentInstanceContext();
    }

    public Viewable getTailViewInstance() {
        return null;
    }

    public VirtualDWView getVirtualDataWindow() {
        return null;
    }
}
