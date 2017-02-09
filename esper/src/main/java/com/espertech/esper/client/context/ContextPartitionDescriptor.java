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
 * Descriptor encapsulates information about a context partition.
 */
public class ContextPartitionDescriptor {
    private final int agentInstanceId;
    private final ContextPartitionIdentifier identifier;
    private ContextPartitionState state;

    /**
     * Ctor.
     *
     * @param agentInstanceId context partition id
     * @param identifier      identifier object specific to context declaration
     * @param state           current state
     */
    public ContextPartitionDescriptor(int agentInstanceId, ContextPartitionIdentifier identifier, ContextPartitionState state) {
        this.agentInstanceId = agentInstanceId;
        this.identifier = identifier;
        this.state = state;
    }

    /**
     * Returns the context partition id.
     *
     * @return id
     */
    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    /**
     * Returns an identifier object that identifies the context partition.
     *
     * @return identifier
     */
    public ContextPartitionIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns context partition state.
     *
     * @return state
     */
    public ContextPartitionState getState() {
        return state;
    }

    /**
     * Convenience method for updating context partition state, does not affect actual context partition state.
     *
     * @param state of context partition
     */
    public void setState(ContextPartitionState state) {
        this.state = state;
    }
}
