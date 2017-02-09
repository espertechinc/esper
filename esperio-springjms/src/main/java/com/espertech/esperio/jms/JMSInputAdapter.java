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

import com.espertech.esper.adapter.AdapterSPI;
import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.adapter.AdapterStateManager;
import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created for ESPER.
 */
public abstract class JMSInputAdapter implements InputAdapter, AdapterSPI {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Manages adapter state.
     */
    protected final AdapterStateManager stateManager = new AdapterStateManager();

    /**
     * Engine services.
     */
    protected EPServiceProviderSPI epServiceProviderSPI;

    /**
     * Start time.
     */
    protected long startTime;

    /**
     * Unmarshaller for JMS messages.
     */
    protected JMSMessageUnmarshaller jmsMessageUnmarshaller;

    /**
     * Returns the unmarshaller.
     *
     * @return unmarshaller
     */
    public JMSMessageUnmarshaller getJmsMessageUnmarshaller() {
        return jmsMessageUnmarshaller;
    }

    /**
     * Sets the unmarshaller to use.
     *
     * @param jmsMessageUnmarshaller is the unmarshaller to use
     */
    public void setJmsMessageUnmarshaller(
            JMSMessageUnmarshaller jmsMessageUnmarshaller) {
        this.jmsMessageUnmarshaller = jmsMessageUnmarshaller;
    }

    public EPServiceProvider getEPServiceProvider() {
        return epServiceProviderSPI;
    }

    public void setEPServiceProvider(EPServiceProvider epService) {
        if (epService == null) {
            throw new IllegalArgumentException("Null service provider");
        }
        if (!(epService instanceof EPServiceProviderSPI)) {
            throw new IllegalArgumentException("Cannot downcast service provider to SPI");
        }
        epServiceProviderSPI = (EPServiceProviderSPI) epService;
    }

    public void start() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".start");
        }
        if (epServiceProviderSPI.getEPRuntime() == null) {
            throw new EPException(
                    "Attempting to start an Adapter that hasn't had the epService provided");
        }

        startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug(".start startTime==" + startTime);
        }
        stateManager.start();
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
        stateManager.destroy();
    }

    public AdapterState getState() {
        return stateManager.getState();
    }
}
