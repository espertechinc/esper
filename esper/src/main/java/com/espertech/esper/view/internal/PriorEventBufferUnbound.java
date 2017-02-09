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
package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.RollingEventBuffer;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.view.window.RandomAccessByIndex;

import java.util.Collection;
import java.util.Iterator;

/**
 * Buffer class for insert stream events only for use with unbound streams that inserts data only, to serve
 * up one or more prior events in the insert stream based on an index.
 * <p>
 * Does not expect or care about the remove stream and simple keeps a rolling buffer of new data events
 * up to the maximum prior event we are asking for.
 */
public class PriorEventBufferUnbound implements ViewUpdatedCollection, RandomAccessByIndex {
    private final int maxSize;
    private final RollingEventBuffer newEvents;

    /**
     * Ctor.
     *
     * @param maxPriorIndex is the highest prior-event index required by any expression
     */
    public PriorEventBufferUnbound(int maxPriorIndex) {
        this.maxSize = maxPriorIndex + 1;
        newEvents = new RollingEventBuffer(maxSize);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // Post new data to rolling buffer starting with the oldest
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                EventBean newEvent = newData[i];

                // Add new event
                newEvents.add(newEvent);
            }
        }
    }

    public EventBean getNewData(int index) {
        if (index >= maxSize) {
            throw new IllegalArgumentException("Index " + index + " not allowed, max size is " + maxSize);
        }
        return newEvents.get(index);
    }

    public EventBean getOldData(int index) {
        return null;
    }

    public void destroy() {
        // No action required
    }

    public EventBean getNewDataTail(int index) {
        // No requirement to index from end of current buffer
        return null;
    }

    public Iterator<EventBean> getWindowIterator() {
        // no requirement for window iterator support
        return null;
    }

    public Collection<EventBean> getWindowCollectionReadOnly() {
        return null;
    }

    public int getWindowCount() {
        // no requirement for count support
        return 0;
    }

    public RollingEventBuffer getNewEvents() {
        return newEvents;
    }

    public int getNumEventsInsertBuf() {
        return newEvents.getSize();
    }
}
