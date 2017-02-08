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

import java.util.HashSet;
import java.util.LinkedHashSet;

public class IntersectBatchViewLocalState {
    private final EventBean[][] oldEventsPerView;
    private final EventBean[][] newEventsPerView;
    private final HashSet<EventBean> removedEvents = new LinkedHashSet<EventBean>();
    private boolean captureIRNonBatch;
    private boolean ignoreViewIRStream;

    public IntersectBatchViewLocalState(EventBean[][] oldEventsPerView, EventBean[][] newEventsPerView) {
        this.oldEventsPerView = oldEventsPerView;
        this.newEventsPerView = newEventsPerView;
    }

    public EventBean[][] getOldEventsPerView() {
        return oldEventsPerView;
    }

    public EventBean[][] getNewEventsPerView() {
        return newEventsPerView;
    }

    public HashSet<EventBean> getRemovedEvents() {
        return removedEvents;
    }

    public boolean isCaptureIRNonBatch() {
        return captureIRNonBatch;
    }

    public void setCaptureIRNonBatch(boolean captureIRNonBatch) {
        this.captureIRNonBatch = captureIRNonBatch;
    }

    public boolean isIgnoreViewIRStream() {
        return ignoreViewIRStream;
    }

    public void setIgnoreViewIRStream(boolean ignoreViewIRStream) {
        this.ignoreViewIRStream = ignoreViewIRStream;
    }
}
