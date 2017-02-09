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
package com.espertech.esper.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.util.CollectionUtil;

import java.util.Iterator;

/**
 * Event stream implementation that does not keep any window by itself of the events coming into the stream,
 * without the possibility to iterate the last event.
 */
public final class ZeroDepthStreamNoIterate implements EventStream {
    private View[] children = ViewSupport.EMPTY_VIEW_ARRAY;
    private final EventType eventType;

    /**
     * Ctor.
     *
     * @param eventType - type of event
     */
    public ZeroDepthStreamNoIterate(EventType eventType) {
        this.eventType = eventType;
    }

    public void insert(EventBean[] events) {
        for (View childView : children) {
            childView.update(events, null);
        }
    }

    public final void insert(EventBean theEvent) {
        // Get a new array created rather then re-use the old one since some client listeners
        // to this view may keep reference to the new data
        EventBean[] row = new EventBean[]{theEvent};
        for (View childView : children) {
            childView.update(row, null);
        }
    }

    public final EventType getEventType() {
        return eventType;
    }

    public final Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }

    public final View addView(View view) {
        children = ViewSupport.addView(children, view);
        view.setParent(this);
        return view;
    }

    public final View[] getViews() {
        return children;
    }

    public final boolean removeView(View view) {
        int index = ViewSupport.findViewIndex(children, view);
        if (index == -1) {
            return false;
        }
        children = ViewSupport.removeView(children, index);
        view.setParent(null);
        return true;
    }

    public final boolean hasViews() {
        return children.length > 0;
    }

    public void removeAllViews() {
        children = ViewSupport.EMPTY_VIEW_ARRAY;
    }
}


