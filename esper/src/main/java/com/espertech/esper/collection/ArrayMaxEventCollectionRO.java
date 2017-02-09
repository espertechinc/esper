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
 * Iterator for iterating over an array of events up to a given max number of events.
 */
public class ArrayMaxEventCollectionRO implements Collection<EventBean> {
    private final EventBean[] events;
    private final int maxNumEvents;

    /**
     * Ctor.
     *
     * @param events       to iterate
     * @param maxNumEvents max to iterate
     */
    public ArrayMaxEventCollectionRO(EventBean[] events, int maxNumEvents) {
        this.events = events;
        this.maxNumEvents = maxNumEvents;
    }

    public boolean isEmpty() {
        return maxNumEvents == 0;
    }

    public int size() {
        return maxNumEvents;
    }

    public Iterator<EventBean> iterator() {
        return new ArrayMaxEventIterator(events, maxNumEvents);
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Partial implementation");
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Partial implementation");
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("Partial implementation");
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Partial implementation");
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

    public boolean addAll(Collection<? extends EventBean> c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean add(EventBean eventBean) {
        throw new UnsupportedOperationException("Read-only implementation");
    }
}