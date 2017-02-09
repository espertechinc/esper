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
package com.espertech.esper.client.hook;

/**
 * Base class for events indicating a named-window consumer management.
 */
public abstract class VirtualDataWindowEventConsumerBase extends VirtualDataWindowEvent {

    private final String namedWindowName;
    private final Object consumerObject;
    private final String statementName;
    private final int agentInstanceId;

    /**
     * Ctor.
     *
     * @param namedWindowName the named window name
     * @param consumerObject  an object that identifies the consumer, the same instance or the add and for the remove event
     * @param statementName   statement name
     * @param agentInstanceId agent instance id
     */
    public VirtualDataWindowEventConsumerBase(String namedWindowName, Object consumerObject, String statementName, int agentInstanceId) {
        this.namedWindowName = namedWindowName;
        this.consumerObject = consumerObject;
        this.statementName = statementName;
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
     * Returns an object that serves as a unique identifier for the consumer, with multiple consumer per statements possible.
     * <p>
     * Upon remove the removal event contains the same consumer object.
     * </p>
     *
     * @return consumer object
     */
    public Object getConsumerObject() {
        return consumerObject;
    }

    /**
     * Returns the statement name.
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the agent instance id (context partition id).
     *
     * @return agent instance id
     */
    public int getAgentInstanceId() {
        return agentInstanceId;
    }
}
