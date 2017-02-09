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
package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventBean;

import java.util.Set;

/**
 * Represents a lookup strategy object that an EPL statement that queries a virtual data window obtains
 * to perform read operations into the virtual data window.
 * <p>
 * An instance is associated to each EPL statement querying (join, subquery, on-action etc.) the virtual data window.
 * <p>
 * Optimally an implementation returns only those rows matching the complete lookup context filtered field information.*
 * </p>
 * <p>
 * It is legal for an implementation to return rows that are not matching lookup context filter field information.
 * Such rows are removed by where-clause criteria, when provided.
 * </p>
 */
public interface VirtualDataWindowLookup {

    /**
     * Invoked by an EPL statement that queries a virtual data window to perform a lookup.
     * <p>
     * Keys passed are the actual query lookup values. For range lookups, the key
     * passed is an instance of {@link VirtualDataWindowKeyRange}.
     * <p>
     * Key values follow {@link VirtualDataWindowLookupContext}.
     * </p>
     * <p>
     * EventsPerStream contains the events participating in the subquery or join.
     * It is not necessary to use eventsPerStream and the events
     * are provided for additional information.
     * Please consider eventsPerStream for Esper internal use.
     * </p>
     *
     * @param keys            lookup values
     * @param eventsPerStream input events for the lookup
     * @return set of events
     */
    public Set<EventBean> lookup(Object[] keys, EventBean[] eventsPerStream);
}
