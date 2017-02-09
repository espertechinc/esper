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
package com.espertech.esper.supportunit.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public abstract class SupportBaseView extends ViewSupport {
    protected EventBean[] lastNewData;
    protected EventBean[] lastOldData;
    protected EventType eventType;
    protected boolean isInvoked;

    /**
     * Default constructor since views are also beans.
     */
    public SupportBaseView() {
    }

    public SupportBaseView(EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Iterator<EventBean> iterator() {
        log.info(".iterator Not implemented");
        return null;
    }

    public EventBean[] getLastNewData() {
        return lastNewData;
    }

    public EventBean[] getLastOldData() {
        return lastOldData;
    }

    public void setLastNewData(EventBean[] lastNewData) {
        this.lastNewData = lastNewData;
    }

    public void setLastOldData(EventBean[] lastOldData) {
        this.lastOldData = lastOldData;
    }

    public void clearLastNewData() {
        lastNewData = null;
    }

    public void clearLastOldData() {
        lastOldData = null;
    }

    public void setInvoked(boolean invoked) {
        isInvoked = invoked;
    }

    public boolean getAndClearIsInvoked() {
        boolean invoked = isInvoked;
        isInvoked = false;
        return invoked;
    }

    public void reset() {
        isInvoked = false;
        lastNewData = null;
        lastOldData = null;
    }

    private static final Logger log = LoggerFactory.getLogger(SupportBaseView.class);
}
