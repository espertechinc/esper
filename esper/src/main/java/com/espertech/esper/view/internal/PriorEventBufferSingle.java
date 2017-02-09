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
import com.espertech.esper.view.window.RelativeAccessByEventNIndex;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Buffers view-posted insert stream (new data) and remove stream (old data) events for
 * use with serving prior results in these streams, for a single prior event.
 * <p>
 * Buffers only exactly those events in new data and old data that are being asked for via the
 * 2 or more 'prior' functions that specify different indexes. For example "select prior(2, price), prior(1, price)"
 * results in on buffer instance handling both the need to the immediatly prior (1) and the 2-events-ago
 * event (2).
 * <p>
 * As all views are required to post new data and post old data that removes the new data to subsequent views,
 * this buffer can be attached to all views and should not result in a memory leak.
 * <p>
 * When the buffer receives old data (rstream) events it removes the prior events to the rstream events
 * from the buffer the next time it receives a post (not immediatly) to allow queries to the buffer.
 */
public class PriorEventBufferSingle implements ViewUpdatedCollection, RelativeAccessByEventNIndex {
    private final int priorEventIndex;
    private final Map<EventBean, EventBean> priorEventMap;
    private final RollingEventBuffer newEvents;
    private EventBean[] lastOldData;

    /**
     * Ctor.
     *
     * @param priorEventIndex is the number-of-events prior to the current event we are interested in
     */
    public PriorEventBufferSingle(int priorEventIndex) {
        this.priorEventIndex = priorEventIndex;
        // Construct a rolling buffer of new data for holding max index + 1 (position 1 requires 2 events to keep)
        newEvents = new RollingEventBuffer(priorEventIndex + 1);
        priorEventMap = new HashMap<EventBean, EventBean>();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // Remove last old data posted in previous post
        if (lastOldData != null) {
            for (int i = 0; i < lastOldData.length; i++) {
                priorEventMap.remove(lastOldData[i]);
            }
        }

        // Post new data to rolling buffer starting with the oldest
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                EventBean newEvent = newData[i];

                // Add new event
                newEvents.add(newEvent);

                EventBean priorEvent = newEvents.get(priorEventIndex);
                priorEventMap.put(newEvent, priorEvent);
            }
        }

        // Save old data to be removed next time we get posted results
        lastOldData = oldData;
    }

    public void update(EventBean[] newData, EventBean[] oldData, PriorEventBufferChangeCaptureSingle captureSingle) {
        // Remove last old data posted in previous post
        if (lastOldData != null) {
            for (int i = 0; i < lastOldData.length; i++) {
                EventBean oldDataItem = lastOldData[i];
                priorEventMap.remove(oldDataItem);
                captureSingle.removed(oldDataItem);
            }
        }

        // Post new data to rolling buffer starting with the oldest
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                EventBean newEvent = newData[i];

                // Add new event
                newEvents.add(newEvent);

                EventBean priorEvent = newEvents.get(priorEventIndex);
                priorEventMap.put(newEvent, priorEvent);
                captureSingle.added(newEvent, priorEvent);
            }
        }

        // Save old data to be removed next time we get posted results
        lastOldData = oldData;
    }

    // Users are assigned an index
    public EventBean getRelativeToEvent(EventBean theEvent, int priorToIndex) {
        if (priorToIndex != 0) {
            throw new IllegalArgumentException("Single prior event buffer takes only a given index of zero");
        }
        return priorEventMap.get(theEvent);
    }

    public EventBean getRelativeToEnd(int index) {
        // No requirement to index from end of current buffer
        return null;
    }

    public Iterator<EventBean> getWindowToEvent() {
        // no requirement for window iterator support
        return null;
    }

    public int getWindowToEventCount() {
        // no requirement for count support
        return 0;
    }

    public Collection<EventBean> getWindowToEventCollReadOnly() {
        return null;
    }

    public Map<EventBean, EventBean> getPriorEventMap() {
        return priorEventMap;
    }

    public RollingEventBuffer getNewEvents() {
        return newEvents;
    }

    public void destroy() {
        // No action required
    }

    public int getNumEventsInsertBuf() {
        return newEvents.getSize();
    }
}
