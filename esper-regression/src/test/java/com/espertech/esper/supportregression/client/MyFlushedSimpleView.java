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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewSupport;
import com.espertech.esper.view.Viewable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyFlushedSimpleView extends ViewSupport implements StopCallback {
    private List<EventBean> events;
    private EventType eventType;

    public MyFlushedSimpleView(AgentInstanceViewFactoryChainContext agentInstanceContext) {
        agentInstanceContext.addTerminationCallback(this);
        events = new ArrayList<EventBean>();
    }

    public void stop() {
        this.updateChildren(events.toArray(new EventBean[0]), null);
        events = new ArrayList<EventBean>();
    }

    public void setParent(Viewable parent) {
        super.setParent(parent);
        if (parent != null) {
            eventType = parent.getEventType();
        }
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                events.add(newData[0]);
            }
        }
    }

    public final EventType getEventType() {
        return eventType;
    }

    public final Iterator<EventBean> iterator() {
        return events.iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }
}
