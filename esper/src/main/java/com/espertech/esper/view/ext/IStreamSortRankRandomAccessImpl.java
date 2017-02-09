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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.ArrayMaxEventCollectionRO;
import com.espertech.esper.collection.ArrayMaxEventIterator;
import com.espertech.esper.view.window.RandomAccessByIndex;
import com.espertech.esper.view.window.RandomAccessByIndexObserver;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Provides random access into a rank-window's data.
 */
public class IStreamSortRankRandomAccessImpl implements RandomAccessByIndex, IStreamSortRankRandomAccess {
    private final RandomAccessByIndexObserver updateObserver;

    private TreeMap<Object, Object> sortedEvents;
    private int currentSize;

    private Iterator<Object> iterator;
    private EventBean[] cache;
    private int cacheFilledTo;

    /**
     * Ctor.
     *
     * @param updateObserver for indicating updates to
     */
    public IStreamSortRankRandomAccessImpl(RandomAccessByIndexObserver updateObserver) {
        this.updateObserver = updateObserver;
    }

    /**
     * Refreshes the random access data with the updated information.
     *
     * @param sortedEvents is the sorted window contents
     * @param currentSize  is the current size of the window
     * @param maxSize      is the maximum size of the window
     */
    public void refresh(TreeMap<Object, Object> sortedEvents, int currentSize, int maxSize) {
        updateObserver.updated(this);
        this.sortedEvents = sortedEvents;
        this.currentSize = currentSize;

        this.iterator = null;
        this.cacheFilledTo = 0;
        if (cache == null || cache.length < maxSize) {
            cache = new EventBean[maxSize];
        }
    }

    public EventBean getNewData(int index) {
        if (iterator == null) {
            iterator = sortedEvents.values().iterator();
        }

        // if asking for more then the sorted window currently holds, return no data
        if (index >= currentSize) {
            return null;
        }

        // If we have it in cache, serve from cache
        if (index < cacheFilledTo) {
            return cache[index];
        }

        // Load more into cache
        while (true) {
            if (cacheFilledTo == currentSize) {
                break;
            }
            if (!iterator.hasNext()) {
                break;
            }
            Object entry = iterator.next();
            if (entry instanceof List) {
                List<EventBean> events = (List<EventBean>) entry;
                for (EventBean theEvent : events) {
                    cache[cacheFilledTo] = theEvent;
                    cacheFilledTo++;
                }
            } else {
                EventBean theEvent = (EventBean) entry;
                cache[cacheFilledTo] = theEvent;
                cacheFilledTo++;
            }

            if (cacheFilledTo > index) {
                break;
            }
        }

        // If we have it in cache, serve from cache
        if (index <= cacheFilledTo) {
            return cache[index];
        }

        return null;
    }

    public EventBean getOldData(int index) {
        return null;
    }

    public EventBean getNewDataTail(int index) {
        initCache();

        if ((index < cacheFilledTo) && (index >= 0)) {
            return cache[cacheFilledTo - index - 1];
        }

        return null;
    }

    public Iterator<EventBean> getWindowIterator() {
        initCache();
        return new ArrayMaxEventIterator(cache, cacheFilledTo);
    }

    public Collection<EventBean> getWindowCollectionReadOnly() {
        initCache();
        return new ArrayMaxEventCollectionRO(cache, cacheFilledTo);
    }

    public int getWindowCount() {
        return currentSize;
    }

    private void initCache() {

        if (iterator == null) {
            iterator = sortedEvents.values().iterator();
        }

        // Load more into cache
        while (true) {
            if (cacheFilledTo == currentSize) {
                break;
            }
            if (!iterator.hasNext()) {
                break;
            }
            Object entry = iterator.next();
            if (entry instanceof List) {
                List<EventBean> events = (List<EventBean>) entry;
                for (EventBean theEvent : events) {
                    cache[cacheFilledTo] = theEvent;
                    cacheFilledTo++;
                }
            } else {
                EventBean theEvent = (EventBean) entry;
                cache[cacheFilledTo] = theEvent;
                cacheFilledTo++;
            }
        }
    }
}
