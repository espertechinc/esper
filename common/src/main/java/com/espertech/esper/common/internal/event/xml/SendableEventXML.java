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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.event.core.SendableEvent;
import org.w3c.dom.Node;

public class SendableEventXML implements SendableEvent {
    private final Node event;
    private final String typeName;

    public SendableEventXML(Node event, String typeName) {
        this.event = event;
        this.typeName = typeName;
    }

    public void send(EventServiceSendEventCommon eventService) {
        eventService.sendEventXMLDOM(event, typeName);
    }

    public Object getUnderlying() {
        return event;
    }
}
