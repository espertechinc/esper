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
import com.espertech.esper.client.EventType;
import com.espertech.esper.view.View;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;

/**
 * A view that retains the last update.
 */
public final class LastPostObserverView implements View {
    private Viewable parent;
    private final int streamId;
    private LastPostObserver observer;

    /**
     * Ctor.
     *
     * @param streamId - number of the stream for which the view buffers the generated events.
     */
    public LastPostObserverView(int streamId) {
        this.streamId = streamId;
    }

    /**
     * Set an observer.
     *
     * @param observer to be called when results are available
     */
    public void setObserver(LastPostObserver observer) {
        this.observer = observer;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public final Iterator<EventBean> iterator() {
        return parent.iterator();
    }

    public Viewable getParent() {
        return parent;
    }

    public void setParent(Viewable parent) {
        this.parent = parent;
    }

    public View addView(View view) {
        throw new UnsupportedOperationException();
    }

    public View[] getViews() {
        return new View[0];
    }

    public boolean removeView(View view) {
        throw new UnsupportedOperationException();
    }

    public void removeAllViews() {
        throw new UnsupportedOperationException();
    }

    public boolean hasViews() {
        return false;
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (observer != null) {
            observer.newData(streamId, newData, oldData);
        }
    }
}
