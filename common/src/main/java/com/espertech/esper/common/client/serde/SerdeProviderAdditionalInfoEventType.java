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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

/**
 * Information about the event type for which to obtain a serde.
 */
public class SerdeProviderAdditionalInfoEventType extends SerdeProviderAdditionalInfo {
    private final String eventTypeName;
    private final EventType[] eventTypeSupertypes;

    /**
     * Ctor.
     * @param raw statement info
     * @param eventTypeName       event type name
     * @param eventTypeSupertypes optional supertypes
     */
    public SerdeProviderAdditionalInfoEventType(StatementRawInfo raw, String eventTypeName, EventType[] eventTypeSupertypes) {
        super(raw);
        this.eventTypeName = eventTypeName;
        this.eventTypeSupertypes = eventTypeSupertypes;
    }

    /**
     * Returns the event type name if provided
     *
     * @return type name
     */
    public String getEventTypeName() {
        return eventTypeName;
    }

    /**
     * Returns supertypes when available.
     *
     * @return supertypes
     */
    public EventType[] getEventTypeSupertypes() {
        return eventTypeSupertypes;
    }

    public String toString() {
        return "event-type '" + eventTypeName + '\'';
    }
}
