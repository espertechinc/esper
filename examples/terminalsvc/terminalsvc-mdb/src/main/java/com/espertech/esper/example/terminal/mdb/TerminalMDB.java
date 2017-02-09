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
package com.espertech.esper.example.terminal.mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

@MessageDriven(name = "TerminalMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue_b"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
public class TerminalMDB implements MessageListener {
    private static OutboundQueueSender outboundQueueSender;
    private static EPServiceMDBAdapter mdbAdapter;
    private static Boolean isInitialized = new Boolean(false);

    public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) throws EJBException {
        // System.out.println(TerminalMDB.class.getName() + "::setMessageDrivenContext invoked");
    }

    public void ejbCreate() throws EJBException {
        // System.out.println(TerminalMDB.class.getName() + "::ejbCreate invoked");

        synchronized (isInitialized) {
            if (!isInitialized) {
                System.out.println(TerminalMDB.class.getName() + "::ejbCreate initializing sender and engine");

                // Connect to outbound queue
                outboundQueueSender = new OutboundQueueSender();

                // Get engine instance - same engine instance for all MDB instances
                mdbAdapter = new EPServiceMDBAdapter(outboundQueueSender);

                isInitialized = true;
            }
        }
    }

    public void ejbRemove() throws EJBException {
        System.out.println(TerminalMDB.class.getName() + "::ejbRemove invoked");
    }

    public void onMessage(Message message) {
        Object theEvent = null;
        try {
            ObjectMessage objMessage = (ObjectMessage) message;
            // System.out.println("onMessage received event=" + objMessage.getObject());
            theEvent = objMessage.getObject();
        } catch (JMSException ex) {
            String messageText = "Error sending response message";
            System.out.println(messageText + ":" + ex);
            return;
        }

        try {
            mdbAdapter.sendEvent(theEvent);
        } catch (RuntimeException ex) {
            String messageText = "Error processing event, event=" + theEvent;
            System.out.println(messageText + ":" + ex);
            return;
        }
    }
}
