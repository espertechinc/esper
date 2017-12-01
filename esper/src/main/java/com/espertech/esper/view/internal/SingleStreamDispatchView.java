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
import com.espertech.esper.event.FlushedEventBuffer;
import com.espertech.esper.core.service.EPStatementDispatch;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

/**
 * View to dispatch for a single stream (no join).
 */
public final class SingleStreamDispatchView extends ViewSupport implements EPStatementDispatch {
    private boolean hasData = false;
    private FlushedEventBuffer newDataBuffer = new FlushedEventBuffer();
    private FlushedEventBuffer oldDataBuffer = new FlushedEventBuffer();

    /**
     * Ctor.
     */
    public SingleStreamDispatchView() {
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public final Iterator<EventBean> iterator() {
        return parent.iterator();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        newDataBuffer.add(newData);
        oldDataBuffer.add(oldData);
        hasData = true;
    }

    public void execute() {
        if (hasData) {
            hasData = false;
            this.updateChildren(newDataBuffer.getAndFlush(), oldDataBuffer.getAndFlush());
        }
    }
}