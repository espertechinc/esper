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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.util.ThreadLogUtil;

public class LogUpdateListener implements UpdateListener {
    private String fieldNameLogged;

    public LogUpdateListener(String fieldNameLogged) {
        this.fieldNameLogged = fieldNameLogged;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        EventBean theEvent = newEvents[0];
        if (fieldNameLogged == null) {
            ThreadLogUtil.trace("listener received, " + " listener=" + this + " eventUnderlying=" + Integer.toHexString(theEvent.getUnderlying().hashCode()));
        } else {
            ThreadLogUtil.trace("listener received, " + " listener=" + this + " eventUnderlying=" + Integer.toHexString(theEvent.get("a").hashCode()));
        }
    }
}
