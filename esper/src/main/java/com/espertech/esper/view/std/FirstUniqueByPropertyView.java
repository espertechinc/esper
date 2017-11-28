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
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This view retains the first event for each multi-key of distinct property values.
 * <p>
 * The view does not post a remove stream unless explicitly deleted from.
 * <p>
 * The view swallows any insert stream events that provide no new distinct set of property values.
 */
public class FirstUniqueByPropertyView extends ViewSupport implements DataWindowView {
    private final FirstUniqueByPropertyViewFactory viewFactory;
    private EventBean[] eventsPerStream = new EventBean[1];
    protected final Map<Object, EventBean> firstEvents = new HashMap<Object, EventBean>();
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;

    public FirstUniqueByPropertyView(FirstUniqueByPropertyViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
    }

    /**
     * Returns the expressions supplying the unique value to keep the most recent record for.
     *
     * @return expressions for unique value
     */
    public final ExprNode[] getUniqueCriteria() {
        return viewFactory.criteriaExpressions;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, FirstUniqueByPropertyViewFactory.NAME, newData, oldData);
        }

        EventBean[] newDataToPost = null;
        EventBean[] oldDataToPost = null;

        if (oldData != null) {
            for (EventBean oldEvent : oldData) {
                // Obtain unique value
                Object key = getUniqueKey(oldEvent);

                // If the old event is the current unique event, remove and post as old data
                EventBean lastValue = firstEvents.get(key);

                if (lastValue != oldEvent) {
                    continue;
                }

                if (oldDataToPost == null) {
                    oldDataToPost = new EventBean[]{oldEvent};
                } else {
                    oldDataToPost = EventBeanUtility.addToArray(oldDataToPost, oldEvent);
                }

                firstEvents.remove(key);
                internalHandleRemoved(key, lastValue);
            }
        }

        if (newData != null) {
            for (EventBean newEvent : newData) {
                // Obtain unique value
                Object key = getUniqueKey(newEvent);

                // already-seen key
                if (firstEvents.containsKey(key)) {
                    continue;
                }

                // store
                firstEvents.put(key, newEvent);
                internalHandleAdded(key, newEvent);

                // Post the new value
                if (newDataToPost == null) {
                    newDataToPost = new EventBean[]{newEvent};
                } else {
                    newDataToPost = EventBeanUtility.addToArray(newDataToPost, newEvent);
                }
            }
        }

        if ((this.hasViews()) && ((newDataToPost != null) || (oldDataToPost != null))) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, FirstUniqueByPropertyViewFactory.NAME, newDataToPost, oldDataToPost);
            }
            updateChildren(newDataToPost, oldDataToPost);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public void internalHandleRemoved(Object key, EventBean lastValue) {
        // no action required
    }

    public void internalHandleAdded(Object key, EventBean newEvent) {
        // no action required
    }

    public final Iterator<EventBean> iterator() {
        return firstEvents.values().iterator();
    }

    public final String toString() {
        return this.getClass().getName() + " uniqueCriteria=" + Arrays.toString(viewFactory.criteriaExpressions);
    }

    protected Object getUniqueKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        ExprEvaluator[] uniqueCriteriaEval = viewFactory.criteriaExpressionEvals;
        if (uniqueCriteriaEval.length == 1) {
            return uniqueCriteriaEval[0].evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        }

        Object[] values = new Object[uniqueCriteriaEval.length];
        for (int i = 0; i < uniqueCriteriaEval.length; i++) {
            values[i] = uniqueCriteriaEval[i].evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        }
        return new MultiKeyUntyped(values);
    }

    /**
     * Returns true if empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return firstEvents.isEmpty();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(firstEvents, true, FirstUniqueByPropertyViewFactory.NAME, firstEvents.size(), firstEvents.size());
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
