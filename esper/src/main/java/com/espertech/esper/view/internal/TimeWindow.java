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
import com.espertech.esper.view.DataWindowViewFactory;
import com.espertech.esper.view.ViewDataVisitor;

import java.util.*;

/**
 * Container for events per time slot. The time is provided as long milliseconds by client classes.
 * Events are for a specified timestamp and the implementation creates and adds the event to a slot for that timestamp.
 * Events can be expired from the window via the expireEvents method when their timestamp is before
 * (or less then) an expiry timestamp passed in. Expiry removes the event from the window.
 * The window allows iteration through its contents.
 * <p>
 * It is assumed that the timestamp passed to the add method is ascending. The window is backed by a
 * collection reflecting the timestamp order rather then any sorted map or linked hash map for performance reasons.
 */
public final class TimeWindow implements Iterable {
    private ArrayDeque<TimeWindowPair> window;
    private Map<EventBean, TimeWindowPair> reverseIndex;
    private int size;

    /**
     * Ctor.
     *
     * @param isSupportRemoveStream true to indicate the time window should support effective removal of events
     *                              in the window based on the remove stream events received, or false to not accomodate removal at all
     */
    public TimeWindow(boolean isSupportRemoveStream) {
        this.window = new ArrayDeque<TimeWindowPair>();

        if (isSupportRemoveStream) {
            reverseIndex = new HashMap<EventBean, TimeWindowPair>();
        }
    }

    /**
     * Adjust expiry dates.
     *
     * @param delta delta to adjust for
     */
    public void adjust(long delta) {
        for (TimeWindowPair data : window) {
            data.setTimestamp(data.getTimestamp() + delta);
        }
    }

    /**
     * Adds event to the time window for the specified timestamp.
     *
     * @param timestamp - the time slot for the event
     * @param bean      - event to add
     */
    public final void add(long timestamp, EventBean bean) {
        // Empty window
        if (window.isEmpty()) {
            TimeWindowPair pair = new TimeWindowPair(timestamp, bean);
            window.add(pair);

            if (reverseIndex != null) {
                reverseIndex.put(bean, pair);
            }
            size = 1;
            return;
        }

        TimeWindowPair lastPair = window.getLast();

        // Windows last timestamp matches the one supplied
        if (lastPair.getTimestamp() == timestamp) {
            if (lastPair.getEventHolder() instanceof List) {
                List<EventBean> list = (List<EventBean>) lastPair.getEventHolder();
                list.add(bean);
            } else if (lastPair.getEventHolder() == null) {
                lastPair.setEventHolder(bean);
            } else {
                EventBean existing = (EventBean) lastPair.getEventHolder();
                List<EventBean> list = new ArrayList<EventBean>(4);
                list.add(existing);
                list.add(bean);
                lastPair.setEventHolder(list);
            }
            if (reverseIndex != null) {
                reverseIndex.put(bean, lastPair);
            }
            size++;
            return;
        }

        // Append to window
        TimeWindowPair pair = new TimeWindowPair(timestamp, bean);
        if (reverseIndex != null) {
            reverseIndex.put(bean, pair);
        }
        window.add(pair);
        size++;
    }

    /**
     * Removes the event from the window, if remove stream handling is enabled.
     *
     * @param theEvent to remove
     */
    public final void remove(EventBean theEvent) {
        if (reverseIndex == null) {
            throw new UnsupportedOperationException("Time window does not accept event removal");
        }
        TimeWindowPair pair = reverseIndex.get(theEvent);
        if (pair != null) {
            if (pair.getEventHolder() != null && pair.getEventHolder().equals(theEvent)) {
                pair.setEventHolder(null);
                size--;
            } else if (pair.getEventHolder() != null) {
                List<EventBean> list = (List<EventBean>) pair.getEventHolder();
                boolean removed = list.remove(theEvent);
                if (removed) {
                    size--;
                }
            }
            reverseIndex.remove(theEvent);
        }
    }

    /**
     * Return and remove events in time-slots earlier (less) then the timestamp passed in,
     * returning the list of events expired.
     *
     * @param expireBefore is the timestamp from which on to keep events in the window
     * @return a list of events expired and removed from the window, or null if none expired
     */
    public final ArrayDeque<EventBean> expireEvents(long expireBefore) {
        if (window.isEmpty()) {
            return null;
        }

        TimeWindowPair pair = window.getFirst();

        // If the first entry's timestamp is after the expiry date, nothing to expire
        if (pair.getTimestamp() >= expireBefore) {
            return null;
        }

        ArrayDeque<EventBean> resultBeans = new ArrayDeque<EventBean>();

        // Repeat until the window is empty or the timestamp is above the expiry time
        do {
            if (pair.getEventHolder() != null) {
                if (pair.getEventHolder() instanceof EventBean) {
                    resultBeans.add((EventBean) pair.getEventHolder());
                } else {
                    resultBeans.addAll((List<EventBean>) pair.getEventHolder());
                }
            }

            window.removeFirst();

            if (window.isEmpty()) {
                break;
            }

            pair = window.getFirst();
        }
        while (pair.getTimestamp() < expireBefore);

        if (reverseIndex != null) {
            for (EventBean expired : resultBeans) {
                reverseIndex.remove(expired);
            }
        }

        size -= resultBeans.size();
        return resultBeans;
    }

    /**
     * Returns event iterator.
     *
     * @return iterator over events currently in window
     */
    public final Iterator<EventBean> iterator() {
        return new TimeWindowIterator(window);
    }

    /**
     * Returns the oldest timestamp in the collection if there is at least one entry,
     * else it returns null if the window is empty.
     *
     * @return null if empty, oldest timestamp if not empty
     */
    public final Long getOldestTimestamp() {
        if (window.isEmpty()) {
            return null;
        }
        if (window.getFirst().getEventHolder() != null) {
            return window.getFirst().getTimestamp();
        }
        for (TimeWindowPair pair : window) {
            if (pair.getEventHolder() != null) {
                return pair.getTimestamp();
            }
        }
        return null;
    }

    /**
     * Returns true if the window is currently empty.
     *
     * @return true if empty, false if not
     */
    public final boolean isEmpty() {
        return getOldestTimestamp() == null;
    }

    /**
     * Returns the reverse index, for testing purposes.
     *
     * @return reverse index
     */
    public Map<EventBean, TimeWindowPair> getReverseIndex() {
        return reverseIndex;
    }

    public ArrayDeque<TimeWindowPair> getWindow() {
        return window;
    }

    public void setWindow(ArrayDeque<TimeWindowPair> window, int size) {
        this.window = window;
        this.size = size;
    }

    public void setReverseIndex(Map<EventBean, TimeWindowPair> reverseIndex) {
        this.reverseIndex = reverseIndex;
    }

    public void visitView(ViewDataVisitor viewDataVisitor, DataWindowViewFactory viewFactory) {
        viewDataVisitor.visitPrimary(window, false, viewFactory.getViewName(), size);
    }
}
