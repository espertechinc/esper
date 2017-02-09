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
 * Reversed iterator for an array of events.
 */
public class ReversedArrayEventIterator implements Iterator<EventBean> {
    private final EventBean[] events;
    private int position;

    public ReversedArrayEventIterator(EventBean[] events) {
        position = events.length - 1;
        this.events = events;
    }

    public boolean hasNext() {
        if ((events == null) || (position < 0)) {
            return false;
        }
        return true;
    }

    public EventBean next() {
        if ((events == null) || (position < 0)) {
            throw new NoSuchElementException();
        }
        return events[position--];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}