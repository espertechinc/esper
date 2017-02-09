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
package com.espertech.esper.client.dataflow;

/**
 * Data flow runtime for instantiating data flows.
 */
public interface EPDataFlowRuntime {

    /**
     * Returns a descriptor for the given data flow, or null if the data flow has not been declared.
     *
     * @param dataFlowName data flow name
     * @return data flow descriptor
     */
    public EPDataFlowDescriptor getDataFlow(String dataFlowName);

    /**
     * Returns the names of all declared data flows.
     *
     * @return data flow names
     */
    public String[] getDataFlows();

    /**
     * Instantiate a data flow.
     *
     * @param dataFlowName name of data flow to instantiate
     * @return data flow instance
     * @throws EPDataFlowInstantiationException when the instantiation failed
     */
    public EPDataFlowInstance instantiate(String dataFlowName) throws EPDataFlowInstantiationException;

    /**
     * Instantiate a data flow, with options.
     *
     * @param dataFlowName name of data flow to instantiate
     * @param options      populate options to control parameterization, instantiation etc.
     * @return data flow instance
     * @throws EPDataFlowInstantiationException when the instantiation failed
     */
    public EPDataFlowInstance instantiate(String dataFlowName, EPDataFlowInstantiationOptions options) throws EPDataFlowInstantiationException;

    /**
     * Save an existing data flow configuration (data flow name and its options) for later retrieval.
     *
     * @param dataflowConfigName configuration name to save, must be unique
     * @param dataFlowName       data flow name
     * @param options            options object
     * @throws EPDataFlowAlreadyExistsException if the configuration name is already used
     * @throws EPDataFlowNotFoundException      if the data flow by this name does not exist
     */
    public void saveConfiguration(String dataflowConfigName, String dataFlowName, EPDataFlowInstantiationOptions options) throws EPDataFlowAlreadyExistsException, EPDataFlowNotFoundException;

    /**
     * Returns the names of a saved data flow configurations.
     *
     * @return data flow configuration names
     */
    public String[] getSavedConfigurations();

    /**
     * Returns a saved dataflow configuration or null if it is not found.
     *
     * @param configurationName name to find
     * @return data flow configuration
     */
    public EPDataFlowSavedConfiguration getSavedConfiguration(String configurationName);

    /**
     * Instantiate a data flow from a saved configuration.
     *
     * @param configurationName configuration name
     * @return instance
     * @throws EPDataFlowInstantiationException if the configuration name could not be found
     */
    public EPDataFlowInstance instantiateSavedConfiguration(String configurationName) throws EPDataFlowInstantiationException;

    /**
     * Remove a previously saved data flow configuration.
     *
     * @param configurationName to remove
     * @return indicator whether found and removed
     */
    public boolean removeSavedConfiguration(String configurationName);

    /**
     * Save an existing instance with the runtime, for later retrieval.
     *
     * @param instanceName name to use to save, must be unique among currently saved instances
     * @param instance     saved
     * @throws EPDataFlowAlreadyExistsException if an instance by this name already exists
     */
    public void saveInstance(String instanceName, EPDataFlowInstance instance) throws EPDataFlowAlreadyExistsException;

    /**
     * Returns the instance names of a saved data flow instances.
     *
     * @return data flow instance names
     */
    public String[] getSavedInstances();

    /**
     * Returns a specific saved data flow instance, or null if it has not been found
     *
     * @param instanceName to look for
     * @return instance
     */
    public EPDataFlowInstance getSavedInstance(String instanceName);

    /**
     * Remove an instance previously saved.
     *
     * @param instanceName to be removed
     * @return indicator whether found or not
     */
    public boolean removeSavedInstance(String instanceName);
}
