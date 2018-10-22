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
package com.espertech.esper.common.client.hook.vdw;

/**
 * This event is raised when a virtual data window is stopped.
 */
public class VirtualDataWindowEventStopWindow extends VirtualDataWindowEvent {

    private final String namedWindowName;
    private final int agentInstanceId;

    /**
     * Ctor.
     *
     * @param namedWindowName named window name
     * @param agentInstanceId agent instance id
     */
    public VirtualDataWindowEventStopWindow(String namedWindowName, int agentInstanceId) {
        this.namedWindowName = namedWindowName;
        this.agentInstanceId = agentInstanceId;
    }

    /**
     * Returns the named window name.
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }

    /**
     * Returns the agent instance id
     *
     * @return agent instance id
     */
    public int getAgentInstanceId() {
        return agentInstanceId;
    }
}
