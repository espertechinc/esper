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
package com.espertech.esper.common.internal.view.unique;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This view includes only the most recent among events having the same value for the specified field or fields.
 * The view accepts the field name as parameter from which the unique values are obtained.
 * For example, a trade's symbol could be used as a unique value.
 * In this example, the first trade for symbol IBM would be posted as new data to child views.
 * When the second trade for symbol IBM arrives the second trade is posted as new data to child views,
 * and the first trade is posted as old data.
 * Should more than one trades for symbol IBM arrive at the same time (like when batched)
 * then the child view will get all new events in newData and all new events in oldData minus the most recent event.
 * When the current new event arrives as old data, the the current unique event gets thrown away and
 * posted as old data to child views.
 * Iteration through the views data shows only the most recent events received for the unique value in the order
 * that events arrived in.
 * The type of the field returning the unique value can be any type but should override equals and hashCode()
 * as the type plays the role of a key in a map storing unique values.
 */
public class UniqueByPropertyView extends ViewSupport implements DataWindowView {
    private final UniqueByPropertyViewFactory viewFactory;
    protected final Map<Object, EventBean> mostRecentEvents = new HashMap<Object, EventBean>();
    private final EventBean[] eventsPerStream = new EventBean[1];
    protected final AgentInstanceContext agentInstanceContext;

    public UniqueByPropertyView(UniqueByPropertyViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceViewFactoryContext.getAgentInstanceContext();
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, viewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(viewFactory, newData, oldData);

        if (newData != null && newData.length == 1 && (oldData == null || oldData.length == 0)) {
            // Shortcut
            Object key = getUniqueKey(newData[0]);
            EventBean lastValue = mostRecentEvents.put(key, newData[0]);
            if (child != null) {
                EventBean[] oldDataToPost = lastValue == null ? null : new EventBean[]{lastValue};
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newData, oldDataToPost);
                child.update(newData, oldDataToPost);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        } else {
            OneEventCollection postOldData = null;

            if (child != null) {
                postOldData = new OneEventCollection();
            }

            if (newData != null) {
                for (int i = 0; i < newData.length; i++) {
                    // Obtain unique value
                    Object key = getUniqueKey(newData[i]);

                    // If there are no child views, just update the own collection
                    if (child == null) {
                        mostRecentEvents.put(key, newData[i]);
                        continue;
                    }

                    // Post the last value as old data
                    EventBean lastValue = mostRecentEvents.get(key);
                    if (lastValue != null) {
                        postOldData.add(lastValue);
                    }

                    // Override with recent event
                    mostRecentEvents.put(key, newData[i]);
                }
            }

            if (oldData != null) {
                for (int i = 0; i < oldData.length; i++) {
                    // Obtain unique value
                    Object key = getUniqueKey(oldData[i]);

                    // If the old event is the current unique event, remove and post as old data
                    EventBean lastValue = mostRecentEvents.get(key);
                    if (lastValue == null || !lastValue.equals(oldData[i])) {
                        continue;
                    }

                    postOldData.add(lastValue);
                    mostRecentEvents.remove(key);
                }
            }


            // If there are child views, fireStatementStopped update method
            if (child != null) {
                if (postOldData.isEmpty()) {
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newData, null);
                    child.update(newData, null);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                } else {
                    EventBean[] postOldDataArray = postOldData.toArray();
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newData, postOldDataArray);
                    child.update(newData, postOldDataArray);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                }
            }
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    /**
     * Returns true if the view is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return mostRecentEvents.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return mostRecentEvents.values().iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    protected Object getUniqueKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        return viewFactory.criteriaEval.evaluate(eventsPerStream, true, agentInstanceContext);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(mostRecentEvents, true, viewFactory.getViewName(), mostRecentEvents.size(), mostRecentEvents.size());
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
