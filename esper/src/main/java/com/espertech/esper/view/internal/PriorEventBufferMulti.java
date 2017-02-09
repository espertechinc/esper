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

import java.util.*;

/**
 * Buffers view-posted insert stream (new data) and remove stream (old data) events for
 * use with determining prior results in these streams, for multiple different prior events.
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
public class PriorEventBufferMulti implements ViewUpdatedCollection, RelativeAccessByEventNIndex {
    private final int priorToIndexesSize;
    private final int[] priorToIndexes;
    private final Map<EventBean, EventBean[]> priorEventMap;
    private final RollingEventBuffer newEvents;
    private EventBean[] lastOldData;

    /**
     * Ctor.
     *
     * @param priorToIndexSet holds a list of prior-event indexes.
     *                        <p> For example, an array {0,4,6} means the current event, 4 events before the current event
     *                        and 6 events before the current event.
     */
    public PriorEventBufferMulti(int[] priorToIndexSet) {
        // Determine the maximum prior index to retain
        int maxPriorIndex = 0;
        for (Integer priorIndex : priorToIndexSet) {
            if (priorIndex > maxPriorIndex) {
                maxPriorIndex = priorIndex;
            }
        }

        // Copy the set of indexes into an array, sort in ascending order
        priorToIndexesSize = priorToIndexSet.length;
        priorToIndexes = new int[priorToIndexesSize];
        int count = 0;
        for (Integer priorIndex : priorToIndexSet) {
            priorToIndexes[count++] = priorIndex;
        }
        Arrays.sort(priorToIndexes);

        // Construct a rolling buffer of new data for holding max index + 1 (position 1 requires 2 events to keep)
        newEvents = new RollingEventBuffer(maxPriorIndex + 1);
        priorEventMap = new HashMap<EventBean, EventBean[]>();
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

                // Save prior index events in array
                EventBean[] priorEvents = new EventBean[priorToIndexesSize];
                for (int j = 0; j < priorToIndexesSize; j++) {
                    int priorIndex = priorToIndexes[j];
                    priorEvents[j] = newEvents.get(priorIndex);
                }
                priorEventMap.put(newEvent, priorEvents);
            }
        }

        // Save old data to be removed next time we get posted results
        lastOldData = oldData;
    }

    public void update(EventBean[] newData, EventBean[] oldData, PriorEventBufferChangeCaptureMulti capture) {
        // Remove last old data posted in previous post
        if (lastOldData != null) {
            for (int i = 0; i < lastOldData.length; i++) {
                EventBean oldDataItem = lastOldData[i];
                priorEventMap.remove(oldDataItem);
                capture.removed(oldDataItem);
            }
        }

        // Post new data to rolling buffer starting with the oldest
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                EventBean newEvent = newData[i];

                // Add new event
                newEvents.add(newEvent);

                // Save prior index events in array
                EventBean[] priorEvents = new EventBean[priorToIndexesSize];
                for (int j = 0; j < priorToIndexesSize; j++) {
                    int priorIndex = priorToIndexes[j];
                    priorEvents[j] = newEvents.get(priorIndex);
                }
                priorEventMap.put(newEvent, priorEvents);
                capture.added(newEvent, priorEvents);
            }
        }

        // Save old data to be removed next time we get posted results
        lastOldData = oldData;
    }

    public EventBean getRelativeToEvent(EventBean theEvent, int priorToIndex) {
        if (priorToIndex >= priorToIndexesSize) {
            throw new IllegalArgumentException("Index " + priorToIndex + " not allowed, max size is " + priorToIndexesSize);
        }
        EventBean[] priorEvents = priorEventMap.get(theEvent);
        if (priorEvents == null) {
            throw new IllegalStateException("Event not currently in collection, event=" + theEvent);
        }
        return priorEvents[priorToIndex];
    }

    public EventBean getRelativeToEnd(int index) {
        // No requirements to return events related to the end of the current buffer
        return null;
    }

    public int getWindowToEventCount() {
        // No requirements to return events related to the end of the current buffer
        return 0;
    }

    public Iterator<EventBean> getWindowToEvent() {
        // No requirements to return events related to the end of the current buffer
        return null;
    }

    public Collection<EventBean> getWindowToEventCollReadOnly() {
        // No requirements to return events related to the end of the current buffer
        return null;
    }

    public void destroy() {
        // No action required
    }

    public Map<EventBean, EventBean[]> getPriorEventMap() {
        return priorEventMap;
    }

    public RollingEventBuffer getNewEvents() {
        return newEvents;
    }

    public int getNumEventsInsertBuf() {
        return newEvents.getSize();
    }
}
