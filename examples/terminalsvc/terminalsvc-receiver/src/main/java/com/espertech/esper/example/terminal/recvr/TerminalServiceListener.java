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
package com.espertech.esper.example.terminal.recvr;

import javax.jms.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TerminalServiceListener implements MessageListener {
    public void onMessage(Message message) {
        try {
            DateFormat dateFormat = SimpleDateFormat.getInstance();
            String date = dateFormat.format(new Date());

            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                System.out.println(date + " " + textMessage.getText());
            } else {
                ObjectMessage objectMessage = (ObjectMessage) message;
                System.out.println(date + " " + objectMessage.getObject());
            }
        } catch (JMSException ex) {
            System.out.println("Error reading text message:" + ex);
        }
    }
}
