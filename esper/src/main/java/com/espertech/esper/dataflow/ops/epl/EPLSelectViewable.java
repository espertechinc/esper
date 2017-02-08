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
package com.espertech.esper.dataflow.ops.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

public class EPLSelectViewable extends ViewSupport {

    private View childView;
    private EventBean[] eventBatch = new EventBean[1];
    private final EventType eventType;

    public EPLSelectViewable(EventType eventType) {
        this.eventType = eventType;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        return;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        return null;
    }

    public void process(EventBean theEvent) {
        eventBatch[0] = theEvent;
        childView.update(eventBatch, null);
    }

    public View addView(View view) {
        childView = view;
        return super.addView(view);
    }
}
