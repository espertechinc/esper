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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class EventTypeForgablesPair {
    private final EventType eventType;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public EventTypeForgablesPair(EventType eventType, List<StmtClassForgeableFactory> additionalForgeables) {
        this.eventType = eventType;
        this.additionalForgeables = additionalForgeables;
    }

    public EventType getEventType() {
        return eventType;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
