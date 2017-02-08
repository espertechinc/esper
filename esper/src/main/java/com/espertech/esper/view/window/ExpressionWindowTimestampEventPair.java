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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;

public class ExpressionWindowTimestampEventPair {
    private final long timestamp;
    private final EventBean theEvent;

    public ExpressionWindowTimestampEventPair(long timestamp, EventBean theEvent) {
        this.timestamp = timestamp;
        this.theEvent = theEvent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventBean getTheEvent() {
        return theEvent;
    }
}
