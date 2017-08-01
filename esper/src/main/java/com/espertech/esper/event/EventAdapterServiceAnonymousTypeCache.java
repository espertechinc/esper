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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class EventAdapterServiceAnonymousTypeCache {
    private final int size;
    private final Deque<EventTypeSPI> recentTypes;

    public EventAdapterServiceAnonymousTypeCache(int size) {
        this.size = size;
        recentTypes = new ArrayDeque<EventTypeSPI>(size);
    }

    public synchronized EventType addReturnExistingAnonymousType(EventType requiredType) {
        // only EventTypeSPI compliant implementations considered
        if (!(requiredType instanceof EventTypeSPI) || requiredType instanceof WrapperEventType) {
            return requiredType;
        }

        // check recent types
        for (EventTypeSPI existing : recentTypes) {
            if (existing.getClass() == requiredType.getClass() &&
                    Arrays.equals(requiredType.getPropertyNames(), existing.getPropertyNames()) &&
                    Arrays.equals(requiredType.getPropertyDescriptors(), existing.getPropertyDescriptors()) &&
                    existing.equalsCompareType(requiredType)) {
                return existing;
            }
        }

        // add, removing the oldest
        if (recentTypes.size() == size && !recentTypes.isEmpty()) {
            recentTypes.removeFirst();
        }
        if (recentTypes.size() < size) {
            recentTypes.addLast((EventTypeSPI) requiredType);
        }
        return requiredType;
    }
}
