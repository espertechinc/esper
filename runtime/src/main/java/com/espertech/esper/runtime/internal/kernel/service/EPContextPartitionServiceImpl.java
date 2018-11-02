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

import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManager;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EPContextPartitionServiceImpl implements EPContextPartitionService {

    private final EPServicesContext services;

    public EPContextPartitionServiceImpl(EPServicesContext services) {
        this.services = services;
    }

    public String[] getContextStatementNames(String deploymentId, String contextName) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        String[] statements = new String[contextManager.getStatements().size()];
        int count = 0;
        for (Map.Entry<Integer, ContextControllerStatementDesc> entry : contextManager.getStatements().entrySet()) {
            statements[count++] = entry.getValue().getLightweight().getStatementContext().getStatementName();
        }
        return statements;
    }

    public int getContextNestingLevel(String deploymentId, String contextName) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getNumNestingLevels();
    }

    public ContextPartitionCollection getContextPartitions(String deploymentId, String contextName, ContextPartitionSelector selector) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getContextPartitions(selector);
    }

    public Set<Integer> getContextPartitionIds(String deploymentId, String contextName, ContextPartitionSelector selector) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getContextPartitionIds(selector);
    }

    public long getContextPartitionCount(String deploymentId, String contextName) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getContextPartitionCount();
    }

    public ContextPartitionIdentifier getIdentifier(String deploymentId, String contextName, int agentInstanceId) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getContextIdentifier(agentInstanceId);
    }

    public void addContextStateListener(ContextStateListener listener) {
        services.getContextManagementService().getListeners().add(listener);
    }

    public void removeContextStateListener(ContextStateListener listener) {
        services.getContextManagementService().getListeners().remove(listener);
    }

    public Iterator<ContextStateListener> getContextStateListeners() {
        return services.getContextManagementService().getListeners().iterator();
    }

    public void removeContextStateListeners() {
        services.getContextManagementService().getListeners().clear();
    }

    public void addContextPartitionStateListener(String deploymentId, String contextName, ContextPartitionStateListener listener) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        contextManager.addListener(listener);
    }

    public void removeContextPartitionStateListener(String deploymentId, String contextName, ContextPartitionStateListener listener) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        contextManager.removeListener(listener);
    }

    public Iterator<ContextPartitionStateListener> getContextPartitionStateListeners(String deploymentId, String contextName) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getListeners();
    }

    public void removeContextPartitionStateListeners(String deploymentId, String contextName) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        contextManager.removeListeners();
    }

    public Map<String, Object> getContextProperties(String deploymentId, String contextName, int contextPartitionId) {
        ContextManager contextManager = checkedGetContextManager(deploymentId, contextName);
        return contextManager.getContextPartitions(contextPartitionId);
    }

    private ContextManager checkedGetContextManager(String deploymentId, String contextName) {
        ContextManager contextManager = services.getContextManagementService().getContextManager(deploymentId, contextName);
        if (contextManager == null) {
            throw new IllegalArgumentException("Context by name '" + contextName + "' could not be found for deployment-id '" + deploymentId + "'");
        }
        return contextManager;
    }
}
