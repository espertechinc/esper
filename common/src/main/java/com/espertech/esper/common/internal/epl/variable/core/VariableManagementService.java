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
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.common.internal.util.NullableObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BiConsumer;

/**
 * Variables service for reading and writing variables, and for setting a version number for the current thread to
 * consider variables for.
 * <p>
 * See implementation class for further details.
 */
public interface VariableManagementService {
    /**
     * Sets the variable version that subsequent reads consider.
     */
    void setLocalVersion();

    /**
     * Lock for use in atomic writes to the variable space.
     *
     * @return read write lock for external coordinated write
     */
    ReadWriteLock getReadWriteLock();

    void addVariable(String deploymentId, VariableMetaData metaData, String optionalDeploymentIdContext, DataInputOutputSerde<Object> optionalSerde);

    /**
     * Returns a reader that provides access to variable values. The reader considers the
     * version currently set via setLocalVersion.
     *
     * @param deploymentId            deployment id
     * @param variableName            the variable that the reader should read
     * @param agentInstanceIdAccessor agent instance id of accessor
     * @return reader
     */
    VariableReader getReader(String deploymentId, String variableName, int agentInstanceIdAccessor);

    /**
     * Registers a callback invoked when the variable is written with a new value.
     *
     * @param deploymentId           deployment id
     * @param variableName           variable name
     * @param agentInstanceId        agent instance id
     * @param variableChangeCallback a callback
     */
    void registerCallback(String deploymentId, String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback);

    /**
     * Removes a callback.
     *
     * @param deploymentId           deployment id
     * @param variableName           variable name
     * @param agentInstanceId        agent instance id
     * @param variableChangeCallback a callback
     */
    public void unregisterCallback(String deploymentId, String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback);

    /**
     * Writes a new variable value.
     * <p>
     * Must be followed by either a commit or rollback.
     *
     * @param variableNumber  the index number of the variable to write (from VariableReader)
     * @param newValue        the new value
     * @param agentInstanceId agent instance id
     */
    void write(int variableNumber, int agentInstanceId, Object newValue);

    /**
     * Check type of the value supplied and writes the new variable value.
     * <p>
     * Must be followed by either a commit or rollback.
     *
     * @param deploymentId    deployment id
     * @param variableName    variable name
     * @param agentInstanceId agent instance id
     * @param newValue        the new value
     */
    void checkAndWrite(String deploymentId, String variableName, int agentInstanceId, Object newValue);

    /**
     * Commits the variable outstanding changes.
     */
    void commit();

    /**
     * Rolls back the variable outstanding changes.
     */
    void rollback();

    Variable getVariableMetaData(String deploymentId, String variableName);

    /**
     * Removes a variable.
     *
     * @param deploymentId deployment id
     * @param variableName to remove
     */
    void removeVariableIfFound(String deploymentId, String variableName);

    void destroy();

    void allocateVariableState(String deploymentId, String variableName, int agentInstanceId, boolean recovery, NullableObject<Object> initialValue, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    void deallocateVariableState(String deploymentId, String variableName, int agentInstanceId);

    ConcurrentHashMap<Integer, VariableReader> getReadersPerCP(String deploymentId, String variableName);

    Map<DeploymentIdNamePair, VariableReader> getVariableReadersNonCP();

    VariableStateNonConstHandler getOptionalStateHandler();

    Map<String, VariableDeployment> getDeploymentsWithVariables();

    void traverseVariables(BiConsumer<String, Variable> consumer);
}
