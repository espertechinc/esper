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
 * For use with high-availability and scale-out only, this class provides contextual information about the event type that we
 * looking to serialize or de-serialize, for use with {@link SerdeProvider}
 */
public class SerdeProviderEventTypeContext extends SerdeProviderAdditionalInfo {
    private final EventType eventType;

    /**
     * Ctor.
     * @param raw statement information
     * @param eventType event type
     */
    public SerdeProviderEventTypeContext(StatementRawInfo raw, EventType eventType) {
        super(raw);
        this.eventType = eventType;
    }

    /**
     * Returns the event type
     * @return event type
     */
    public EventType getEventType() {
        return eventType;
    }
}
