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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.util.AdapterState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.w3c.dom.Node;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Input adapter for receiving runtime from the JMS world using Spring JMS templates and sending these to an runtime.
 */
public class SpringJMSTemplateInputAdapter extends JMSInputAdapter
    implements MessageListener {
    private JmsTemplate jmsTemplate;

    private final Logger log = LoggerFactory.getLogger(getClass());

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
     * @param jmsTemplate is the jms template
     */
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void onMessage(Message message) {
        try {
            if (stateManager.getState() == AdapterState.DESTROYED) {
                log.warn(".onMessage Event message not sent to runtime, state of adapter is destroyed, message ack'd");
                message.acknowledge();
                return;
            }

            if (runtime == null) {
                log.warn(".onMessage Event message not sent to runtime, runtime not set yet, message ack'd");
                message.acknowledge();
                return;
            }

            synchronized (message) {
                Object theEvent = null;
                try {
                    theEvent = jmsMessageUnmarshaller.unmarshal(runtime, message);
                } catch (RuntimeException ex) {
                    log.error("Failed to unmarshal event: " + ex.getMessage(), ex);
                }

                if (theEvent != null) {
                    if (theEvent instanceof Node) {
                        Node node = (Node) theEvent;
                        runtime.getEventService().sendEventXMLDOM(node, node.getNodeName());
                    } else if (theEvent instanceof EventBean) {
                        runtime.getEventServiceSPI().processWrappedEvent((EventBean) theEvent);
                    } else {
                        runtime.getEventServiceSPI().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(".onMessage Event object not sent to runtime: " + message.getJMSMessageID());
                    }
                }

                message.acknowledge();
            }
        } catch (JMSException ex) {
            throw new EPException(ex);
        } catch (EPException ex) {
            log.error(".onMessage exception", ex);
            if (stateManager.getState() == AdapterState.STARTED) {
                stop();
            } else {
                destroy();
            }
        }
    }
}
