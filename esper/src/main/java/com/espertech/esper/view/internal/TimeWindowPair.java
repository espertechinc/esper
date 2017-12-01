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

public final class TimeWindowPair {
    private long timestamp;
    private Object eventHolder;

    public TimeWindowPair(long timestamp, Object eventHolder) {
        this.timestamp = timestamp;
        this.eventHolder = eventHolder;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Object getEventHolder() {
        return eventHolder;
    }

    public void setEventHolder(Object eventHolder) {
        this.eventHolder = eventHolder;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
