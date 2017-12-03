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
package com.espertech.esper.filterspec;

import com.espertech.esper.client.EventBean;

import java.util.HashMap;
import java.util.Map;

/**
 * Collection for internal use similar to the MatchedEventMap class in the client package
 * that holds the one or more events that could match any defined event expressions.
 * The optional tag value supplied when an event expression is created is used as a key for placing
 * matching event objects into this collection.
 */
public final class MatchedEventMapImpl implements MatchedEventMap {
    private final MatchedEventMapMeta meta;
    private final Object[] matches;

    /**
     * Constructor creates an empty collection of events.
     *
     * @param meta metadata
     */
    public MatchedEventMapImpl(MatchedEventMapMeta meta) {
        this.meta = meta;
        this.matches = new Object[meta.getTagsPerIndex().length];
    }

    public MatchedEventMapImpl(MatchedEventMapMeta meta, Object[] matches) {
        this.meta = meta;
        this.matches = matches;
    }

    /**
     * Add an event to the collection identified by the given tag.
     *
     * @param tag      is an identifier to retrieve the event from
     * @param theEvent is the event object or array of event object to be added
     */
    public void add(final int tag, final Object theEvent) {
        matches[tag] = theEvent;
    }

    /**
     * Returns a map containing the events where the key is the event tag string and the value is the event
     * instance.
     *
     * @return Hashtable containing event instances
     */
    public Object[] getMatchingEvents() {
        return matches;
    }

    /**
     * Returns a single event instance given the tag identifier, or null if the tag could not be located.
     *
     * @param tag is the identifier to look for
     * @return event instances for the tag
     */
    public EventBean getMatchingEvent(final int tag) {
        return (EventBean) matches[tag];
    }

    public Object getMatchingEventAsObject(final int tag) {
        return matches[tag];
    }

    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        int count = 0;

        for (int i = 0; i < matches.length; i++) {
            buffer.append(" (");
            buffer.append(count++);
            buffer.append(") ");
            buffer.append("tag=");
            buffer.append(meta.getTagsPerIndex()[i]);
            buffer.append("  event=");
            buffer.append(matches[i]);
        }

        return buffer.toString();
    }

    /**
     * Make a shallow copy of this collection.
     *
     * @return shallow copy
     */
    public MatchedEventMapImpl shallowCopy() {
        if (matches.length == 0) {
            return this;
        }

        Object[] copy = new Object[matches.length];
        if (matches.length > 1) {
            System.arraycopy(matches, 0, copy, 0, matches.length);
        } else {
            copy[0] = matches[0];
        }
        return new MatchedEventMapImpl(meta, copy);
    }

    /**
     * Merge the state of an other match event structure into this one by adding all entries
     * within the MatchedEventMap to this match event.
     *
     * @param other is the other instance to merge in.
     */
    public void merge(final MatchedEventMap other) {
        if (!(other instanceof MatchedEventMapImpl)) {
            throw new UnsupportedOperationException("Merge requires same types");
        }
        MatchedEventMapImpl otherImpl = (MatchedEventMapImpl) other;
        for (int i = 0; i < matches.length; i++) {
            if (otherImpl.matches[i] == null) {
                continue;
            }
            matches[i] = otherImpl.matches[i];
        }
    }

    public Map<String, Object> getMatchingEventsAsMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < meta.getTagsPerIndex().length; i++) {
            if (matches[i] == null) {
                continue;
            }
            map.put(meta.getTagsPerIndex()[i], matches[i]);
        }
        return map;
    }

    public MatchedEventMapMeta getMeta() {
        return meta;
    }

    public EventBean getMatchingEventByTag(String resultEventAsName) {
        Object obj = getMatchingEventAsObjectByTag(resultEventAsName);
        return (EventBean) obj;
    }

    public Object getMatchingEventAsObjectByTag(String key) {
        int index = meta.getTagFor(key);
        if (index == -1) {
            return null;
        }
        return matches[index];
    }
}
