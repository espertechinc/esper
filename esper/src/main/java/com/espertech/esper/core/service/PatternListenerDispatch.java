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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.dispatch.Dispatchable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;

/**
 * Dispatchable for dispatching events to update listeners.
 */
public class PatternListenerDispatch implements Dispatchable {
    private final static Logger log = LoggerFactory.getLogger(PatternListenerDispatch.class);
    private final Set<UpdateListener> listeners;

    private EventBean singleEvent;
    private ArrayList<EventBean> eventList;

    /**
     * Constructor.
     *
     * @param listeners is the listeners to dispatch to.
     */
    public PatternListenerDispatch(Set<UpdateListener> listeners) {
        this.listeners = listeners;
    }

    /**
     * Add an event to be dispatched.
     *
     * @param theEvent to add
     */
    public void add(EventBean theEvent) {
        if (singleEvent == null) {
            singleEvent = theEvent;
        } else {
            if (eventList == null) {
                eventList = new ArrayList<EventBean>(5);
                eventList.add(singleEvent);
            }
            eventList.add(theEvent);
        }
    }

    public void execute() {
        EventBean[] eventArray;

        if (eventList != null) {
            eventArray = eventList.toArray(new EventBean[eventList.size()]);
            eventList = null;
            singleEvent = null;
        } else {
            eventArray = new EventBean[]{singleEvent};
            singleEvent = null;
        }

        for (UpdateListener listener : listeners) {
            try {
                listener.update(eventArray, null);
            } catch (Throwable t) {
                String message = "Unexpected exception invoking listener update method on listener class '" + listener.getClass().getSimpleName() +
                        "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                log.error(message, t);
            }
        }
    }

    /**
     * Returns true if at least one event has been added.
     *
     * @return true if it has data, false if not
     */
    public boolean hasData() {
        if (singleEvent != null) {
            return true;
        }
        return false;
    }
}
