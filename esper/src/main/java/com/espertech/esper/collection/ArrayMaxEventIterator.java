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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for iterating over an array of events up to a given max number of events.
 */
public class ArrayMaxEventIterator implements Iterator<EventBean> {
    private final EventBean[] events;
    private final int maxNumEvents;
    private int position;

    /**
     * Ctor.
     *
     * @param events       to iterate
     * @param maxNumEvents max to iterate
     */
    public ArrayMaxEventIterator(EventBean[] events, int maxNumEvents) {
        this.events = events;
        this.maxNumEvents = maxNumEvents;
    }

    public boolean hasNext() {
        if ((events == null) || (position >= events.length) || (position >= maxNumEvents)) {
            return false;
        }
        return true;
    }

    public EventBean next() {
        if ((events == null) || (position >= events.length) || (position >= maxNumEvents)) {
            throw new NoSuchElementException();
        }
        return events[position++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}