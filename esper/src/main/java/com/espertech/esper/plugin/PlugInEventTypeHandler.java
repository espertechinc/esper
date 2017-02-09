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
package com.espertech.esper.plugin;

import com.espertech.esper.client.EventSender;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPRuntimeEventSender;

/**
 * Provided once by an {@link PlugInEventRepresentation} for any event type it creates.
 */
public interface PlugInEventTypeHandler {
    /**
     * Returns the event type.
     *
     * @return event type.
     */
    public EventType getType();

    /**
     * Returns a facility responsible for converting or wrapping event objects.
     *
     * @param runtimeEventSender for sending events into the engine
     * @return sender
     */
    public EventSender getSender(EPRuntimeEventSender runtimeEventSender);
}
