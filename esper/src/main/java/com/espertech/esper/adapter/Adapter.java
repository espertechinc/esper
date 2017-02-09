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
package com.espertech.esper.adapter;

import com.espertech.esper.client.EPException;

/**
 * An Adapter takes some external data, converts it into events, and sends it
 * into the runtime engine.
 */
public interface Adapter {
    /**
     * Start the sending of events into the runtime egine.
     *
     * @throws EPException in case of errors processing the events
     */
    void start() throws EPException;

    /**
     * Pause the sending of events after a Adapter has been started.
     *
     * @throws EPException if this Adapter has already been stopped
     */
    void pause() throws EPException;

    /**
     * Resume sending events after the Adapter has been paused.
     *
     * @throws EPException in case of errors processing the events
     */
    void resume() throws EPException;

    /**
     * Stop sending events and return the Adapter to the OPENED state, ready to be
     * started once again.
     *
     * @throws EPException in case of errors releasing resources
     */
    void stop() throws EPException;

    /**
     * Destroy the Adapter, stopping the sending of all events and releasing all
     * the resources, and disallowing any further state changes on the Adapter.
     *
     * @throws EPException to indicate errors during destroy
     */
    void destroy() throws EPException;

    /**
     * Get the state of this Adapter.
     *
     * @return state
     */
    AdapterState getState();
}
