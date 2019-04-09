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
package com.espertech.esper.common.internal.view.groupwin;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewDataVisitorContained;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GroupByViewReclaimAged extends ViewSupport implements GroupByView, AgentInstanceStopCallback {
    private final GroupByViewFactory groupByViewFactory;
    private final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private final MergeView mergeView;
    private EventBean[] eventsPerStream = new EventBean[1];

    protected final Map<Object, GroupByViewAgedEntry> subViewPerKey = new HashMap<>();
    private final HashMap<GroupByViewAgedEntry, Pair<Object, Object>> groupedEvents = new HashMap<GroupByViewAgedEntry, Pair<Object, Object>>();
    private Long nextSweepTime = null;

    public GroupByViewReclaimAged(GroupByViewFactory groupByViewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.groupByViewFactory = groupByViewFactory;
        this.agentInstanceContext = agentInstanceContext;
        this.mergeView = new MergeView(this, groupByViewFactory.eventType);
    }

    public GroupByViewFactory getViewFactory() {
        return groupByViewFactory;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        AgentInstanceContext aiContext = agentInstanceContext.getAgentInstanceContext();
        aiContext.getAuditProvider().view(newData, oldData, aiContext, groupByViewFactory);
        aiContext.getInstrumentationProvider().qViewProcessIRStream(groupByViewFactory, newData, oldData);

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
            Object groupByValuesKey = getGroupKey(theEvent);

            // Get child views that belong to this group-by value combination
            GroupByViewAgedEntry subView = this.subViewPerKey.get(groupByValuesKey);

            // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
            if (subView == null) {
                View subview = GroupByViewUtil.makeSubView(this, groupByValuesKey);
                subView = new GroupByViewAgedEntry(subview, currentTime);
                subViewPerKey.put(groupByValuesKey, subView);
            } else {
                subView.setLastUpdateTime(currentTime);
            }

            agentInstanceContext.getInstrumentationProvider().qViewIndicate(groupByViewFactory, newData, null);
            subView.getSubview().update(newData, null);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
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
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(groupByViewFactory, newEvents, oldEvents);
                entry.getKey().getSubview().update(newEvents, oldEvents);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }

            groupedEvents.clear();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    private void handleEvent(EventBean theEvent, boolean isNew) {
        Object groupByValuesKey = getGroupKey(theEvent);

        // Get child views that belong to this group-by value combination
        GroupByViewAgedEntry subViews = this.subViewPerKey.get(groupByValuesKey);

        // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
        if (subViews == null) {
            View subview = GroupByViewUtil.makeSubView(this, groupByValuesKey);
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
        return mergeView.iterator();
    }

    public final String toString() {
        return this.getClass().getName() + " groupFieldNames=" + Arrays.toString(groupByViewFactory.getPropertyNames());
    }

    public AgentInstanceViewFactoryChainContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public MergeView getMergeView() {
        return mergeView;
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        viewDataVisitor.visitPrimary(groupByViewFactory.getViewName(), subViewPerKey.size());
        for (Map.Entry<Object, GroupByViewAgedEntry> entry : subViewPerKey.entrySet()) {
            GroupByViewImpl.visitView(viewDataVisitor, entry.getKey(), entry.getValue().getSubview());
        }
    }

    public void stop(AgentInstanceStopServices services) {
        for (Map.Entry<Object, GroupByViewAgedEntry> entry : subViewPerKey.entrySet()) {
            GroupByViewUtil.removeSubview(entry.getValue().getSubview(), services);
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
            GroupByViewUtil.removeSubview(entry.getSubview(), new AgentInstanceStopServices(agentInstanceContext.getAgentInstanceContext()));
        }
    }

    private Object getGroupKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        return groupByViewFactory.getCriteriaEval().evaluate(eventsPerStream, true, agentInstanceContext);
    }

    private static final Logger log = LoggerFactory.getLogger(GroupByViewReclaimAged.class);
}
