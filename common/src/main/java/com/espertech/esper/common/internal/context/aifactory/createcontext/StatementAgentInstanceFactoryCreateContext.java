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
package com.espertech.esper.common.internal.context.aifactory.createcontext;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextStateEventContextCreated;
import com.espertech.esper.common.client.context.ContextStateListener;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextStateEventUtil;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.view.core.ZeroDepthStreamNoIterate;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatementAgentInstanceFactoryCreateContext implements StatementAgentInstanceFactory, StatementReadyCallback {
    private String contextName;
    private EventType statementEventType;

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setStatementEventType(EventType statementEventType) {
        this.statementEventType = statementEventType;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        ContextManager contextManager = statementContext.getContextManagementService().getContextManager(statementContext.getDeploymentId(), contextName);
        contextManager.setStatementContext(statementContext);
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        ContextManager manager = agentInstanceContext.getContextManagementService().getContextManager(agentInstanceContext.getDeploymentId(), contextName);
        agentInstanceContext.getEpStatementAgentInstanceHandle().setFilterFaultHandler(manager);

        ContextManagerRealization realization = manager.allocateNewRealization(agentInstanceContext);
        return new StatementAgentInstanceFactoryCreateContextResult(new ZeroDepthStreamNoIterate(statementEventType), AgentInstanceStopCallback.INSTANCE_NO_ACTION, agentInstanceContext, null, null, null, null, null, null, Collections.emptyList(), realization);
    }

    public void statementCreate(StatementContext statementContext) {
        CopyOnWriteArrayList<ContextStateListener> listeners = statementContext.getContextManagementService().getListeners();
        ContextStateEventUtil.dispatchContext(listeners, () -> new ContextStateEventContextCreated(statementContext.getRuntimeURI(), statementContext.getDeploymentId(), contextName), ContextStateListener::onContextCreated);
    }

    public void statementDestroyPreconditions(StatementContext statementContext) throws UndeployPreconditionException {
        ContextManager manager = statementContext.getContextManagementService().getContextManager(statementContext.getDeploymentId(), contextName);
        int count = manager.countStatements(stmt -> !stmt.getDeploymentId().equals(statementContext.getDeploymentId()));
        if (count != 0) {
            throw new UndeployPreconditionException("Context by name '" + contextName + "' is still referenced by statements and may not be undeployed");
        }
    }

    public void statementDestroy(StatementContext statementContext) {
        statementContext.getContextManagementService().destroyedContext(statementContext.getRuntimeURI(), statementContext.getDeploymentId(), contextName);
    }

    public EventType getStatementEventType() {
        return statementEventType;
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
