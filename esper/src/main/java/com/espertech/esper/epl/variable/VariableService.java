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
package com.espertech.esper.epl.variable;

import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Variables service for reading and writing variables, and for setting a version number for the current thread to
 * consider variables for.
 * <p>
 * See implementation class for further details.
 */
public interface VariableService {
    /**
     * Sets the variable version that subsequent reads consider.
     */
    public void setLocalVersion();

    /**
     * Lock for use in atomic writes to the variable space.
     *
     * @return read write lock for external coordinated write
     */
    public ReadWriteLock getReadWriteLock();

    public void createNewVariable(String optionalContextName, String variableName, String type, boolean constant, boolean array, boolean arrayOfPrimitive, Object value, EngineImportService engineImportService)
            throws VariableExistsException, VariableTypeException;

    /**
     * Returns a reader that provides access to variable values. The reader considers the
     * version currently set via setLocalVersion.
     *
     * @param variableName            the variable that the reader should read
     * @param agentInstanceIdAccessor agent instance id of accessor
     * @return reader
     */
    public VariableReader getReader(String variableName, int agentInstanceIdAccessor);

    /**
     * Registers a callback invoked when the variable is written with a new value.
     *
     * @param variableName           variable name
     * @param agentInstanceId        agent instance id
     * @param variableChangeCallback a callback
     */
    public void registerCallback(String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback);

    /**
     * Removes a callback.
     *
     * @param variableName           variable name
     * @param agentInstanceId        agent instance id
     * @param variableChangeCallback a callback
     */
    public void unregisterCallback(String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback);

    /**
     * Writes a new variable value.
     * <p>
     * Must be followed by either a commit or rollback.
     *
     * @param variableNumber  the index number of the variable to write (from VariableReader)
     * @param newValue        the new value
     * @param agentInstanceId agent instance id
     */
    public void write(int variableNumber, int agentInstanceId, Object newValue);

    /**
     * Check type of the value supplied and writes the new variable value.
     * <p>
     * Must be followed by either a commit or rollback.
     *
     * @param variableName    variable name
     * @param agentInstanceId agent instance id
     * @param newValue        the new value
     */
    public void checkAndWrite(String variableName, int agentInstanceId, Object newValue);

    /**
     * Commits the variable outstanding changes.
     */
    public void commit();

    /**
     * Rolls back the variable outstanding changes.
     */
    public void rollback();

    /**
     * Returns a map of variable name and reader, for thread-safe iteration.
     *
     * @return variable names and readers
     */
    public Map<String, VariableReader> getVariableReadersNonCP();

    public VariableMetaData getVariableMetaData(String variableName);

    /**
     * Removes a variable.
     *
     * @param name to remove
     */
    public void removeVariableIfFound(String name);

    public void destroy();

    public String isContextVariable(String propertyName);

    public void allocateVariableState(String variableName, int agentInstanceId, StatementExtensionSvcContext extensionServicesContext, boolean isRecoveringResilient);

    public void deallocateVariableState(String variableName, int agentInstanceId);

    public ConcurrentHashMap<Integer, VariableReader> getReadersPerCP(String variableName);
}
