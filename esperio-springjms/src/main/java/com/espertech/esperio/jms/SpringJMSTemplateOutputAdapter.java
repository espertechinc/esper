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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.Message;
import javax.jms.Session;

/**
 * Output adapter for sending engine events out into the JMS world using Spring JMS templates.
 */
public class SpringJMSTemplateOutputAdapter extends JMSOutputAdapter {
    private JmsTemplate jmsTemplate;
    private SpringMessageCreator messageCreator;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Returns the jms template.
     *
     * @return Spring JMS template
     */
    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    /**
     * Sets the Spring JMS template
     *
     * @param jmsTemplate to set
     */
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void send(final EventBean eventBean,
                     JMSMessageMarshaller jmsMessageMarshaller) throws EPException {
        if (jmsTemplate != null) {
            if (messageCreator == null) {
                messageCreator = new SpringMessageCreator();
            }
            messageCreator.setMessageParameters(
                    eventBean, (jmsMessageMarshaller != null) ?
                            jmsMessageMarshaller :
                            this.jmsMessageMarshaller);
            if (destination != null) {
                jmsTemplate.send(destination, messageCreator);
            } else {
                jmsTemplate.send(messageCreator);
            }
        }
    }

    private class SpringMessageCreator implements MessageCreator {
        EventBean eventBean;
        JMSMessageMarshaller jmsMessageMarshaller;

        public void setMessageParameters(EventBean eventBean,
                                         JMSMessageMarshaller jmsMessageMarshaller) {
            this.eventBean = eventBean;
            this.jmsMessageMarshaller = jmsMessageMarshaller;
        }

        public Message createMessage(Session session) {
            if ((eventBean == null) || (jmsMessageMarshaller == null)) {
                return null;
            }
            Message msg =
                    jmsMessageMarshaller.marshal(eventBean, session, System.currentTimeMillis());
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Creating jms message from event." + msg.toString());
            }
            return msg;
        }
    }
}
