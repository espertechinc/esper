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
package com.espertech.esper.common.internal.epl.dataflow.core;

import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.internal.context.aifactory.createdataflow.DataflowDesc;
import com.espertech.esper.common.internal.epl.dataflow.realize.DataflowInstantiator;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EPDataFlowServiceImpl implements EPDataFlowService {
    public final static String OP_PACKAGE_NAME = "com.espertech.esper.runtime.internal.dataflow.op";
    private static final Logger log = LoggerFactory.getLogger(EPDataFlowServiceImpl.class);

    private final Map<String, DataflowDeployment> deployments = new HashMap<>();
    private final Map<String, EPDataFlowInstance> instances = new HashMap<String, EPDataFlowInstance>();
    private final DataFlowConfigurationStateService configurationState = new DataFlowConfigurationStateServiceImpl();
    private int agentInstanceNumCurrent;

    public synchronized EPDataFlowDescriptor getDataFlow(String deploymentId, String dataflowName) {
        DataflowDesc entry = getEntryMayNull(deploymentId, dataflowName);
        return entry == null ? null : new EPDataFlowDescriptor(deploymentId, entry.getDataflowName(), entry.getStatementContext().getStatementName());
    }

    public synchronized DeploymentIdNamePair[] getDataFlows() {
        List<DeploymentIdNamePair> ids = new ArrayList<>();
        for (Map.Entry<String, DataflowDeployment> deployment : deployments.entrySet()) {
            for (Map.Entry<String, DataflowDesc> entry : deployment.getValue().getDataflows().entrySet()) {
                ids.add(new DeploymentIdNamePair(deployment.getKey(), entry.getKey()));
            }
        }
        return ids.toArray(new DeploymentIdNamePair[ids.size()]);
    }

    public synchronized EPDataFlowInstance instantiate(String deploymentId, String dataflowName) throws EPDataFlowInstantiationException {
        return instantiate(deploymentId, dataflowName, new EPDataFlowInstantiationOptions());
    }

    public synchronized EPDataFlowInstance instantiate(String deploymentId, String dataFlowName, EPDataFlowInstantiationOptions options) throws EPDataFlowInstantiationException {
        DataflowDesc entry = getEntryMayNull(deploymentId, dataFlowName);
        if (entry == null) {
            throw new EPDataFlowInstantiationException("Data flow by name '" + dataFlowName + "' for deployment id '" + deploymentId + "' has not been defined");
        }
        try {
            agentInstanceNumCurrent++;
            return DataflowInstantiator.instantiate(agentInstanceNumCurrent, entry, options);
        } catch (Exception ex) {
            String message = "Failed to instantiate data flow '" + dataFlowName + "': " + ex.getMessage();
            log.debug(message, ex);
            throw new EPDataFlowInstantiationException(message, ex);
        }
    }

    public synchronized void saveInstance(String instanceName, EPDataFlowInstance instance) throws EPDataFlowAlreadyExistsException {
        if (instances.containsKey(instanceName)) {
            throw new EPDataFlowAlreadyExistsException("Data flow instance name '" + instanceName + "' already saved");
        }
        instances.put(instanceName, instance);
    }

    public synchronized String[] getSavedInstances() {
        Set<String> instanceids = instances.keySet();
        return instanceids.toArray(new String[instanceids.size()]);
    }

    public synchronized EPDataFlowInstance getSavedInstance(String instanceName) {
        return instances.get(instanceName);
    }

    public synchronized boolean removeSavedInstance(String instanceName) {
        return instances.remove(instanceName) != null;
    }

    public synchronized void addDataflow(String deploymentId, DataflowDesc dataflow) {
        DataflowDeployment deployment = deployments.get(deploymentId);
        if (deployment == null) {
            deployment = new DataflowDeployment();
            deployments.put(deploymentId, deployment);
        }
        deployment.add(dataflow.getDataflowName(), dataflow);
    }

    public synchronized void removeDataflow(String deploymentId, DataflowDesc dataflow) {
        DataflowDeployment deployment = deployments.get(deploymentId);
        if (deployment == null) {
            return;
        }
        deployment.remove(dataflow.getDataflowName());
    }

    public synchronized void saveConfiguration(String dataflowConfigName, String deploymentId, String dataFlowName, EPDataFlowInstantiationOptions options) {
        DataflowDesc entry = getEntryMayNull(deploymentId, dataFlowName);
        if (entry == null) {
            String message = "Failed to locate data flow '" + dataFlowName + "'";
            throw new EPDataFlowNotFoundException(message);
        }
        if (configurationState.exists(dataflowConfigName)) {
            String message = "Data flow saved configuration by name '" + dataflowConfigName + "' already exists";
            throw new EPDataFlowAlreadyExistsException(message);
        }
        configurationState.add(new EPDataFlowSavedConfiguration(dataflowConfigName, deploymentId, dataFlowName, options));
    }

    public synchronized String[] getSavedConfigurations() {
        return configurationState.getSavedConfigNames();
    }

    public synchronized EPDataFlowSavedConfiguration getSavedConfiguration(String configurationName) {
        return configurationState.getSavedConfig(configurationName);
    }

    public synchronized EPDataFlowInstance instantiateSavedConfiguration(String configurationName) throws EPDataFlowInstantiationException {
        EPDataFlowSavedConfiguration savedConfiguration = configurationState.getSavedConfig(configurationName);
        if (savedConfiguration == null) {
            throw new EPDataFlowInstantiationException("Dataflow saved configuration '" + configurationName + "' could not be found");
        }
        EPDataFlowInstantiationOptions options = savedConfiguration.getOptions();
        if (options == null) {
            options = new EPDataFlowInstantiationOptions();
            options.setDataFlowInstanceId(configurationName);
        }
        return instantiate(savedConfiguration.getDeploymentId(), savedConfiguration.getDataflowName(), options);
    }

    public synchronized boolean removeSavedConfiguration(String configurationName) {
        return configurationState.removePrototype(configurationName) != null;
    }

    private DataflowDesc getEntryMayNull(String deploymentId, String dataFlowName) {
        DataflowDeployment deployment = deployments.get(deploymentId);
        return deployment == null ? null : deployment.getDataflow(dataFlowName);
    }
}
