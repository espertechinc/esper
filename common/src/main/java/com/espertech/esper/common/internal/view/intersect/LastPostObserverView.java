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
package com.espertech.esper.common.internal.view.intersect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Iterator;

/**
 * A view that retains the last update.
 */
public final class LastPostObserverView extends ViewSupport {
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

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (observer != null) {
            observer.newData(streamId, newData, oldData);
        }
    }
}
