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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.util.NullableObject;

import java.util.Set;

/**
 * Interface for a plug-in to {@link VariableManagementService} to handle variable persistent state.
 */
public interface VariableStateNonConstHandler {
    void addVariable(String deploymentId, String variableName, Variable variable, DataInputOutputSerde serde);

    /**
     * Returns the current variable state plus Boolean.TRUE if there is a current state since the variable
     * may have the value of null; returns Boolean.FALSE and null if there is no current state
     *
     * @param variable        variable
     * @param agentInstanceId agent instance id
     * @return indicator whether the variable is known and it's state, or whether it doesn't have state (false)
     */
    NullableObject<Object> getHasState(Variable variable, int agentInstanceId);

    /**
     * Sets the new variable value
     *
     * @param variable        variable
     * @param agentInstanceId agent instance id
     * @param newValue        new variable value, null values allowed
     */
    void setState(Variable variable, int agentInstanceId, Object newValue);

    void removeState(Variable variable, int agentInstanceId);

    void removeVariable(Variable variable, String deploymentId, Set<Integer> cps);
}
