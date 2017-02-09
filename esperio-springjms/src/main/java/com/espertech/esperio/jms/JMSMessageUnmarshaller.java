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
package com.espertech.esperio.jms;

import com.espertech.esper.client.EPException;
import com.espertech.esper.event.EventAdapterService;

import javax.jms.Message;

/**
 * Interface for a un-marshaller that takes a JMS message and creates or wraps an event object for use to
 * send as an event into an engine instance.
 */
public interface JMSMessageUnmarshaller {
    /**
     * Unmarshal the given JMS message into an object for sending into the engine.
     *
     * @param eventAdapterService is the wrapper service for events
     * @param message             is the message to unmarshal
     * @return event to send to engine
     * @throws EPException if the unmarshal operation failed
     */
    public Object unmarshal(EventAdapterService eventAdapterService, Message message) throws EPException;
}
