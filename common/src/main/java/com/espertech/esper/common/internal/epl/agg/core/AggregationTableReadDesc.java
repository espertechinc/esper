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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EventType;

public class AggregationTableReadDesc {
    private final AggregationTableAccessAggReaderForge reader;
    private final EventType eventTypeCollection;
    private final Class componentTypeCollection;
    private final EventType eventTypeSingle;

    public AggregationTableReadDesc(AggregationTableAccessAggReaderForge reader, EventType eventTypeCollection, Class componentTypeCollection, EventType eventTypeSingle) {
        this.reader = reader;
        this.eventTypeCollection = eventTypeCollection;
        this.componentTypeCollection = componentTypeCollection;
        this.eventTypeSingle = eventTypeSingle;
    }

    public AggregationTableAccessAggReaderForge getReader() {
        return reader;
    }

    public EventType getEventTypeCollection() {
        return eventTypeCollection;
    }

    public Class getComponentTypeCollection() {
        return componentTypeCollection;
    }

    public EventType getEventTypeSingle() {
        return eventTypeSingle;
    }
}
