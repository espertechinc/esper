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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.event.core.SendableEvent;

import java.util.Map;

public class SendableEventMap implements SendableEvent {
    private final Map<String, Object> event;
    private final String typeName;

    public SendableEventMap(Map<String, Object> event, String typeName) {
        this.event = event;
        this.typeName = typeName;
    }

    public void send(EventServiceSendEventCommon eventService) {
        eventService.sendEventMap(event, typeName);
    }

    public Object getUnderlying() {
        return event;
    }
}
