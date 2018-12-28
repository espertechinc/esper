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

public class AggregationMultiFunctionMethodDesc {
    private final AggregationMethodForge reader;
    private final EventType eventTypeCollection;
    private final Class componentTypeCollection;
    private final EventType eventTypeSingle;

    public AggregationMultiFunctionMethodDesc(AggregationMethodForge forge, EventType eventTypeCollection, Class componentTypeCollection, EventType eventTypeSingle) {
        this.reader = forge;
        this.eventTypeCollection = eventTypeCollection;
        this.componentTypeCollection = componentTypeCollection;
        this.eventTypeSingle = eventTypeSingle;
    }

    public AggregationMethodForge getReader() {
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
