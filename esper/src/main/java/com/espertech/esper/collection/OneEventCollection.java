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
package com.espertech.esper.collection;

import com.espertech.esper.client.EventBean;

import java.util.ArrayDeque;

/**
 * Simple collection that exposes a limited add-and-get interface and that is optimized towards holding
 * a single event, but can hold multiple events. If more then one event is added, the
 * class allocates a linked list for additional events.
 */
public class OneEventCollection {
    private EventBean firstEvent;
    private ArrayDeque<EventBean> additionalEvents;

    /**
     * Add an event to the collection.
     *
     * @param theEvent is the event to add
     */
    public void add(EventBean theEvent) {
        if (theEvent == null) {
            throw new IllegalArgumentException("Null event not allowed");
        }

        if (firstEvent == null) {
            firstEvent = theEvent;
            return;
        }

        if (additionalEvents == null) {
            additionalEvents = new ArrayDeque<EventBean>();
        }
        additionalEvents.add(theEvent);
    }

    /**
     * Returns true if the collection is empty.
     *
     * @return true if empty, false if not
     */
    public boolean isEmpty() {
        return firstEvent == null;
    }

    /**
     * Returns an array holding the collected events.
     *
     * @return event array
     */
    public EventBean[] toArray() {
        if (firstEvent == null) {
            return new EventBean[0];
        }

        if (additionalEvents == null) {
            return new EventBean[]{firstEvent};
        }

        EventBean[] events = new EventBean[1 + additionalEvents.size()];
        events[0] = firstEvent;

        int count = 1;
        for (EventBean theEvent : additionalEvents) {
            events[count] = theEvent;
            count++;
        }

        return events;
    }

    public void add(EventBean[] events) {
        for (EventBean theEvent : events) {
            add(theEvent);
        }
    }

    public EventBean getFirstEvent() {
        return firstEvent;
    }

    public ArrayDeque<EventBean> getAdditionalEvents() {
        return additionalEvents;
    }
}
