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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.CollectionUtil;

/**
 * State holder for matches, backed by an array, for fast copying and writing.
 */
public class MultimatchState {
    private int count;
    private EventBean[] events;

    public MultimatchState(int count, EventBean[] events) {
        this.count = count;
        this.events = events;
    }

    /**
     * Ctor.
     *
     * @param theEvent first event to hold
     */
    public MultimatchState(EventBean theEvent) {
        events = new EventBean[3];
        add(theEvent);
    }

    /**
     * Ctor.
     *
     * @param state to copy
     */
    public MultimatchState(MultimatchState state) {
        EventBean[] copyArray = new EventBean[state.getBuffer().length];
        System.arraycopy(state.getBuffer(), 0, copyArray, 0, state.getCount());

        count = state.getCount();
        events = copyArray;
    }

    /**
     * Add an event.
     *
     * @param theEvent to add
     */
    public void add(EventBean theEvent) {
        if (count == events.length) {
            EventBean[] buf = new EventBean[events.length * 2];
            System.arraycopy(events, 0, buf, 0, events.length);
            events = buf;
        }
        events[count++] = theEvent;
    }

    /**
     * Returns the count of events.
     *
     * @return count
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns the raw buffer.
     *
     * @return buffer
     */
    public EventBean[] getBuffer() {
        return events;
    }

    /**
     * Determines if an event is in the collection.
     *
     * @param theEvent to check
     * @return indicator
     */
    public boolean containsEvent(EventBean theEvent) {
        for (int i = 0; i < count; i++) {
            if (events[i].equals(theEvent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the buffer sized to only the contained events, and shrinks the event array unless it is empty
     *
     * @return events
     */
    public EventBean[] getShrinkEventArray() {
        if (count == 0) {
            return CollectionUtil.EVENTBEANARRAY_EMPTY;
        }
        if (count == events.length) {
            return events;
        }
        EventBean[] array = new EventBean[count];
        System.arraycopy(events, 0, array, 0, count);
        events = array; // we hold on to the result, avoiding future shrinking
        return array;
    }
}
