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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

public class ContextMergeView extends ViewSupport implements UpdateDispatchView {

    private final EventType eventType;

    public ContextMergeView(EventType eventType) {
        this.eventType = eventType;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // no action required
    }

    public void newResult(UniformPair<EventBean[]> result) {
        if (result != null && child != null) {
            child.update(result.getFirst(), result.getSecond());
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException("Iterator not supported");
    }
}
