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
package com.espertech.esper.supportregression.plugineventrep;

import com.espertech.esper.client.EventSender;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.plugin.PlugInEventTypeHandler;

public class MyPlugInPropertiesEventTypeHandler implements PlugInEventTypeHandler {
    private final MyPlugInPropertiesEventType eventType;

    public MyPlugInPropertiesEventTypeHandler(MyPlugInPropertiesEventType eventType) {
        this.eventType = eventType;
    }

    public EventSender getSender(EPRuntimeEventSender runtimeEventSender) {
        return new MyPlugInPropertiesEventSender(eventType, runtimeEventSender);
    }

    public EventType getType() {
        return eventType;
    }
}
