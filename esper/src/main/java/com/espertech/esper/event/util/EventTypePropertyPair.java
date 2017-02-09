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
package com.espertech.esper.event.util;

import com.espertech.esper.client.EventType;

/**
 * Pair of event type and property.
 */
public class EventTypePropertyPair {
    private final String propertyName;
    private final EventType eventType;

    /**
     * Ctor.
     *
     * @param eventType    event type
     * @param propertyName property
     */
    public EventTypePropertyPair(EventType eventType, String propertyName) {
        this.eventType = eventType;
        this.propertyName = propertyName;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventTypePropertyPair that = (EventTypePropertyPair) o;

        if (!eventType.equals(that.eventType)) {
            return false;
        }
        if (!propertyName.equals(that.propertyName)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = propertyName.hashCode();
        result = 31 * result + eventType.hashCode();
        return result;
    }
}
