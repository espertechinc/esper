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

import java.util.Collection;
import java.util.Iterator;

/**
 * Iterator for an iterator of events returning the underlying itself.
 */
public class EventUnderlyingIterator implements Iterator<Object> {
    private final Iterator<EventBean> events;

    public EventUnderlyingIterator(Collection<EventBean> events) {
        this.events = events.iterator();
    }

    public boolean hasNext() {
        return events.hasNext();
    }

    public Object next() {
        return events.next().getUnderlying();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
