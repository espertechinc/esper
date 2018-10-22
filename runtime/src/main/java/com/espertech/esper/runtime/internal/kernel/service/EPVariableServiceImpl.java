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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionVariableState;
import com.espertech.esper.common.client.variable.VariableConstantValueException;
import com.espertech.esper.common.client.variable.VariableNotFoundException;
import com.espertech.esper.common.client.variable.VariableValueException;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;

import java.util.*;

import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

public class EPVariableServiceImpl implements EPVariableServiceSPI {
    private final EPServicesContext services;

    public EPVariableServiceImpl(EPServicesContext services) {
        this.services = services;
    }

    public Map<DeploymentIdNamePair, Class> getVariableTypeAll() {
        Map<DeploymentIdNamePair, VariableReader> variables = services.getVariableManagementService().getVariableReadersNonCP();
        Map<DeploymentIdNamePair, Class> values = new HashMap<>();
        for (Map.Entry<DeploymentIdNamePair, VariableReader> entry : variables.entrySet()) {
            Class type = entry.getValue().getMetaData().getType();
            values.put(entry.getKey(), type);
        }
        return values;
    }

    public Class getVariableType(String deploymentId, String variableName) {
        Variable metaData = services.getVariableManagementService().getVariableMetaData(deploymentId, variableName);
        if (metaData == null) {
            return null;
        }
        return metaData.getMetaData().getType();
    }

    public Object getVariableValue(String deploymentId, String variableName) throws VariableNotFoundException {
        services.getVariableManagementService().setLocalVersion();
        Variable metaData = services.getVariableManagementService().getVariableMetaData(deploymentId, variableName);
        if (metaData == null) {
            throw new VariableNotFoundException("Variable by name '" + variableName + "' has not been declared");
        }
        if (metaData.getMetaData().getOptionalContextName() != null) {
            throw new VariableNotFoundException("Variable by name '" + variableName + "' has been declared for context '" + metaData.getMetaData().getOptionalContextName() + "' and cannot be read without context partition selector");
        }
        VariableReader reader = services.getVariableManagementService().getReader(deploymentId, variableName, DEFAULT_AGENT_INSTANCE_ID);
        Object value = reader.getValue();
        if (value == null || reader.getMetaData().getEventType() == null) {
            return value;
        }
        return ((EventBean) value).getUnderlying();
    }

    public Map<DeploymentIdNamePair, List<ContextPartitionVariableState>> getVariableValue(Set<DeploymentIdNamePair> variableNames, ContextPartitionSelector contextPartitionSelector) throws VariableNotFoundException {
        services.getVariableManagementService().setLocalVersion();
        String contextPartitionName = null;
        String contextDeploymentId = null;
        Variable[] variables = new Variable[variableNames.size()];
        int count = 0;
        for (DeploymentIdNamePair namePair : variableNames) {
            Variable variable = services.getVariableManagementService().getVariableMetaData(namePair.getDeploymentId(), namePair.getName());
            if (variable == null) {
                throw new VariableNotFoundException("Variable by name '" + namePair.getName() + "' has not been declared");
            }
            if (variable.getMetaData().getOptionalContextName() == null) {
                throw new VariableNotFoundException("Variable by name '" + namePair.getName() + "' is a global variable and not context-partitioned");
            }
            if (contextPartitionName == null) {
                contextPartitionName = variable.getMetaData().getOptionalContextName();
                contextDeploymentId = variable.getOptionalContextDeploymentId();
            } else {
                if (!contextPartitionName.equals(variable.getMetaData().getOptionalContextName()) || !contextDeploymentId.equals(variable.getOptionalContextDeploymentId())) {
                    throw new VariableNotFoundException("Variable by name '" + namePair.getName() + "' is a declared for context '" + variable.getMetaData().getOptionalContextName() + "' however the expected context is '" + contextPartitionName + "'");
                }
            }
            variables[count++] = variable;
        }

        ContextManager contextManager = services.getContextManagementService().getContextManager(contextDeploymentId, contextPartitionName);
        if (contextManager == null) {
            throw new VariableNotFoundException("Context by name '" + contextPartitionName + "' cannot be found");
        }
        Map<Integer, ContextPartitionIdentifier> contextPartitions = contextManager.getContextPartitions(contextPartitionSelector).getIdentifiers();
        if (contextPartitions.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<DeploymentIdNamePair, List<ContextPartitionVariableState>> statesMap = new HashMap<>();
        count = 0;
        for (DeploymentIdNamePair pair : variableNames) {
            List<ContextPartitionVariableState> states = new ArrayList<>();
            Variable variable = variables[count++];
            statesMap.put(pair, states);
            for (Map.Entry<Integer, ContextPartitionIdentifier> entry : contextPartitions.entrySet()) {
                VariableReader reader = services.getVariableManagementService().getReader(variable.getDeploymentId(), variable.getMetaData().getVariableName(), entry.getKey());
                Object value = reader.getValue();
                if (value != null && reader.getMetaData().getEventType() != null) {
                    value = ((EventBean) value).getUnderlying();
                }
                states.add(new ContextPartitionVariableState(entry.getKey(), entry.getValue(), value));
            }
            count++;
        }
        return statesMap;
    }

    public Map<DeploymentIdNamePair, Object> getVariableValue(Set<DeploymentIdNamePair> variableNames) throws VariableNotFoundException {
        services.getVariableManagementService().setLocalVersion();
        Map<DeploymentIdNamePair, Object> values = new HashMap<>();
        for (DeploymentIdNamePair pair : variableNames) {
            Variable metaData = services.getVariableManagementService().getVariableMetaData(pair.getDeploymentId(), pair.getName());
            checkVariable(pair.getDeploymentId(), pair.getName(), metaData, false, false);
            VariableReader reader = services.getVariableManagementService().getReader(pair.getDeploymentId(), pair.getName(), DEFAULT_AGENT_INSTANCE_ID);
            Object value = reader.getValue();
            if (value != null && reader.getMetaData().getEventType() != null) {
                value = ((EventBean) value).getUnderlying();
            }
            values.put(pair, value);
        }
        return values;
    }

    public Map<DeploymentIdNamePair, Object> getVariableValueAll() {
        services.getVariableManagementService().setLocalVersion();
        Map<DeploymentIdNamePair, VariableReader> variables = services.getVariableManagementService().getVariableReadersNonCP();
        Map<DeploymentIdNamePair, Object> values = new HashMap<>();
        for (Map.Entry<DeploymentIdNamePair, VariableReader> entry : variables.entrySet()) {
            Object value = entry.getValue().getValue();
            values.put(entry.getKey(), value);
        }
        return values;
    }

    public void setVariableValue(String deploymentId, String variableName, Object variableValue) throws VariableValueException, VariableNotFoundException {
        Variable metaData = services.getVariableManagementService().getVariableMetaData(deploymentId, variableName);
        checkVariable(deploymentId, variableName, metaData, true, false);

        services.getVariableManagementService().getReadWriteLock().writeLock().lock();
        try {
            services.getVariableManagementService().checkAndWrite(deploymentId, variableName, DEFAULT_AGENT_INSTANCE_ID, variableValue);
            services.getVariableManagementService().commit();
        } finally {
            services.getVariableManagementService().getReadWriteLock().writeLock().unlock();
        }
    }

    public void setVariableValue(Map<DeploymentIdNamePair, Object> variableValues) throws VariableValueException, VariableNotFoundException {
        setVariableValueInternal(variableValues, DEFAULT_AGENT_INSTANCE_ID, false);
    }

    public void setVariableValue(Map<DeploymentIdNamePair, Object> variableValues, int agentInstanceId) throws VariableValueException, VariableNotFoundException {
        setVariableValueInternal(variableValues, agentInstanceId, true);
    }

    private void checkVariable(String deploymentId, String variableName, Variable variable, boolean settable, boolean requireContextPartitioned) {
        if (variable == null) {
            if (deploymentId == null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' has not been declared");
            }
            throw new VariableNotFoundException("Variable by name '" + variableName + "' and deployment id '" + deploymentId + "' has not been declared");
        }
        String optionalContextName = variable.getMetaData().getOptionalContextName();
        if (!requireContextPartitioned) {
            if (optionalContextName != null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' has been declared for context '" + optionalContextName + "' and cannot be set without context partition selectors");
            }
        } else {
            if (optionalContextName == null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' is a global variable and not context-partitioned");
            }
        }
        if (settable && variable.getMetaData().isConstant()) {
            throw new VariableConstantValueException("Variable by name '" + variableName + "' is declared as constant and may not be assigned a new value");
        }
    }

    private void setVariableValueInternal(Map<DeploymentIdNamePair, Object> variableValues, int agentInstanceId, boolean requireContextPartitioned) throws EPException {
        // verify
        for (Map.Entry<DeploymentIdNamePair, Object> entry : variableValues.entrySet()) {
            String deploymentId = entry.getKey().getDeploymentId();
            String variableName = entry.getKey().getName();
            Variable metaData = services.getVariableManagementService().getVariableMetaData(deploymentId, variableName);
            checkVariable(deploymentId, variableName, metaData, true, requireContextPartitioned);
        }

        // set values
        services.getVariableManagementService().getReadWriteLock().writeLock().lock();
        try {
            for (Map.Entry<DeploymentIdNamePair, Object> entry : variableValues.entrySet()) {
                String deploymentId = entry.getKey().getDeploymentId();
                String variableName = entry.getKey().getName();
                try {
                    services.getVariableManagementService().checkAndWrite(deploymentId, variableName, agentInstanceId, entry.getValue());
                } catch (RuntimeException ex) {
                    services.getVariableManagementService().rollback();
                    throw ex;
                }
            }
            services.getVariableManagementService().commit();
        } finally {
            services.getVariableManagementService().getReadWriteLock().writeLock().unlock();
        }
    }
}
