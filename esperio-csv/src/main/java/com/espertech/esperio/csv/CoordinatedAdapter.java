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

import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;

/**
 * An Adapter that can be coordinated by an AdapterCoordinator.
 */
public interface CoordinatedAdapter extends InputAdapter {
    /**
     * Get the next event in line to be sent into the runtime , or null if there is no available event.
     *
     * @return an instance of SendableEvent that wraps the next event to send, or null if none
     * @throws EPException in case of errors creating the event
     */
    public SendableEvent read() throws EPException;

    /**
     * Set the usingEngineThread value
     *
     * @param usingEngineThread - the value to set
     */
    public void setUsingEngineThread(boolean usingEngineThread);

    /**
     * Set the usingExternalTimer value
     *
     * @param usingExternalTimer - the value to set
     */
    public void setUsingExternalTimer(boolean usingExternalTimer);

    /**
     * Disallow subsequent state changes and throw an IllegalStateTransitionException
     * if they are attempted.
     */
    public void disallowStateTransitions();

    /**
     * Set the scheduleSlot for this Adapter.
     *
     * @param scheduleSlot - the scheduleSlot to set
     */
    public void setScheduleSlot(long scheduleSlot);

    /**
     * Set the epService
     *
     * @param epService - the value to set
     */
    public void setEPService(EPServiceProvider epService);
}
