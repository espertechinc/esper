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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.client.context.ContextStateEventContextDestroyed;
import com.espertech.esper.common.client.context.ContextStateListener;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContextManagementServiceImpl implements ContextManagementService {
    private static final Logger log = LoggerFactory.getLogger(ContextManagementServiceImpl.class);

    private final CopyOnWriteArrayList<ContextStateListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, ContextDeployment> deployments = new HashMap<>();

    public void addContext(ContextDefinition contextDefinition, EPStatementInitServices services) {
        ContextDeployment deployment = deployments.get(services.getDeploymentId());
        if (deployment == null) {
            deployment = new ContextDeployment();
            deployments.put(services.getDeploymentId(), deployment);
        }
        deployment.add(contextDefinition, services);
    }

    public void addStatement(String deploymentIdCreateContext, String contextName, ContextControllerStatementDesc statement, boolean recovery) {
        ContextManager contextManager = getAssertContextManager(deploymentIdCreateContext, contextName);
        contextManager.addStatement(statement, recovery);
    }

    public void stoppedStatement(String deploymentIdCreateContext, String contextName, ContextControllerStatementDesc statement) {
        ContextManager contextManager = getAssertContextManager(deploymentIdCreateContext, contextName);
        contextManager.stopStatement(statement);
    }

    public ContextManager getContextManager(String deploymentIdCreateContext, String contextName) {
        ContextDeployment deployment = deployments.get(deploymentIdCreateContext);
        if (deployment == null) {
            return null;
        }
        return deployment.getContextManager(contextName);
    }

    public int getContextCount() {
        int count = 0;
        for (Map.Entry<String, ContextDeployment> entry : deployments.entrySet()) {
            count += entry.getValue().getContextCount();
        }
        return count;
    }

    public void destroyedContext(String runtimeURI, String deploymentIdCreateContext, String contextName) {
        ContextDeployment deployment = deployments.get(deploymentIdCreateContext);
        if (deployment == null) {
            log.warn("Destroy for context '" + contextName + "' deployment-id '" + deploymentIdCreateContext + "' failed to locate");
            return;
        }
        deployment.destroyContext(deploymentIdCreateContext, contextName);
        if (deployment.getContextCount() == 0) {
            deployments.remove(deploymentIdCreateContext);
        }
        ContextStateEventUtil.dispatchContext(listeners, () -> new ContextStateEventContextDestroyed(runtimeURI, deploymentIdCreateContext, contextName), ContextStateListener::onContextDestroyed);
    }

    public CopyOnWriteArrayList<ContextStateListener> getListeners() {
        return listeners;
    }

    public Map<String, ContextDeployment> getDeployments() {
        return deployments;
    }

    private ContextManager getAssertContextManager(String deploymentIdCreateContext, String contextName) {
        ContextManager contextManager = getContextManager(deploymentIdCreateContext, contextName);
        if (contextManager == null) {
            throw new IllegalArgumentException("Cannot find context for name '" + contextName + "' deployment-id '" + deploymentIdCreateContext + "'");
        }
        return contextManager;
    }
}
