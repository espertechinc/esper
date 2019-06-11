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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryUtil;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignments;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignmentsImpl;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.view.core.View;

import java.util.concurrent.locks.ReentrantLock;

public class StatementCPCacheService {
    public final static int DEFAULT_AGENT_INSTANCE_ID = -1;

    private final boolean contextPartitioned;
    private final StatementResourceService statementResourceService;
    private final StatementAIResourceRegistry statementAgentInstanceRegistry; // for expression resources under context partitioning
    private final ReentrantLock lock = new ReentrantLock();

    public StatementCPCacheService(boolean contextPartitioned, StatementResourceService statementResourceService, StatementAIResourceRegistry statementAgentInstanceRegistry) {
        this.contextPartitioned = contextPartitioned;
        this.statementResourceService = statementResourceService;
        this.statementAgentInstanceRegistry = statementAgentInstanceRegistry;
    }

    public StatementResourceService getStatementResourceService() {
        return statementResourceService;
    }

    public StatementResourceHolder makeOrGetEntryCanNull(int agentInstanceId, StatementContext statementContext) {
        if (!contextPartitioned) {
            return makeOrGetEntryUnpartitioned(statementContext);
        }
        return makeOrGetEntryPartitioned(agentInstanceId, statementContext);
    }

    /**
     * Thread-safe and efficient make-or-get
     *
     * @param statementContext statement context
     * @return page resources
     */
    private StatementResourceHolder makeOrGetEntryUnpartitioned(StatementContext statementContext) {
        StatementResourceHolder resources = statementResourceService.getResourcesUnpartitioned();

        if (resources == null) {
            lock.lock();
            try {
                resources = statementResourceService.getResourcesUnpartitioned();
                if (resources != null) {
                    return resources;
                }

                AgentInstanceContext agentInstanceContext = makeNewAgentInstanceContextCanNull(DEFAULT_AGENT_INSTANCE_ID, statementContext, false);
                StatementAgentInstanceFactoryResult result = statementContext.getStatementAIFactoryProvider().getFactory().newContext(agentInstanceContext, true);
                hookUpNewRealization(result, statementContext);
                resources = statementContext.getStatementContextRuntimeServices().getStatementResourceHolderBuilder().build(agentInstanceContext, result);
                // for consistency with context partition behavior we are holding on to resources for now
                statementResourceService.setUnpartitioned(resources);
            } finally {
                lock.unlock();
            }
        }
        return resources;
    }

    private StatementResourceHolder makeOrGetEntryPartitioned(int agentInstanceId, StatementContext statementContext) {
        StatementResourceService statementResourceService = statementContext.getStatementCPCacheService().getStatementResourceService();
        StatementResourceHolder resources = statementResourceService.getResourcesPartitioned().get(agentInstanceId);
        if (resources != null) {
            return resources;
        }

        lock.lock();
        try {
            resources = statementResourceService.getResourcesPartitioned().get(agentInstanceId);
            if (resources != null) {
                return resources;
            }

            AgentInstanceContext agentInstanceContext = makeNewAgentInstanceContextCanNull(agentInstanceId, statementContext, true);

            // we may receive a null if the context partition has already been deleted
            if (agentInstanceContext == null) {
                return null;
            }

            StatementAgentInstanceFactoryResult result = statementContext.getStatementAIFactoryProvider().getFactory().newContext(agentInstanceContext, true);
            hookUpNewRealization(result, statementContext);
            resources = statementContext.getStatementContextRuntimeServices().getStatementResourceHolderBuilder().build(agentInstanceContext, result);

            // we need to hold onto the handle for now even if it gets removed in order to correctly handle filter faults
            // i.e. for example context partitioned and context partition gets destroyed the statement should not fire for same event
            statementResourceService.setPartitioned(agentInstanceId, resources);

            // assign the strategies
            assignAIResourcesForExpressionContextPartitions(agentInstanceId, resources);
        } finally {
            lock.unlock();
        }

        return resources;
    }

    private static AgentInstanceContext makeNewAgentInstanceContextCanNull(int agentInstanceId, StatementContext statementContext, boolean partitioned) {
        // re-allocate lock: for unpartitoned cases we use the same lock associated to the statement (no need to produce more locks)
        StatementAgentInstanceLock lock = statementContext.getStatementAIFactoryProvider().getFactory().obtainAgentInstanceLock(statementContext, agentInstanceId);
        EPStatementAgentInstanceHandle epStatementAgentInstanceHandle = new EPStatementAgentInstanceHandle(statementContext.getEpStatementHandle(), agentInstanceId, lock);

        // filter fault handler for create-context statements
        String contextName = statementContext.getContextName();
        MappedEventBean contextProperties = null;
        AgentInstanceFilterProxy agentInstanceFilterProxy = null;
        if (contextName != null) {
            String contextDeploymentId = statementContext.getContextRuntimeDescriptor().getContextDeploymentId();
            ContextManager contextManager = statementContext.getContextManagementService().getContextManager(contextDeploymentId, contextName);
            epStatementAgentInstanceHandle.setFilterFaultHandler(contextManager);

            // the context partition may have been deleted
            ContextAgentInstanceInfo info = contextManager.getContextAgentInstanceInfo(statementContext, agentInstanceId);
            if (info == null) {
                return null;
            }

            agentInstanceFilterProxy = info.getFilterProxy();
            contextProperties = info.getContextProperties();
        }

        // re-allocate context
        AuditProvider auditProvider = statementContext.getStatementInformationals().getAuditProvider();
        InstrumentationCommon instrumentationProvider = statementContext.getStatementInformationals().getInstrumentationProvider();
        return new AgentInstanceContext(statementContext, epStatementAgentInstanceHandle, agentInstanceFilterProxy, contextProperties, auditProvider, instrumentationProvider);
    }

    private void hookUpNewRealization(StatementAgentInstanceFactoryResult result, StatementContext statementContext) {
        View dispatchChildView = statementContext.getUpdateDispatchView();
        if (dispatchChildView != null) {
            result.getFinalView().setChild(dispatchChildView);
        }
        if (statementContext.getContextName() == null) {
            StatementAIFactoryAssignments assignments = new StatementAIFactoryAssignmentsImpl(result.getOptionalAggegationService(),
                    result.getPriorStrategies(), result.getPreviousGetterStrategies(), result.getSubselectStrategies(), result.getTableAccessStrategies(),
                    result.getRowRecogPreviousStrategy());
            statementContext.getStatementAIFactoryProvider().assign(assignments);
        }
    }

    private void assignAIResourcesForExpressionContextPartitions(int agentInstanceId, StatementResourceHolder holder) {
        AIRegistryUtil.assignFutures(statementAgentInstanceRegistry, agentInstanceId,
                holder.getAggregationService(),
                holder.getPriorEvalStrategies(),
                holder.getPreviousGetterStrategies(),
                holder.getSubselectStrategies(),
                holder.getTableAccessStrategies(),
                holder.getRowRecogPreviousStrategy());
    }

    public boolean isContextPartitioned() {
        return contextPartitioned;
    }

    public int clear(StatementContext statementContext) {
        int numCleared = 0;

        // un-assign any assigned expressions
        if (statementContext.getContextName() == null) {
            statementContext.getStatementAIFactoryProvider().unassign();
        }

        StatementResourceService statementResourceService = statementContext.getStatementCPCacheService().getStatementResourceService();

        if (!contextPartitioned) {
            if (statementResourceService.getResourcesUnpartitioned() != null) {
                statementResourceService.deallocateUnpartitioned();
                numCleared++;
            }
            return numCleared;
        }

        Integer[] agentInstanceIds = statementResourceService.getResourcesPartitioned().keySet().toArray(new Integer[0]);
        for (Integer agentInstanceId : agentInstanceIds) {
            statementAgentInstanceRegistry.deassign(agentInstanceId);
        }
        for (int agentInstanceId : agentInstanceIds) {
            statementResourceService.deallocatePartitioned(agentInstanceId);
            numCleared++;
        }

        return numCleared;
    }
}
