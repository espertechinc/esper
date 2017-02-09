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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Iterator for an iterator of events returning the underlying itself.
 */
public class EventUnderlyingCollection implements Collection<Object> {
    private final Collection<EventBean> events;
    private Collection<Object> buf;

    public EventUnderlyingCollection(Collection<EventBean> events) {
        this.events = events;
    }

    private void init() {
        if (buf == null) {
            Object[] objects = new Object[events.size()];
            int count = 0;
            for (EventBean theEvent : events) {
                objects[count++] = theEvent.getUnderlying();
            }
            buf = Arrays.asList(objects);
        }
    }

    public boolean containsAll(Collection<?> c) {
        init();
        return buf.containsAll(c);
    }

    public <T> T[] toArray(T[] arr) {
        int count = 0;
        for (EventBean theEvent : events) {
            arr[count++] = (T) theEvent.getUnderlying();
        }
        return arr;
    }

    public Object[] toArray() {
        Object[] arr = new Object[events.size()];
        int count = 0;
        for (EventBean theEvent : events) {
            arr[count++] = theEvent.getUnderlying();
        }
        return arr;
    }

    public Iterator<Object> iterator() {
        return new EventUnderlyingIterator(events);
    }

    public boolean contains(Object o) {
        init();
        return buf.contains(o);
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public int size() {
        return events.size();
    }

    public void clear() {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean addAll(Collection<? extends Object> c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException("Read-only implementation");
    }
}
