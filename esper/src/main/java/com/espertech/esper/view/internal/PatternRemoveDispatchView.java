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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.FlushedEventBuffer;
import com.espertech.esper.core.service.EPStatementDispatch;
import com.espertech.esper.pattern.EvalRootMatchRemover;
import com.espertech.esper.view.ViewSupport;

import java.util.*;

/**
 * View to handle pattern discarding for a single stream (no join).
 */
public final class PatternRemoveDispatchView extends ViewSupport implements EPStatementDispatch {
    private final EvalRootMatchRemover matchRemoveCallback;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;

    private boolean hasData = false;
    private FlushedEventBuffer newDataBuffer = new FlushedEventBuffer();

    public PatternRemoveDispatchView(EvalRootMatchRemover matchRemoveCallback, boolean suppressSameEventMatches, boolean discardPartialsOnMatch) {
        this.matchRemoveCallback = matchRemoveCallback;
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public final Iterator<EventBean> iterator() {
        return parent.iterator();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        newDataBuffer.add(newData);
        hasData = true;
    }

    public void execute() {
        if (hasData) {
            hasData = false;

            EventBean[] matches = newDataBuffer.getAndFlush();

            if (discardPartialsOnMatch) {
                Set<EventBean> events = new HashSet<EventBean>();
                for (EventBean match : matches) {
                    addEventsFromMatch(match, events);
                }
                if (events.size() > 0) {
                    matchRemoveCallback.removeMatch(events);
                }
            }

            if (suppressSameEventMatches && matches.length > 1) {
                Set<EventBean> events = new HashSet<EventBean>();
                addEventsFromMatch(matches[0], events);
                if (matches.length == 2) {
                    boolean overlaps = addEventsFromMatch(matches[1], events);
                    if (overlaps) {
                        matches = new EventBean[]{matches[0]};
                    }
                } else {
                    List<EventBean> matchesNonOverlapping = new ArrayList<EventBean>(matches.length);
                    matchesNonOverlapping.add(matches[0]);
                    for (int i = 1; i < matches.length; i++) {
                        Set<EventBean> eventsThisMatch = new HashSet<EventBean>();
                        eventsThisMatch.addAll(events);
                        boolean overlaps = addEventsFromMatch(matches[i], eventsThisMatch);
                        if (!overlaps) {
                            events.addAll(eventsThisMatch);
                            matchesNonOverlapping.add(matches[i]);
                        }
                    }
                    matches = matchesNonOverlapping.toArray(new EventBean[matchesNonOverlapping.size()]);
                }
            }

            this.updateChildren(matches, null);
        }
    }

    private boolean addEventsFromMatch(EventBean match, Set<EventBean> events) {
        EventPropertyDescriptor[] properties = match.getEventType().getPropertyDescriptors();
        boolean overlaps = false;

        for (EventPropertyDescriptor desc : properties) {
            Object prop = ((Map) match.getUnderlying()).get(desc.getPropertyName());
            if (prop == null) {
            } else if (prop instanceof EventBean) {
                overlaps |= !events.add((EventBean) prop);
            } else if (prop instanceof EventBean[]) {
                EventBean[] arr = (EventBean[]) prop;
                for (EventBean ele : arr) {
                    overlaps |= !events.add(ele);
                }
            }
        }

        return overlaps;
    }
}