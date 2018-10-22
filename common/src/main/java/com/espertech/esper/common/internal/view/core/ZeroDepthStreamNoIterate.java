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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Iterator;

/**
 * Event stream implementation that does not keep any window by itself of the events coming into the stream,
 * without the possibility to iterate the last event.
 */
public class ZeroDepthStreamNoIterate implements EventStream {
    private final EventType eventType;

    private View childView;

    /**
     * Ctor.
     *
     * @param eventType - type of event
     */
    public ZeroDepthStreamNoIterate(EventType eventType) {
        this.eventType = eventType;
    }

    public void insert(EventBean theEvent) {
        // Get a new array created rather then re-use the old one since some client listeners
        // to this view may keep reference to the new data
        EventBean[] row = new EventBean[]{theEvent};
        childView.update(row, null);
    }

    public void insert(EventBean[] events) {
        childView.update(events, null);
    }

    public final EventType getEventType() {
        return eventType;
    }

    public final Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }

    public void setChild(View view) {
        childView = view;
    }

    public View getChild() {
        return childView;
    }
}


