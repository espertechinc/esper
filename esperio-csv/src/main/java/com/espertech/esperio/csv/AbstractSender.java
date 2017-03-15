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
package com.espertech.esperio.csv;

import com.espertech.esper.client.EPRuntime;

import java.util.Map;

/**
 * Sender that abstracts the send processin terms of threading or further pre-processing.
 */
public abstract class AbstractSender {

    /**
     * Runtime.
     */
    protected EPRuntime runtime;

    /**
     * Set the engine runtime to use.
     *
     * @param runtime runtime to use
     */
    public void setRuntime(EPRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Send an event
     *
     * @param theEvent   wrapper
     * @param beanToSend event object
     */
    public abstract void sendEvent(AbstractSendableEvent theEvent, Object beanToSend);

    /**
     * Send an event.
     *
     * @param theEvent      wrapper
     * @param mapToSend     event object
     * @param eventTypeName name of event type
     */
    public abstract void sendEvent(AbstractSendableEvent theEvent, Map mapToSend, String eventTypeName);

    /**
     * Indicate that sender should stop.
     */
    public abstract void onFinish();
}
