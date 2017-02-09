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

import com.espertech.esper.client.EventBean;

import javax.jms.Message;
import javax.jms.Session;

/**
 * Interface for a marshaller that creates a JMS message given a JMS session and event.
 */
public interface JMSMessageMarshaller {
    /**
     * Marshals the response out of the event bean.
     *
     * @param eventBean is the event to marshal
     * @param session   is the JMS session
     * @param timestamp is the timestamp to use
     * @return marshalled event as JMS message
     */
    public Message marshal(EventBean eventBean, Session session, long timestamp);

}
