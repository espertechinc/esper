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
package com.espertech.esper.common.client.variable;

import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionVariableState;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for variable management.
 */
public interface EPVariableService {
    /**
     * Returns the current variable value for a global variable. A null value is a valid value for a variable.
     * Not for use with context-partitioned variables.
     *
     * @param deploymentId deployment id
     * @param variableName is the name of the variable to return the value for
     * @return current variable value
     * @throws VariableNotFoundException if a variable by that name has not been declared
     */
    Object getVariableValue(String deploymentId, String variableName) throws VariableNotFoundException;

    /**
     * Returns the current variable values for a context-partitioned variable, per context partition.
     * A null value is a valid value for a variable.
     * Only for use with context-partitioned variables.
     * Variable names provided must all be associated to the same context partition.
     *
     * @param variableNames            are the names of the variables to return the value for
     * @param contextPartitionSelector selector for the context partition to return the value for
     * @return current variable value
     * @throws VariableNotFoundException if a variable by that name has not been declared
     */
    Map<DeploymentIdNamePair, List<ContextPartitionVariableState>> getVariableValue(Set<DeploymentIdNamePair> variableNames, ContextPartitionSelector contextPartitionSelector) throws VariableNotFoundException;

    /**
     * Returns current variable values for each of the global variable names passed in,
     * guaranteeing consistency in the face of concurrent updates to the variables.
     * Not for use with context-partitioned variables.
     *
     * @param variableNames is a set of variable names for which to return values
     * @return map of variable name and variable value
     * @throws VariableNotFoundException if any of the variable names has not been declared
     */
    Map<DeploymentIdNamePair, Object> getVariableValue(Set<DeploymentIdNamePair> variableNames) throws VariableNotFoundException;

    /**
     * Returns current variable values for all global variables,
     * guaranteeing consistency in the face of concurrent updates to the variables.
     * Not for use with context-partitioned variables.
     *
     * @return map of variable name and variable value
     */
    Map<DeploymentIdNamePair, Object> getVariableValueAll();

    /**
     * Sets the value of a single global variable.
     * <p>
     * Note that the thread setting the variable value queues the changes, i.e. it does not itself
     * re-evaluate such new variable value for any given statement. The timer thread performs this work.
     * </p>
     * Not for use with context-partitioned variables.
     *
     * @param deploymentId  deployment id
     * @param variableName  is the name of the variable to change the value of
     * @param variableValue is the new value of the variable, with null an allowed value
     * @throws VariableValueException    if the value does not match variable type or cannot be safely coerced
     *                                   to the variable type
     * @throws VariableNotFoundException if the variable name has not been declared
     */
    void setVariableValue(String deploymentId, String variableName, Object variableValue) throws VariableValueException, VariableNotFoundException;

    /**
     * Sets the value of multiple global variables in one update, applying all or none of the changes
     * to variable values in one atomic transaction.
     * <p>
     * Note that the thread setting the variable value queues the changes, i.e. it does not itself
     * re-evaluate such new variable value for any given statement. The timer thread performs this work.
     * </p>
     * Not for use with context-partitioned variables.
     *
     * @param variableValues is the map of variable name and variable value, with null an allowed value
     * @throws VariableValueException    if any value does not match variable type or cannot be safely coerced
     *                                   to the variable type
     * @throws VariableNotFoundException if any of the variable names has not been declared
     */
    void setVariableValue(Map<DeploymentIdNamePair, Object> variableValues) throws VariableValueException, VariableNotFoundException;

    /**
     * Sets the value of multiple context-partitioned variables in one update, applying all or none of the changes
     * to variable values in one atomic transaction.
     * <p>
     * Note that the thread setting the variable value queues the changes, i.e. it does not itself
     * re-evaluate such new variable value for any given statement. The timer thread performs this work.
     * </p>
     * Only for use with context-partitioned variables.
     *
     * @param variableValues  is the map of variable name and variable value, with null an allowed value
     * @param agentInstanceId the id of the context partition
     * @throws VariableValueException    if any value does not match variable type or cannot be safely coerced
     *                                   to the variable type
     * @throws VariableNotFoundException if any of the variable names has not been declared
     */
    void setVariableValue(Map<DeploymentIdNamePair, Object> variableValues, int agentInstanceId) throws VariableValueException, VariableNotFoundException;
}
