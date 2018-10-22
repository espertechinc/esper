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

import javax.ejb.EJBException;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class OutboundQueueSender implements OutboundSender {
    private QueueSession session;
    private QueueSender sender;

    public OutboundQueueSender() throws EJBException {
        try {
            // Connect to outbound queue
            InitialContext iniCtx = new InitialContext();
            Object tmp = iniCtx.lookup("java:/ConnectionFactory");
            QueueConnectionFactory qcf = (QueueConnectionFactory) tmp;
            QueueConnection conn = qcf.createQueueConnection();
            Queue queA = (Queue) iniCtx.lookup("queue_a");
            session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            conn.start();
            sender = session.createSender(queA);
        } catch (NamingException ex) {
            String message = "Error looking up outbound queue";
            System.out.println(message + ": " + ex);
            throw new EJBException(message, ex);
        } catch (JMSException ex) {
            String message = "Error connecting to outbound queue";
            System.out.println(message + ": " + ex);
            throw new EJBException(message, ex);
        }
    }

    public void send(String text) {
        try {
            Message response = session.createTextMessage(text);
            sender.send(response);
        } catch (JMSException ex) {
            String messageText = "Error sending response message";
            System.out.println(messageText + ": " + ex);
        }
    }

    public void destroy() {
        try {
            sender.close();
            session.close();
        } catch (JMSException ex) {
            String messageText = "Error closing outbound sender";
            System.out.println(messageText + ": " + ex);
        }
    }
}
