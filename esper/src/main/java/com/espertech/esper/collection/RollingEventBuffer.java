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

/**
 * Event buffer of a given size provides a random access API into the most current event to prior events
 * up to the given size. Oldest events roll out of the buffer first.
 * <p>
 * Backed by a fixed-size array that is filled forward, then rolls back to the beginning
 * keeping track of the current position.
 */
public class RollingEventBuffer {
    private EventBean[] buffer;
    private int nextFreeIndex;

    /**
     * Ctor.
     *
     * @param size is the maximum number of events in buffer
     */
    public RollingEventBuffer(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Minimum buffer size is 1");
        }

        nextFreeIndex = 0;
        buffer = new EventBean[size];
    }

    /**
     * Add events to the buffer.
     *
     * @param events to add
     */
    public void add(EventBean[] events) {
        if (events == null) {
            return;
        }

        for (int i = 0; i < events.length; i++) {
            add(events[i]);
        }
    }

    /**
     * Add an event to the buffer.
     *
     * @param theEvent to add
     */
    public void add(EventBean theEvent) {
        buffer[nextFreeIndex] = theEvent;
        nextFreeIndex++;

        if (nextFreeIndex == buffer.length) {
            nextFreeIndex = 0;
        }
    }

    /**
     * Get an event prior to the last event posted given a number of events before the last.
     * <p>
     * Thus index 0 returns the last event added, index 1 returns the prior to the last event added
     * up to the maximum buffer size.
     *
     * @param index prior event index from zero to max size
     * @return prior event at given index
     */
    public EventBean get(int index) {
        if (index >= buffer.length) {
            throw new IllegalArgumentException("Invalid index " + index + " for size " + buffer.length);
        }

        // The newest event is at (nextFreeIndex + 1)
        int newest = nextFreeIndex - 1;
        int relative = newest - index;
        if (relative < 0) {
            relative += buffer.length;
        }
        return buffer[relative];
    }

    public int getSize() {
        return buffer.length;
    }

    public EventBean[] getBuffer() {
        return buffer;
    }

    public int getNextFreeIndex() {
        return nextFreeIndex;
    }

    public void setBuffer(EventBean[] buffer) {
        this.buffer = buffer;
    }

    public void setNextFreeIndex(int nextFreeIndex) {
        this.nextFreeIndex = nextFreeIndex;
    }
}
