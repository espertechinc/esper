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
package com.espertech.esper.regressionlib.support.extend.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyFlushedSimpleView extends ViewSupport implements AgentInstanceStopCallback {
    private List<EventBean> events;
    private EventType eventType;

    public MyFlushedSimpleView(AgentInstanceViewFactoryChainContext agentInstanceContext) {
        events = new ArrayList<EventBean>();
    }

    public void stop(AgentInstanceStopServices services) {
        child.update(events.toArray(new EventBean[0]), null);
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
