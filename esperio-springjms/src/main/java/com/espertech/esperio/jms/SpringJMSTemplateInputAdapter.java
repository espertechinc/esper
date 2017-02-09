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

import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.client.EPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.w3c.dom.Node;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Input adapter for receiving engine from the JMS world using Spring JMS templates and sending these to an engine.
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
                log.warn(".onMessage Event message not sent to engine, state of adapter is destroyed, message ack'd");
                message.acknowledge();
                return;
            }

            if (epServiceProviderSPI == null) {
                log.warn(".onMessage Event message not sent to engine, service provider not set yet, message ack'd");
                message.acknowledge();
                return;
            }

            synchronized (message) {
                Object theEvent = null;
                try {
                    theEvent = jmsMessageUnmarshaller.unmarshal(epServiceProviderSPI.getEventAdapterService(), message);
                } catch (RuntimeException ex) {
                    log.error("Failed to unmarshal event: " + ex.getMessage(), ex);
                }

                if (theEvent != null) {
                    if (theEvent instanceof Node) {
                        epServiceProviderSPI.getEPRuntime().sendEvent((Node) theEvent);
                    } else {
                        epServiceProviderSPI.getEPRuntime().sendEvent(theEvent);
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(".onMessage Event object not sent to engine: " + message.getJMSMessageID());
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
