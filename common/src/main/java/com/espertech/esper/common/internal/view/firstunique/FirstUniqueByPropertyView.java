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
package com.espertech.esper.common.internal.view.firstunique;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.view.core.*;

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
    protected final AgentInstanceContext agentInstanceContext;

    public FirstUniqueByPropertyView(FirstUniqueByPropertyViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, viewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(viewFactory, newData, oldData);

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

                // Post the new value
                if (newDataToPost == null) {
                    newDataToPost = new EventBean[]{newEvent};
                } else {
                    newDataToPost = EventBeanUtility.addToArray(newDataToPost, newEvent);
                }
            }
        }

        if ((child != null) && ((newDataToPost != null) || (oldDataToPost != null))) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newDataToPost, oldDataToPost);
            child.update(newDataToPost, oldDataToPost);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }
        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final Iterator<EventBean> iterator() {
        return firstEvents.values().iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    protected Object getUniqueKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        return viewFactory.criteriaEval.evaluate(eventsPerStream, true, agentInstanceContext);
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
        viewDataVisitor.visitPrimary(firstEvents, true, viewFactory.getViewName(), firstEvents.size(), firstEvents.size());
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
