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
package com.espertech.esper.supportregression.patternassert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contains a set of events to send to the runtime for testing along with a time for each event.
 * Each event has a string event id that can be obtained via the getParentEvent method.
 */
public class EventCollection {
    public final static String ON_START_EVENT_ID = "ON_START_ID";

    // Ordered map of string event id and event object
    // Events will be sent in the ordering maintained.
    private LinkedHashMap<String, Object> testEvents;

    // Optional time for each event
    private Map<String, Long> testEventTimes;

    public EventCollection(LinkedHashMap<String, Object> testEvents,
                           Map<String, Long> testEventTimes) {
        this.testEvents = testEvents;
        this.testEventTimes = testEventTimes;
    }

    public Object getEvent(String eventId) {
        if (!testEvents.containsKey(eventId)) {
            throw new IllegalArgumentException("Event id " + eventId + " not found in data set");
        }
        return testEvents.get(eventId);
    }

    public Long getTime(String eventId) {
        return testEventTimes.get(eventId);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return testEvents.entrySet();
    }
}
