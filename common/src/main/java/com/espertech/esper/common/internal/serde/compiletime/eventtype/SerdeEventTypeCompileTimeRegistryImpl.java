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
package com.espertech.esper.common.internal.serde.compiletime.eventtype;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SerdeEventTypeCompileTimeRegistryImpl implements SerdeEventTypeCompileTimeRegistry {
    private final boolean isTargetHA;
    private final Map<EventType, DataInputOutputSerdeForge> eventTypes;

    public SerdeEventTypeCompileTimeRegistryImpl(boolean isTargetHA) {
        this.isTargetHA = isTargetHA;
        this.eventTypes = isTargetHA ? new HashMap<>() : Collections.emptyMap();
    }

    public boolean isTargetHA() {
        return isTargetHA;
    }

    public void addSerdeFor(EventType eventType, DataInputOutputSerdeForge forge) {
        if (isTargetHA) {
            eventTypes.put(eventType, forge);
        }
    }

    public Map<EventType, DataInputOutputSerdeForge> getEventTypes() {
        return eventTypes;
    }
}
