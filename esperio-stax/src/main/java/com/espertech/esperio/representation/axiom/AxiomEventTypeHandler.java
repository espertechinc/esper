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
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.EventSender;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.plugin.PlugInEventTypeHandler;

/**
 * Handles a given Apache Axiom event type.
 * <p>
 * See {@link AxiomEventRepresentation} for more details.
 */
public class AxiomEventTypeHandler implements PlugInEventTypeHandler {
    private final AxiomXMLEventType eventType;

    /**
     * Ctor.
     *
     * @param eventType the event type
     */
    public AxiomEventTypeHandler(AxiomXMLEventType eventType) {
        this.eventType = eventType;
    }

    public EventType getType() {
        return eventType;
    }

    public EventSender getSender(EPRuntimeEventSender runtimeEventSender) {
        return new AxionEventSender(eventType, runtimeEventSender);
    }
}
