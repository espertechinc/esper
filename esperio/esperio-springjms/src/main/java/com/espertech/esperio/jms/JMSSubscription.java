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

import com.espertech.esper.common.client.EventBean;

/**
 * Represents the JMS-aspects of a subscription.
 */
public class JMSSubscription {
    private String eventTypeName;
    private JMSMessageMarshaller jmsMessageMarshaller;
    private JMSOutputAdapter jmsOutputAdapter;

    /**
     * Empty Ctor required for use with Spring.
     */
    public JMSSubscription() {
    }

    /**
     * Returns the marshaller to use for this subscription.
     *
     * @return marshaller
     */
    public JMSMessageMarshaller getJmsMessageMarshaller() {
        return jmsMessageMarshaller;
    }

    /**
     * Sets the marshaller to use for this subscription.
     *
     * @param jmsMessageMarshaller to use
     */
    public void setJmsMessageMarshaller(JMSMessageMarshaller jmsMessageMarshaller) {
        this.jmsMessageMarshaller = jmsMessageMarshaller;
    }

    public void setJMSOutputAdapter(JMSOutputAdapter adapter) {
        this.jmsOutputAdapter = adapter;
    }

    public void process(EventBean theEvent) {
        jmsOutputAdapter.send(theEvent, jmsMessageMarshaller);
    }

    public String getEventTypeName() {
        return eventTypeName;
    }

    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public JMSOutputAdapter getJmsOutputAdapter() {
        return jmsOutputAdapter;
    }

    public void setJmsOutputAdapter(JMSOutputAdapter jmsOutputAdapter) {
        this.jmsOutputAdapter = jmsOutputAdapter;
    }
}
