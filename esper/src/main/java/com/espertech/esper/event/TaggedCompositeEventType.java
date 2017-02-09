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
package com.espertech.esper.event;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;

import java.util.Map;

/**
 * Interface for composite event type in which each property is itself an event.
 * <p>
 * For use with patterns in which pattern tags are properties in a result event and property values
 * are the event itself that is matching in a pattern.
 */
public interface TaggedCompositeEventType {
    /**
     * Returns the event types for each composing event.
     *
     * @return map of tag name and event type
     */
    public Map<String, Pair<EventType, String>> getTaggedEventTypes();
}
