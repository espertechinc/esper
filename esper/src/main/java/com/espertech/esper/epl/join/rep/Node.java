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
package com.espertech.esper.epl.join.rep;

import com.espertech.esper.client.EventBean;

import java.util.Set;

/**
 * Node is the structure to hold results of event lookups in joined streams. A node holds a set of event which
 * are the result of a lookup in a stream's table. A Node can be linked to its parent node and the event within the
 * parent node, which was the event that was used to perform a lookup.
 */
public class Node {
    private final int stream;

    private Node parent;
    private EventBean parentEvent;
    private Set<EventBean> events;

    /**
     * Ctor.
     *
     * @param stream this node stores results for
     */
    public Node(int stream) {
        this.stream = stream;
    }

    /**
     * Returns the stream number of the stream that supplied the event results.
     *
     * @return stream number for results
     */
    public int getStream() {
        return stream;
    }

    /**
     * Returns the parent node, or null if this is a root node.
     *
     * @return parent node or null for root node
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Sets the parent node.
     *
     * @param parent to set
     */
    public void setParent(Node parent) {
        this.parent = parent;
    }

    /**
     * Returns lookup event.
     *
     * @return parent node's event that was used to lookup
     */
    public EventBean getParentEvent() {
        return parentEvent;
    }

    /**
     * Set the parent lookup (from stream) event whose results (to stream) are stored.
     *
     * @param parentEvent is the lookup event
     */
    public void setParentEvent(EventBean parentEvent) {
        this.parentEvent = parentEvent;
    }

    /**
     * Returns the results of the lookup.
     *
     * @return set of events
     */
    public Set<EventBean> getEvents() {
        return events;
    }

    /**
     * Store lookup results.
     *
     * @param events is a set of events
     */
    public void setEvents(Set<EventBean> events) {
        this.events = events;
    }
}
