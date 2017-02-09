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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.RollingEventBuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * "Prev" state for random access to event history.
 */
public class RegexPartitionStateRandomAccessImpl implements RegexPartitionStateRandomAccess {
    private final RegexPartitionStateRandomAccessGetter getter;
    private final Map<EventBean, EventBean[]> priorEventMap;
    private final RollingEventBuffer newEvents;
    private EventBean[] lastNew;

    /**
     * Ctor.
     *
     * @param getter for access
     */
    public RegexPartitionStateRandomAccessImpl(RegexPartitionStateRandomAccessGetter getter) {
        this.getter = getter;

        // Construct a rolling buffer of new data for holding max index + 1 (position 1 requires 2 events to keep)
        newEvents = new RollingEventBuffer(getter.getMaxPriorIndex() + 1);
        if (!getter.isUnbound()) {
            priorEventMap = new HashMap<EventBean, EventBean[]>();
        } else {
            priorEventMap = null;
        }
    }

    /**
     * Add new event.
     *
     * @param newEvent to add
     */
    public void newEventPrepare(EventBean newEvent) {
        // Add new event
        newEvents.add(newEvent);

        // Save prior index events in array
        EventBean[] priorEvents = new EventBean[getter.getIndexesRequestedLen()];
        for (int j = 0; j < priorEvents.length; j++) {
            int priorIndex = getter.getIndexesRequested()[j];
            priorEvents[j] = newEvents.get(priorIndex);
        }

        if (priorEventMap != null) {
            priorEventMap.put(newEvent, priorEvents);
        }

        lastNew = priorEvents;
        getter.setRandomAccess(this);
    }

    /**
     * Prepare relative to existing event, for iterating.
     *
     * @param newEvent to consider for index
     */
    public void existingEventPrepare(EventBean newEvent) {
        if (priorEventMap != null) {
            lastNew = priorEventMap.get(newEvent);
        }
        getter.setRandomAccess(this);
    }

    /**
     * Returns a previous event. Always immediatly preceded by #newEventPrepare.
     *
     * @param assignedRelativeIndex index
     * @return event
     */
    public EventBean getPreviousEvent(int assignedRelativeIndex) {
        if (lastNew == null) {
            return null;
        }
        return lastNew[assignedRelativeIndex];
    }

    /**
     * Remove events.
     *
     * @param oldEvents to remove
     */
    public void remove(EventBean[] oldEvents) {
        if (oldEvents == null) {
            return;
        }
        for (int i = 0; i < oldEvents.length; i++) {
            remove(oldEvents[i]);
        }
    }

    /**
     * Remove event.
     *
     * @param oldEvent to remove
     */
    public void remove(EventBean oldEvent) {
        if (priorEventMap != null) {
            priorEventMap.remove(oldEvent);
        }
    }

    /**
     * Returns true for empty collection.
     *
     * @return indicator if empty
     */
    public boolean isEmpty() {
        if (priorEventMap != null) {
            priorEventMap.isEmpty();
        }
        return true;
    }
}