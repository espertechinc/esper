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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.ViewUpdatedCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * For use with length and time window views that must provide random access into data window contents
 * provided for the "previous" expression if used.
 */
public class IStreamRandomAccess implements RandomAccessByIndex, ViewUpdatedCollection {
    private final ArrayList<EventBean> arrayList;
    private final RandomAccessByIndexObserver updateObserver;

    /**
     * Ctor.
     *
     * @param updateObserver is invoked when updates are received
     */
    public IStreamRandomAccess(RandomAccessByIndexObserver updateObserver) {
        this.updateObserver = updateObserver;
        this.arrayList = new ArrayList<EventBean>();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (updateObserver != null) {
            updateObserver.updated(this);
        }
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                arrayList.add(0, newData[i]);
            }
        }

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                arrayList.remove(arrayList.size() - 1);
            }
        }
    }

    /**
     * Remove event.
     *
     * @param oldData event to remove
     */
    public void remove(EventBean oldData) {
        if (updateObserver != null) {
            updateObserver.updated(this);
        }
        arrayList.remove(arrayList.size() - 1);
    }

    /**
     * Apply event
     *
     * @param newData to apply
     */
    public void update(EventBean newData) {
        if (updateObserver != null) {
            updateObserver.updated(this);
        }
        arrayList.add(0, newData);
    }

    public EventBean getNewData(int index) {
        // New events are added to the start of the list
        if (index < arrayList.size()) {
            return arrayList.get(index);
        }
        return null;
    }

    public EventBean getOldData(int index) {
        return null;
    }

    public void destroy() {
        // No action required
    }

    /**
     * Returns true for empty.
     *
     * @return indicator
     */
    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    public EventBean getNewDataTail(int index) {
        // New events are added to the start of the list
        if (index < arrayList.size() && index >= 0) {
            return arrayList.get(arrayList.size() - index - 1);
        }
        return null;
    }

    public Iterator<EventBean> getWindowIterator() {
        return arrayList.iterator();
    }

    public Collection<EventBean> getWindowCollectionReadOnly() {
        return arrayList;
    }

    public int getWindowCount() {
        return arrayList.size();
    }

    public int getNumEventsInsertBuf() {
        return getWindowCount();
    }
}
