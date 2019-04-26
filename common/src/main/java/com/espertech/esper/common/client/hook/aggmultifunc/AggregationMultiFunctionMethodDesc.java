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
package com.espertech.esper.common.client.hook.aggmultifunc;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;

/**
 * Aggregation multi-function return-type descriptor
 */
public class AggregationMultiFunctionMethodDesc {
    private final AggregationMethodForge reader;
    private final EventType eventTypeCollection;
    private final Class componentTypeCollection;
    private final EventType eventTypeSingle;

    /**
     * Ctor.
     * @param forge the forge of the aggregation value reader
     * @param eventTypeCollection when returning a collection of events, the event type or null if not returning a collection of events
     * @param componentTypeCollection when returning a collection of object values, the type of the values or null if not returning a collection of values
     * @param eventTypeSingle when returning a single event, the event type or null if not returning a single event
     */
    public AggregationMultiFunctionMethodDesc(AggregationMethodForge forge, EventType eventTypeCollection, Class componentTypeCollection, EventType eventTypeSingle) {
        this.reader = forge;
        this.eventTypeCollection = eventTypeCollection;
        this.componentTypeCollection = componentTypeCollection;
        this.eventTypeSingle = eventTypeSingle;
    }

    /**
     * Returns the forge of the aggregation value reader
     * @return forge
     */
    public AggregationMethodForge getReader() {
        return reader;
    }

    /**
     * Returns, when returning a collection of events, the event type or null if not returning a collection of events
     * @return event type
     */
    public EventType getEventTypeCollection() {
        return eventTypeCollection;
    }

    /**
     * Returns, when returning a collection of object values, the type of the values or null if not returning a collection of values
     * @return type
     */
    public Class getComponentTypeCollection() {
        return componentTypeCollection;
    }

    /**
     * Returns, when returning a single event, the event type or null if not returning a single event
     * @return event type
     */
    public EventType getEventTypeSingle() {
        return eventTypeSingle;
    }
}
