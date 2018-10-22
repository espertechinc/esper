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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.EventBean;

import java.util.Map;

public class ContextControllerInitTermPartitionKey {
    private EventBean triggeringEvent;
    private Map<String, Object> triggeringPattern;
    private long startTime;
    private Long expectedEndTime;

    public ContextControllerInitTermPartitionKey() {
    }

    public ContextControllerInitTermPartitionKey(EventBean triggeringEvent, Map<String, Object> triggeringPattern, long startTime, Long expectedEndTime) {
        this.triggeringEvent = triggeringEvent;
        this.triggeringPattern = triggeringPattern;
        this.startTime = startTime;
        this.expectedEndTime = expectedEndTime;
    }

    public EventBean getTriggeringEvent() {
        return triggeringEvent;
    }

    public void setTriggeringEvent(EventBean triggeringEvent) {
        this.triggeringEvent = triggeringEvent;
    }

    public Map<String, Object> getTriggeringPattern() {
        return triggeringPattern;
    }

    public void setTriggeringPattern(Map<String, Object> triggeringPattern) {
        this.triggeringPattern = triggeringPattern;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getExpectedEndTime() {
        return expectedEndTime;
    }

    public void setExpectedEndTime(Long expectedEndTime) {
        this.expectedEndTime = expectedEndTime;
    }
}
