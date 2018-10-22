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
package com.espertech.esperio.regression.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SupportJMSSender {
    private static final Logger log = LoggerFactory.getLogger(SupportJMSSender.class);

    private AbstractXmlApplicationContext springContext;
    private JmsTemplate jmsTemplate;

    public static void main(String[] args) throws JMSException {
        SupportJMSSender sender = new SupportJMSSender();
        //sender.sendSerializable("hello");

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("prop1", "a");
        sender.sendMap(values);

        sender.destroy();
        System.exit(1);
    }

    public SupportJMSSender() {
        springContext = new ClassPathXmlApplicationContext("regression/jms_activemq_spring.xml");
        jmsTemplate = (JmsTemplate) springContext.getBean("jmsTemplate");
    }

    public void sendSerializable(Serializable message) {
        MessageCreator creator = new MyObjectMessageCreator(message);
        jmsTemplate.send(creator);
    }

    public void sendMap(Map<String, Object> message) {
        MessageCreator creator = new MyMapMessageCreator(message);
        jmsTemplate.send(creator);
    }

    public void destroy() {
        springContext.destroy();
    }

    private class MyObjectMessageCreator implements MessageCreator {
        private Serializable msgObject;

        public MyObjectMessageCreator(Serializable msg) {
            this.msgObject = msg;
        }

        public Message createMessage(Session session) {
            if (log.isDebugEnabled()) {
                log.debug(".createMessage Creating object message for object " + msgObject.toString());
            }

            ObjectMessage msg = null;
            try {
                msg = session.createObjectMessage();
                msg.setObject(msgObject);
            } catch (JMSException ex) {
                log.error(".createMessage Error creating object message", ex);
            }
            return msg;
        }
    }

    private class MyMapMessageCreator implements MessageCreator {
        private Map<String, Object> msgObject;

        public MyMapMessageCreator(Map<String, Object> msg) {
            this.msgObject = msg;
        }

        public Message createMessage(Session session) {
            if (log.isDebugEnabled()) {
                log.debug(".createMessage Creating map message for map " + msgObject.toString());
            }

            MapMessage msg = null;
            try {
                msg = session.createMapMessage();
                for (String key : msgObject.keySet()) {
                    Object val = msgObject.get(key);
                    msg.setObject(key, val);
                }
            } catch (JMSException ex) {
                log.error(".createMessage Error creating map message", ex);
            }
            return msg;
        }
    }
}
