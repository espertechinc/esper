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
package com.espertech.esper.client.context;

/**
 * The variable state for a context partitioned variable.
 */
public class ContextPartitionVariableState {
    private final int agentInstanceId;
    private final ContextPartitionIdentifier identifier;
    private final Object state;

    /**
     * Ctor.
     *
     * @param agentInstanceId agent instance id
     * @param identifier      context partition identification
     * @param state           variable state
     */
    public ContextPartitionVariableState(int agentInstanceId, ContextPartitionIdentifier identifier, Object state) {
        this.agentInstanceId = agentInstanceId;
        this.identifier = identifier;
        this.state = state;
    }

    /**
     * Returns the agent instance id
     *
     * @return id
     */
    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    /**
     * Returns context partition identifier
     *
     * @return context partition info
     */
    public ContextPartitionIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns the variable state
     *
     * @return state
     */
    public Object getState() {
        return state;
    }
}
