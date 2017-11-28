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
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.GroupableView;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewDataVisitorContained;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The group view splits the data in a stream to multiple subviews, based on a key index.
 * The key is one or more fields in the stream. Any view that follows the GROUP view will be executed
 * separately on each subview, one per unique key.
 * <p>
 * The view takes a single parameter which is the field name returning the key value to group.
 * <p>
 * This view can, for example, be used to calculate the average price per symbol for a list of symbols.
 * <p>
 * The view treats its child views and their child views as prototypes. It dynamically instantiates copies
 * of each child view and their child views, and the child view's child views as so on. When there are
 * no more child views or the special merge view is encountered, it ends. The view installs a special merge
 * view unto each leaf child view that merges the value key that was grouped by back into the stream
 * using the group-by field name.
 */
public class GroupByViewImpl extends ViewSupport implements GroupByView {
    public final static String VIEWNAME = "Group-By";

    private final GroupByViewFactory groupByViewFactory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private EventBean[] eventsPerStream = new EventBean[1];

    protected final Map<Object, View> subViewPerKey = new HashMap<>();
    private final HashMap<View, Pair<Object, Object>> groupedEvents = new HashMap<>();

    /**
     * Constructor.
     *
     * @param groupByViewFactory view factory
     * @param agentInstanceContext contains required view services
     */
    public GroupByViewImpl(GroupByViewFactory groupByViewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.groupByViewFactory = groupByViewFactory;
        this.agentInstanceContext = agentInstanceContext;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public GroupByViewFactory getViewFactory() {
        return groupByViewFactory;
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, "Grouped", newData, oldData);
        }

        // Algorithm for single new event
        if ((newData != null) && (oldData == null) && (newData.length == 1)) {
            EventBean theEvent = newData[0];
            EventBean[] newDataToPost = new EventBean[]{theEvent};

            Object groupByValuesKey = getGroupKey(theEvent);

            // Get child views that belong to this group-by value combination
            View subView = this.subViewPerKey.get(groupByValuesKey);

            // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
            if (subView == null) {
                subView = GroupByViewUtil.makeSubView(this, groupByValuesKey, agentInstanceContext);
                subViewPerKey.put(groupByValuesKey, subView);
            }

            subView.update(newDataToPost, null);
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
            for (Map.Entry<View, Pair<Object, Object>> entry : groupedEvents.entrySet()) {
                EventBean[] newEvents = convertToArray(entry.getValue().getFirst());
                EventBean[] oldEvents = convertToArray(entry.getValue().getSecond());
                entry.getKey().update(newEvents, oldEvents);
            }

            groupedEvents.clear();
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException("Cannot iterate over group view, this operation is not supported");
    }

    public final String toString() {
        return this.getClass().getName() + " groupFieldNames=" + Arrays.toString(groupByViewFactory.getPropertyNames());
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        viewDataVisitor.visitPrimary(VIEWNAME, subViewPerKey.size());
        for (Map.Entry<Object, View> entry : subViewPerKey.entrySet()) {
            GroupByViewImpl.visitView(viewDataVisitor, entry.getKey(), entry.getValue());
        }
    }

    public static void visitView(ViewDataVisitorContained viewDataVisitor, Object groupkey, View view) {
        if (view == null) {
            return;
        }
        viewDataVisitor.visitContained(groupkey, view);
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
        for (Map.Entry<Object, View> entry : subViewPerKey.entrySet()) {
            Object value = entry.getValue();
            GroupableView subview = (GroupableView) value;
            if (compareViews(subview, removedView)) {
                if (removedKeys == null) {
                    removedKeys = new ArrayDeque<>();
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

    private void handleEvent(EventBean theEvent, boolean isNew) {
        Object groupByValuesKey = getGroupKey(theEvent);

        // Get child views that belong to this group-by value combination
        View subView = this.subViewPerKey.get(groupByValuesKey);

        // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
        if (subView == null) {
            subView = GroupByViewUtil.makeSubView(this, groupByValuesKey, agentInstanceContext);
            subViewPerKey.put(groupByValuesKey, subView);
        }

        // Construct a pair of lists to hold the events for the grouped value if not already there
        Pair<Object, Object> pair = groupedEvents.get(subView);
        if (pair == null) {
            pair = new Pair<>(null, null);
            groupedEvents.put(subView, pair);
        }

        // Add event to a child view event list for later child update that includes new and old events
        if (isNew) {
            pair.setFirst(addUpgradeToDequeIfPopulated(pair.getFirst(), theEvent));
        } else {
            pair.setSecond(addUpgradeToDequeIfPopulated(pair.getSecond(), theEvent));
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

    protected static Object addUpgradeToDequeIfPopulated(Object holder, EventBean theEvent) {
        if (holder == null) {
            return theEvent;
        } else if (holder instanceof Deque) {
            Deque<EventBean> deque = (Deque<EventBean>) holder;
            deque.add(theEvent);
            return deque;
        } else {
            ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(4);
            deque.add((EventBean) holder);
            deque.add(theEvent);
            return deque;
        }
    }

    protected static EventBean[] convertToArray(Object eventOrDeque) {
        if (eventOrDeque == null) {
            return null;
        }
        if (eventOrDeque instanceof EventBean) {
            return new EventBean[]{(EventBean) eventOrDeque};
        }
        return EventBeanUtility.toArray((ArrayDeque<EventBean>) eventOrDeque);
    }

    private static final Logger log = LoggerFactory.getLogger(GroupByViewImpl.class);
}
