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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

public class SupportCallEvent implements Serializable {
    private long callId;
    private String source;
    private String dest;
    private long startTime;
    private long endTime;

    public SupportCallEvent(long callId, String source, String destination, long startTime, long endTime) {
        this.callId = callId;
        this.source = source;
        this.dest = destination;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getCallId() {
        return callId;
    }

    public String getSource() {
        return source;
    }

    public String getDest() {
        return dest;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
