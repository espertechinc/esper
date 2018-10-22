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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowEventConsumerAdd;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowEventConsumerRemove;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.namedwindow.consume.*;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperWCopy;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This view is hooked into a named window's view chain as the last view and handles dispatching of named window
 * insert and remove stream results via {@link NamedWindowManagementService} to consuming statements.
 */
public class NamedWindowTailViewInstance extends ViewSupport implements Iterable<EventBean> {
    private final NamedWindowRootViewInstance rootViewInstance;
    private final NamedWindowTailView tailView;
    private final NamedWindow namedWindow;
    private final AgentInstanceContext agentInstanceContext;
    private final NamedWindowConsumerLatchFactory latchFactory;

    private volatile Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumersInContext;  // handles as copy-on-write
    private volatile long numberOfEvents;

    public NamedWindowTailViewInstance(NamedWindowRootViewInstance rootViewInstance, NamedWindowTailView tailView, NamedWindow namedWindow, AgentInstanceContext agentInstanceContext) {
        this.rootViewInstance = rootViewInstance;
        this.tailView = tailView;
        this.namedWindow = namedWindow;
        this.agentInstanceContext = agentInstanceContext;
        this.consumersInContext = NamedWindowUtil.createConsumerMap(tailView.isPrioritized());
        this.latchFactory = tailView.makeLatchFactory();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // Only old data (remove stream) needs to be removed from indexes (kept by root view), if any
        if (oldData != null) {
            rootViewInstance.removeOldData(oldData);
            numberOfEvents -= oldData.length;
        }

        if ((newData != null) && (!tailView.isParentBatchWindow())) {
            rootViewInstance.addNewData(newData);
        }

        if (newData != null) {
            numberOfEvents += newData.length;
        }

        // Post to child views, only if there are listeners or subscribers
        if (tailView.getStatementResultService().isMakeNatural() || tailView.getStatementResultService().isMakeSynthetic()) {
            child.update(newData, oldData);
        }

        NamedWindowDeltaData delta = new NamedWindowDeltaData(newData, oldData);
        tailView.addDispatches(latchFactory, consumersInContext, delta, agentInstanceContext);
    }

    public NamedWindowConsumerView addConsumer(NamedWindowConsumerDesc consumerDesc, boolean isSubselect) {
        NamedWindowConsumerCallback consumerCallback = new NamedWindowConsumerCallback() {
            public Iterator<EventBean> getIterator() {
                NamedWindowInstance instance = namedWindow.getNamedWindowInstance(agentInstanceContext);
                if (instance == null) {
                    // this can happen on context-partition "output when terminated"
                    return NamedWindowTailViewInstance.this.iterator();
                }
                return instance.getTailViewInstance().iterator();
            }

            public void stopped(NamedWindowConsumerView namedWindowConsumerView) {
                removeConsumer(namedWindowConsumerView);
            }

            public boolean isParentBatchWindow() {
                return rootViewInstance.isParentBatchWindow();
            }

            public Collection<EventBean> snapshot(QueryGraph queryGraph, Annotation[] annotations) {
                return NamedWindowTailViewInstance.this.snapshot(queryGraph, annotations);
            }
        };

        // Construct consumer view, allow a callback to this view to remove the consumer
        boolean audit = AuditEnum.STREAM.getAudit(consumerDesc.getAgentInstanceContext().getStatementContext().getAnnotations()) != null;
        NamedWindowConsumerView consumerView = new NamedWindowConsumerView(consumerDesc.getNamedWindowConsumerId(), consumerDesc.getFilterEvaluator(), consumerDesc.getOptPropertyEvaluator(), tailView.getEventType(), consumerCallback, consumerDesc.getAgentInstanceContext(), audit);

        // indicate to virtual data window that a consumer was added
        VirtualDWView virtualDWView = rootViewInstance.getVirtualDataWindow();
        if (virtualDWView != null) {
            virtualDWView.getVirtualDataWindow().handleEvent(
                    new VirtualDataWindowEventConsumerAdd(tailView.getEventType().getName(), consumerView, consumerDesc.getAgentInstanceContext().getStatementName(), consumerDesc.getAgentInstanceContext().getAgentInstanceId(), consumerDesc.getFilterEvaluator(), agentInstanceContext));
        }

        // Keep a list of consumer views per statement to accommodate joins and subqueries
        List<NamedWindowConsumerView> viewsPerStatements = consumersInContext.get(consumerDesc.getAgentInstanceContext().getEpStatementAgentInstanceHandle());
        if (viewsPerStatements == null) {
            viewsPerStatements = new CopyOnWriteArrayList<NamedWindowConsumerView>();

            // avoid concurrent modification as a thread may currently iterate over consumers as its dispatching
            // without the runtimelock
            Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> newConsumers = NamedWindowUtil.createConsumerMap(tailView.isPrioritized());
            newConsumers.putAll(consumersInContext);
            newConsumers.put(consumerDesc.getAgentInstanceContext().getEpStatementAgentInstanceHandle(), viewsPerStatements);
            consumersInContext = newConsumers;
        }
        if (isSubselect) {
            viewsPerStatements.add(0, consumerView);
        } else {
            viewsPerStatements.add(consumerView);
        }

        return consumerView;
    }

    /**
     * Called by the consumer view to indicate it was stopped or destroyed, such that the
     * consumer can be deregistered and further dispatches disregard this consumer.
     *
     * @param namedWindowConsumerView is the consumer representative view
     **/
    public void removeConsumer(NamedWindowConsumerView namedWindowConsumerView) {
        EPStatementAgentInstanceHandle handleRemoved = null;
        // Find the consumer view
        for (Map.Entry<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> entry : consumersInContext.entrySet()) {
            boolean foundAndRemoved = entry.getValue().remove(namedWindowConsumerView);
            // Remove the consumer view
            if (foundAndRemoved && (entry.getValue().size() == 0)) {
                // Remove the handle if this list is now empty
                handleRemoved = entry.getKey();
                break;
            }
        }
        if (handleRemoved != null) {
            Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> newConsumers = NamedWindowUtil.createConsumerMap(tailView.isPrioritized());
            newConsumers.putAll(consumersInContext);
            newConsumers.remove(handleRemoved);
            consumersInContext = newConsumers;
        }

        // indicate to virtual data window that a consumer was added
        VirtualDWView virtualDWView = rootViewInstance.getVirtualDataWindow();
        if (virtualDWView != null && handleRemoved != null) {
            virtualDWView.getVirtualDataWindow().handleEvent(new VirtualDataWindowEventConsumerRemove(tailView.getEventType().getName(), namedWindowConsumerView, handleRemoved.getStatementHandle().getStatementName(), handleRemoved.getAgentInstanceId()));
        }
    }

    public EventType getEventType() {
        return tailView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().acquireReadLock();
        try {
            Iterator<EventBean> it = parent.iterator();
            if (!it.hasNext()) {
                return CollectionUtil.NULL_EVENT_ITERATOR;
            }
            ArrayList<EventBean> list = new ArrayList<EventBean>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return new ArrayEventIterator(list.toArray(new EventBean[list.size()]));
        } finally {
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().releaseReadLock();
        }
    }

    public Collection<EventBean> snapshot(QueryGraph queryGraph, Annotation[] annotations) {
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().acquireReadLock();
        try {
            return snapshotNoLock(queryGraph, annotations);
        } finally {
            releaseTableLocks(agentInstanceContext);
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().releaseReadLock();
        }
    }

    public EventBean[] snapshotUpdate(QueryGraph filterQueryGraph, ExprEvaluator optionalWhereClause, EventBeanUpdateHelperWCopy updateHelper, Annotation[] annotations) {
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().acquireReadLock();
        try {
            Collection<EventBean> events = snapshotNoLockWithFilter(filterQueryGraph, annotations, optionalWhereClause, agentInstanceContext);
            if (events.isEmpty()) {
                return CollectionUtil.EVENTBEANARRAY_EMPTY;
            }

            EventBean[] eventsPerStream = new EventBean[3];
            EventBean[] updated = new EventBean[events.size()];
            int count = 0;
            for (EventBean event : events) {
                updated[count++] = updateHelper.updateWCopy(event, eventsPerStream, agentInstanceContext);
            }

            EventBean[] deleted = events.toArray(new EventBean[events.size()]);
            rootViewInstance.update(updated, deleted);
            return updated;
        } finally {
            releaseTableLocks(agentInstanceContext);
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().releaseReadLock();
        }
    }

    public EventBean[] snapshotDelete(QueryGraph filterQueryGraph, ExprEvaluator filterExpr, Annotation[] annotations) {
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().acquireReadLock();
        try {
            Collection<EventBean> events = snapshotNoLockWithFilter(filterQueryGraph, annotations, filterExpr, agentInstanceContext);
            if (events.isEmpty()) {
                return CollectionUtil.EVENTBEANARRAY_EMPTY;
            }
            EventBean[] eventsDeleted = events.toArray(new EventBean[events.size()]);
            rootViewInstance.update(null, eventsDeleted);
            return eventsDeleted;
        } finally {
            releaseTableLocks(agentInstanceContext);
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock().releaseReadLock();
        }
    }

    public Collection<EventBean> snapshotNoLock(QueryGraph queryGraph, Annotation[] annotations) {
        Collection<EventBean> indexedResult = rootViewInstance.snapshot(queryGraph, annotations);
        if (indexedResult != null) {
            return indexedResult;
        }
        Iterator<EventBean> it = parent.iterator();
        if (!it.hasNext()) {
            return Collections.EMPTY_LIST;
        }
        ArrayDeque<EventBean> list = new ArrayDeque<EventBean>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    public Collection<EventBean> snapshotNoLockWithFilter(QueryGraph filterQueryGraph, Annotation[] annotations, ExprEvaluator filterExpr, ExprEvaluatorContext exprEvaluatorContext) {
        Collection<EventBean> indexedResult = rootViewInstance.snapshot(filterQueryGraph, annotations);
        if (indexedResult != null) {
            if (indexedResult.isEmpty()) {
                return indexedResult;
            }
            if (filterExpr == null) {
                return indexedResult;
            }
            ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(Math.min(indexedResult.size(), 16));
            ExprNodeUtilityEvaluate.applyFilterExpressionIterable(indexedResult.iterator(), filterExpr, exprEvaluatorContext, deque);
            return deque;
        }

        // fall back to window operator if snapshot doesn't resolve successfully
        Iterator<EventBean> it = parent.iterator();
        if (!it.hasNext()) {
            return Collections.EMPTY_LIST;
        }
        ArrayDeque<EventBean> list = new ArrayDeque<EventBean>();
        if (filterExpr != null) {
            ExprNodeUtilityEvaluate.applyFilterExpressionIterable(it, filterExpr, agentInstanceContext, list);
        } else {
            while (it.hasNext()) {
                list.add(it.next());
            }
        }
        return list;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    /**
     * Destroy the view.
     */
    public void destroy() {
        consumersInContext = NamedWindowUtil.createConsumerMap(tailView.isPrioritized());
    }

    /**
     * Returns the number of events held.
     *
     * @return number of events
     */
    public long getNumberOfEvents() {
        return numberOfEvents;
    }

    public NamedWindowTailView getTailView() {
        return tailView;
    }

    private void releaseTableLocks(AgentInstanceContext agentInstanceContext) {
        agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
    }

    public void stop() {
        // no action
    }
}
