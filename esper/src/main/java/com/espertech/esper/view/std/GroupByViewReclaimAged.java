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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GroupByViewReclaimAged extends ViewSupport implements GroupByView {
    protected final GroupByViewFactory groupByViewFactory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;

    private EventBean[] eventsPerStream = new EventBean[1];

    protected final Map<Object, GroupByViewAgedEntry> subViewPerKey = new HashMap<Object, GroupByViewAgedEntry>();
    private final HashMap<GroupByViewAgedEntry, Pair<Object, Object>> groupedEvents = new HashMap<GroupByViewAgedEntry, Pair<Object, Object>>();
    private Long nextSweepTime = null;

    public GroupByViewReclaimAged(GroupByViewFactory groupByViewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.groupByViewFactory = groupByViewFactory;
        this.agentInstanceContext = agentInstanceContext;
    }

    /**
     * Returns the field name that provides the key valie by which to group by.
     *
     * @return field name providing group-by key.
     */
    public ExprNode[] getCriteriaExpressions() {
        return groupByViewFactory.getCriteriaExpressions();
    }

    public GroupByViewFactory getViewFactory() {
        return groupByViewFactory;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        long currentTime = agentInstanceContext.getTimeProvider().getTime();
        if ((nextSweepTime == null) || (nextSweepTime <= currentTime)) {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Reclaiming groups older then " + groupByViewFactory.getReclaimMaxAge() + " msec and every " + groupByViewFactory.getReclaimFrequency() + "msec in frequency");
            }
            nextSweepTime = currentTime + groupByViewFactory.getReclaimFrequency();
            sweep(currentTime);
        }

        // Algorithm for single new event
        if ((newData != null) && (oldData == null) && (newData.length == 1)) {
            EventBean theEvent = newData[0];
            EventBean[] newDataToPost = new EventBean[]{theEvent};

            Object groupByValuesKey = getGroupKey(theEvent);

            // Get child views that belong to this group-by value combination
            GroupByViewAgedEntry subView = this.subViewPerKey.get(groupByValuesKey);

            // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
            if (subView == null) {
                View subview = GroupByViewUtil.makeSubView(this, groupByValuesKey, agentInstanceContext);
                subView = new GroupByViewAgedEntry(subview, currentTime);
                subViewPerKey.put(groupByValuesKey, subView);
            } else {
                subView.setLastUpdateTime(currentTime);
            }

            subView.getSubview().update(newData, null);
        } else {

            // Algorithm for dispatching multiple events
            if (newData != null) {
                for (EventBean newValue : newData) {
                    handleEvent(newValue, true);
                }
            }

            if (oldData != null) {
                for (EventBean oldValue : oldData) {
                    handleEvent(oldValue, false);
                }
            }

            // Update child views
            for (Map.Entry<GroupByViewAgedEntry, Pair<Object, Object>> entry : groupedEvents.entrySet()) {
                EventBean[] newEvents = GroupByViewImpl.convertToArray(entry.getValue().getFirst());
                EventBean[] oldEvents = GroupByViewImpl.convertToArray(entry.getValue().getSecond());
                entry.getKey().getSubview().update(newEvents, oldEvents);
            }

            groupedEvents.clear();
        }
    }

    private void handleEvent(EventBean theEvent, boolean isNew) {
        Object groupByValuesKey = getGroupKey(theEvent);

        // Get child views that belong to this group-by value combination
        GroupByViewAgedEntry subViews = this.subViewPerKey.get(groupByValuesKey);

        // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
        if (subViews == null) {
            View subview = GroupByViewUtil.makeSubView(this, groupByValuesKey, agentInstanceContext);
            long currentTime = agentInstanceContext.getStatementContext().getTimeProvider().getTime();
            subViews = new GroupByViewAgedEntry(subview, currentTime);
            subViewPerKey.put(groupByValuesKey, subViews);
        } else {
            subViews.setLastUpdateTime(agentInstanceContext.getStatementContext().getTimeProvider().getTime());
        }

        // Construct a pair of lists to hold the events for the grouped value if not already there
        Pair<Object, Object> pair = groupedEvents.get(subViews);
        if (pair == null) {
            pair = new Pair<Object, Object>(null, null);
            groupedEvents.put(subViews, pair);
        }

        // Add event to a child view event list for later child update that includes new and old events
        if (isNew) {
            pair.setFirst(GroupByViewImpl.addUpgradeToDequeIfPopulated(pair.getFirst(), theEvent));
        } else {
            pair.setSecond(GroupByViewImpl.addUpgradeToDequeIfPopulated(pair.getSecond(), theEvent));
        }
    }

    public final Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException("Cannot iterate over group view, this operation is not supported");
    }

    public final String toString() {
        return this.getClass().getName() + " groupFieldNames=" + Arrays.toString(groupByViewFactory.getPropertyNames());
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        viewDataVisitor.visitPrimary(GroupByViewImpl.VIEWNAME, subViewPerKey.size());
        for (Map.Entry<Object, GroupByViewAgedEntry> entry : subViewPerKey.entrySet()) {
            GroupByViewImpl.visitView(viewDataVisitor, entry.getKey(), entry.getValue().getSubview());
        }
    }

    private void sweep(long currentTime) {
        ArrayDeque<Object> removed = new ArrayDeque<Object>();
        for (Map.Entry<Object, GroupByViewAgedEntry> entry : subViewPerKey.entrySet()) {
            long age = currentTime - entry.getValue().getLastUpdateTime();
            if (age > groupByViewFactory.getReclaimMaxAge()) {
                removed.add(entry.getKey());
            }
        }

        for (Object key : removed) {
            GroupByViewAgedEntry entry = subViewPerKey.remove(key);
            removeSubview(entry.getSubview());
        }
    }

    @Override
    public boolean removeView(View view) {
        if (!(view instanceof GroupableView)) {
            super.removeView(view);
        }
        boolean removed = super.removeView(view);
        if (!removed) {
            return false;
        }
        if (!hasViews()) {
            subViewPerKey.clear();
            return true;
        }
        GroupableView removedView = (GroupableView) view;
        Deque<Object> removedKeys = null;
        for (Map.Entry<Object, GroupByViewAgedEntry> entry : subViewPerKey.entrySet()) {
            View value = entry.getValue().getSubview();
            GroupableView subview = (GroupableView) value;
            if (compareViews(subview, removedView)) {
                if (removedKeys == null) {
                    removedKeys = new ArrayDeque<Object>();
                }
                removedKeys.add(entry.getKey());
            }
        }
        if (removedKeys != null) {
            for (Object key : removedKeys) {
                subViewPerKey.remove(key);
            }
        }
        return true;
    }

    private boolean compareViews(GroupableView subview, GroupableView removed) {
        return subview.getViewFactory() == removed.getViewFactory();
    }

    private void removeSubview(View view) {
        view.setParent(null);
        recursiveMergeViewRemove(view);
        if (view instanceof StoppableView) {
            ((StoppableView) view).stop();
        }
    }

    private void recursiveMergeViewRemove(View view) {
        for (View child : view.getViews()) {
            if (child instanceof MergeView) {
                MergeView mergeView = (MergeView) child;
                mergeView.removeParentView(view);
            } else {
                if (child instanceof StoppableView) {
                    ((StoppableView) child).stop();
                }
                if (child.getViews().length > 0) {
                    recursiveMergeViewRemove(child);
                }
            }
        }
    }

    private Object getGroupKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        ExprEvaluator[] criteriaEvaluators = groupByViewFactory.getCriteriaExpressionEvals();
        if (criteriaEvaluators.length == 1) {
            return criteriaEvaluators[0].evaluate(eventsPerStream, true, agentInstanceContext);
        }

        Object[] values = new Object[criteriaEvaluators.length];
        for (int i = 0; i < criteriaEvaluators.length; i++) {
            values[i] = criteriaEvaluators[i].evaluate(eventsPerStream, true, agentInstanceContext);
        }
        return new MultiKeyUntyped(values);
    }

    private static final Logger log = LoggerFactory.getLogger(GroupByViewReclaimAged.class);
}
