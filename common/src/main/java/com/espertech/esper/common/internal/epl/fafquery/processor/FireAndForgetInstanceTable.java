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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDDelete;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDInsertInto;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDUpdate;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.table.core.TableEvalLockUtil;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class FireAndForgetInstanceTable extends FireAndForgetInstance {
    private final TableInstance instance;

    public FireAndForgetInstanceTable(TableInstance instance) {
        this.instance = instance;
    }

    public EventBean[] processInsert(FAFQueryMethodIUDInsertInto insert) {
        TableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().writeLock(), instance.getAgentInstanceContext().getTableExprEvaluatorContext());
        EventBean theEvent = insert.getInsertHelper().process(new EventBean[0], true, true, instance.getAgentInstanceContext());
        AggregationRow aggs = instance.getTable().getAggregationRowFactory().make();
        ((Object[]) theEvent.getUnderlying())[0] = aggs;
        instance.addEvent(theEvent);
        return CollectionUtil.EVENTBEANARRAY_EMPTY;
    }

    public EventBean[] processDelete(FAFQueryMethodIUDDelete delete) {
        TableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().writeLock(), instance.getAgentInstanceContext().getTableExprEvaluatorContext());

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

    public EventBean[] processUpdate(FAFQueryMethodIUDUpdate update) {
        TableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().writeLock(), instance.getAgentInstanceContext().getTableExprEvaluatorContext());
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

    public Collection<EventBean> snapshotBestEffort(QueryGraph queryGraph, Annotation[] annotations) {
        TableEvalLockUtil.obtainLockUnless(instance.getTableLevelRWLock().readLock(), instance.getAgentInstanceContext());
        Collection<EventBean> events = snapshotNullWhenNoIndex(queryGraph, annotations, null, null);
        if (events != null) {
            return events;
        }
        return instance.getEventCollection();
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return instance.getAgentInstanceContext();
    }

    public Viewable getTailViewInstance() {
        return null;
    }

    /**
     * Returns null when a filter cannot be applied, and a collection iterator must be used instead.
     * Returns best-effort matching events otherwise which should still be run through any filter expressions.
     */
    private Collection<EventBean> snapshotNullWhenNoIndex(QueryGraph queryGraph, Annotation[] annotations, ExprNode optionalWhereClause, AgentInstanceContext agentInstanceContext) {
        // return null when filter cannot be applies
        return FireAndForgetQueryExec.snapshot(queryGraph, annotations, null,
                instance.getIndexRepository(), instance.getTable().getName(),
                instance.getAgentInstanceContext());
    }

    private Collection<EventBean> snapshotAndApplyFilter(QueryGraph queryGraph, Annotation[] annotations, ExprEvaluator filterExpr, AgentInstanceContext agentInstanceContext) {
        Collection<EventBean> indexedResult = snapshotNullWhenNoIndex(queryGraph, annotations, null, null);
        if (indexedResult != null) {
            if (indexedResult.isEmpty() || filterExpr == null) {
                return indexedResult;
            }
            ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(Math.min(indexedResult.size(), 16));
            ExprNodeUtilityEvaluate.applyFilterExpressionIterable(indexedResult.iterator(), filterExpr, agentInstanceContext, deque);
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
            ExprNodeUtilityEvaluate.applyFilterExpressionIterable(sourceCollection.iterator(), filterExpr, agentInstanceContext, deque);
        } else {
            while (it.hasNext()) {
                deque.add(it.next());
            }
        }
        return deque;
    }
}
