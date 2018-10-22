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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.util.AdapterSPI;
import com.espertech.esper.runtime.client.util.AdapterState;
import com.espertech.esper.runtime.client.util.AdapterStateManager;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements a JMS output adapter.
 */
public abstract class JMSOutputAdapter implements AdapterSPI {
    private EPRuntimeSPI spi;
    private long startTime;
    private final AdapterStateManager stateManager = new AdapterStateManager();
    private Map<String, JMSSubscription> subscriptionMap;
    private List<String> deploymentIds = new ArrayList<>(2);

    /**
     * Abstract send methods for marshalling and sending an event of to JMS care.
     *
     * @param eventBean            is the event
     * @param jmsAdapterMarshaller is the marshaller
     * @throws EPException when the send failed
     */
    public abstract void send(final EventBean eventBean, JMSMessageMarshaller jmsAdapterMarshaller) throws EPException;

    /**
     * Marshaller to use.
     */
    protected JMSMessageMarshaller jmsMessageMarshaller;

    /**
     * JMS Destination.
     */
    protected Destination destination;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Returns the JMS message marshaller.
     *
     * @return marshaller
     */
    public JMSMessageMarshaller getJmsMessageMarshaller() {
        return jmsMessageMarshaller;
    }

    /**
     * Sets the JMS message marshaller.
     *
     * @param jmsMessageMarshaller is the marshaller
     */
    public void setJmsMessageMarshaller(JMSMessageMarshaller jmsMessageMarshaller) {
        this.jmsMessageMarshaller = jmsMessageMarshaller;
    }

    /**
     * Sets the JMS destination.
     *
     * @param destination is the queue or topic
     */
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public void setSubscriptionMap(Map<String, JMSSubscription> subscriptionMap) {
        this.subscriptionMap = subscriptionMap;
    }

    public EPRuntime getRuntime() {
        return spi;
    }

    public void setRuntime(EPRuntime runtime) {
        if (runtime == null) {
            throw new IllegalArgumentException("Null runtime");
        }
        if (!(runtime instanceof EPRuntimeSPI)) {
            throw new IllegalArgumentException("Cannot downcast runtime to SPI");
        }
        spi = (EPRuntimeSPI) runtime;
    }

    public void start() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".start");
        }
        if (spi.getEventService() == null) {
            throw new EPException("Attempting to start an Adapter that hasn't had the runtime provided");
        }

        startTime = System.currentTimeMillis();
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".start startTime==" + startTime);
        }

        stateManager.start();
        Iterator<Map.Entry<String, JMSSubscription>> it = subscriptionMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, JMSSubscription> sub = it.next();
            JMSSubscription destination = sub.getValue();
            destination.setJMSOutputAdapter(this);

            EPDeployment deployment = compileDeploySubscription(spi, sub.getValue().getEventTypeName(), this.getClass().getSimpleName() + "-" + sub.getKey());
            deployment.getStatements()[0].addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    if (newEvents == null) {
                        return;
                    }
                    for (EventBean event : newEvents) {
                        destination.process(event);
                    }
                }
            });
        }
    }

    public void pause() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".pause");
        }
        stateManager.pause();
    }

    public void resume() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".resume");
        }
        stateManager.resume();
    }

    public void stop() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".stop");
        }
        stateManager.stop();
    }

    public void destroy() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".destroy");
        }

        for (String deployment : deploymentIds) {
            try {
                spi.getDeploymentService().undeploy(deployment);
            } catch (EPUndeployException e) {
                throw new EPException("Failed to undeploy: " + e.getMessage(), e);
            }
        }

        stateManager.destroy();
    }

    public AdapterState getState() {
        return stateManager.getState();
    }

    private EPDeployment compileDeploySubscription(EPRuntimeSPI runtime, String eventTypeName, String name) {
        try {
            String epl = "@name('" + name + "') select * from " + eventTypeName;
            CompilerArguments args = new CompilerArguments(runtime.getConfigurationDeepCopy());
            args.getPath().add(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            return runtime.getDeploymentService().deploy(compiled);
        } catch (Exception ex) {
            throw new EPException("Failed to compile and deploy subscription: " + ex.getMessage(), ex);
        }
    }
}
