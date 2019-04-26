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
import com.espertech.esper.common.client.util.MultiKey;
import com.espertech.esper.common.internal.collection.MultiKeyArrayWrap;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This view simply adds a property to the events posted to it. This is useful for the group-merge views.
 */
public final class AddPropertyValueOptionalView extends ViewSupport implements AgentInstanceStopCallback {
    private final GroupByViewFactory groupByViewFactory;
    private final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private final Object propertyValues;

    // Keep a history of posted old events to avoid reconstructing the event
    // and adhere to the contract of posting the same reference to child views.
    // Only for must-add-property.
    private Map<EventBean, EventBean> newToOldEventMap;

    public AddPropertyValueOptionalView(GroupByViewFactory groupByViewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext, Object mergeValues) {
        this.groupByViewFactory = groupByViewFactory;
        this.propertyValues = mergeValues;
        this.agentInstanceContext = agentInstanceContext;
        this.newToOldEventMap = new HashMap<>();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        EventBean[] newEvents = null;
        EventBean[] oldEvents = null;

        if (newData != null) {
            newEvents = new EventBean[newData.length];

            int index = 0;
            for (EventBean newEvent : newData) {
                EventBean theEvent = addProperty(newEvent, groupByViewFactory.propertyNames, propertyValues, groupByViewFactory.eventType, agentInstanceContext.getEventBeanTypedEventFactory());
                newEvents[index++] = theEvent;

                newToOldEventMap.put(newEvent, theEvent);
            }
        }

        if (oldData != null) {
            oldEvents = new EventBean[oldData.length];

            int index = 0;
            for (EventBean oldEvent : oldData) {
                EventBean outgoing = newToOldEventMap.remove(oldEvent);
                if (outgoing != null) {
                    oldEvents[index++] = outgoing;
                } else {
                    EventBean theEvent = addProperty(oldEvent, groupByViewFactory.propertyNames, propertyValues, groupByViewFactory.eventType, agentInstanceContext.getEventBeanTypedEventFactory());
                    oldEvents[index++] = theEvent;
                }
            }
        }

        agentInstanceContext.getInstrumentationProvider().qViewIndicate(groupByViewFactory, newEvents, oldEvents);
        child.update(newEvents, oldEvents);
        agentInstanceContext.getInstrumentationProvider().aViewIndicate();
    }

    public final EventType getEventType() {
        return groupByViewFactory.eventType;
    }

    public final Iterator<EventBean> iterator() {
        final Iterator<EventBean> parentIterator = parent.iterator();

        return new Iterator<EventBean>() {
            public boolean hasNext() {
                return parentIterator.hasNext();
            }

            public EventBean next() {
                EventBean nextEvent = parentIterator.next();
                return addProperty(nextEvent, groupByViewFactory.propertyNames, propertyValues, groupByViewFactory.eventType,
                    agentInstanceContext.getEventBeanTypedEventFactory());
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void stop(AgentInstanceStopServices services) {
        if (!newToOldEventMap.isEmpty()) {
            OneEventCollection oldEvents = new OneEventCollection();
            for (Map.Entry<EventBean, EventBean> oldEvent : newToOldEventMap.entrySet()) {
                oldEvents.add(oldEvent.getValue());
            }
            if (!oldEvents.isEmpty()) {
                child.update(null, oldEvents.toArray());
            }
            newToOldEventMap.clear();
        }
    }

    /**
     * Add a property to the event passed in.
     *
     * @param originalEvent       - event to add property to
     * @param propertyNames       - names of properties to add
     * @param propertyValues      - value of properties to add
     * @param targetEventType     - new event type
     * @param eventAdapterService - service for generating events and handling event types
     * @return event with added property
     */
    protected static EventBean addProperty(EventBean originalEvent,
                                           String[] propertyNames,
                                           Object propertyValues,
                                           EventType targetEventType,
                                           EventBeanTypedEventFactory eventAdapterService) {
        Map<String, Object> values = new HashMap<String, Object>();
        if (propertyValues instanceof MultiKey) {
            MultiKey props = (MultiKey) propertyValues;
            for (int i = 0; i < propertyNames.length; i++) {
                values.put(propertyNames[i], props.getKey(i));
            }
        } else {
            if (propertyValues instanceof MultiKeyArrayWrap) {
                propertyValues = ((MultiKeyArrayWrap) propertyValues).getArray();
            }
            values.put(propertyNames[0], propertyValues);
        }

        return eventAdapterService.adapterForTypedWrapper(originalEvent, values, targetEventType);
    }

    public final String toString() {
        return this.getClass().getName() + " propertyValue=" + propertyValues;
    }

    private static final Logger log = LoggerFactory.getLogger(AddPropertyValueOptionalView.class);
}
