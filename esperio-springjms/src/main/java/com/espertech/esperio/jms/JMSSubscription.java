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

import com.espertech.esper.adapter.BaseSubscription;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.filter.FilterHandleCallback;

import java.util.Collection;

/**
 * Represents the JMS-aspects of a subscription.
 */
public class JMSSubscription extends BaseSubscription {
    private JMSMessageMarshaller jmsMessageMarshaller;

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
        this.adapter = adapter;
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {

        if (!(adapter instanceof JMSOutputAdapter)) {
            return;
        }
        ((JMSOutputAdapter) adapter).send(theEvent, jmsMessageMarshaller);
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getStatementId() {
        return -1;
    }
}
