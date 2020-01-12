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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.context.util.AgentInstanceTransferServices;
import com.espertech.esper.common.internal.context.util.ContextRuntimeDescriptor;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.util.DestroyCallback;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.stage.EPStageDestroyedException;
import com.espertech.esper.runtime.client.stage.EPStageException;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.runtime.internal.kernel.stage.StageDeploymentHelper.movePath;
import static com.espertech.esper.runtime.internal.kernel.stage.StagePreconditionHelper.validateDependencyPreconditions;
import static com.espertech.esper.runtime.internal.kernel.stage.StageStatementHelper.updateStatement;

public class EPStageImpl implements EPStageSPI {

    private final String stageUri;
    private final int stageId;
    private final EPServicesContext servicesContext;
    private final StageSpecificServices stageSpecificServices;
    private final EPStageEventServiceSPI eventServiceStage;
    private final EPStageDeploymentServiceImpl deploymentServiceStage;
    private final DestroyCallback stageDestroyCallback;
    private boolean destroyed;

    public EPStageImpl(String stageUri, int stageId, EPServicesContext servicesContext, StageSpecificServices stageSpecificServices, EPStageEventServiceSPI eventServiceStage, EPStageDeploymentServiceImpl deploymentServiceStage, DestroyCallback stageDestroyCallback) {
        this.stageUri = stageUri;
        this.stageId = stageId;
        this.servicesContext = servicesContext;
        this.stageSpecificServices = stageSpecificServices;
        this.eventServiceStage = eventServiceStage;
        this.deploymentServiceStage = deploymentServiceStage;
        this.stageDestroyCallback = stageDestroyCallback;
    }

    public synchronized void stage(Collection<String> deploymentIdsProvided) throws EPStageException {
        checkDestroyed();
        Set<String> deploymentIds = checkArgument(deploymentIdsProvided);
        if (deploymentIds.isEmpty()) {
            return;
        }
        checkDeploymentIdsExist(deploymentIds, servicesContext.getDeploymentLifecycleService());
        validateDependencyPreconditions(deploymentIds, servicesContext, servicesContext.getDeploymentLifecycleService());

        Set<Integer> statementIds = new HashSet<>();
        for (String deploymentId : deploymentIds) {
            DeploymentInternal deployment = servicesContext.getDeploymentLifecycleService().getDeploymentById(deploymentId);
            traverseContextPartitions(deployment, this::processStage);
            traverseStatements(deployment, statementContext -> updateStatement(statementContext, stageSpecificServices), statementIds);
            movePath(deployment, servicesContext, stageSpecificServices);
            servicesContext.getDeploymentLifecycleService().removeDeployment(deploymentId);
            stageSpecificServices.getDeploymentLifecycleService().addDeployment(deploymentId, deployment);
            servicesContext.getStageRecoveryService().deploymentSetStage(deploymentId, stageUri);
        }
        servicesContext.getSchedulingService().transfer(statementIds, stageSpecificServices.getSchedulingService());
    }

    public synchronized void unstage(Collection<String> deploymentIdsProvided) throws EPStageException {
        checkDestroyed();
        Set<String> deploymentIds = checkArgument(deploymentIdsProvided);
        if (deploymentIds.isEmpty()) {
            return;
        }
        checkDeploymentIdsExist(deploymentIds, stageSpecificServices.getDeploymentLifecycleService());
        validateDependencyPreconditions(deploymentIds, stageSpecificServices, stageSpecificServices.getDeploymentLifecycleService());

        Set<Integer> statementIds = new HashSet<>();
        for (String deploymentId : deploymentIds) {
            DeploymentInternal deployment = stageSpecificServices.getDeploymentLifecycleService().getDeploymentById(deploymentId);
            traverseContextPartitions(deployment, this::processUnstage);
            traverseStatements(deployment, statementContext -> updateStatement(statementContext, servicesContext), statementIds);
            movePath(deployment, stageSpecificServices, servicesContext);
            stageSpecificServices.getDeploymentLifecycleService().removeDeployment(deploymentId);
            servicesContext.getDeploymentLifecycleService().addDeployment(deploymentId, deployment);
            servicesContext.getStageRecoveryService().deploymentRemoveFromStages(deploymentId);
        }
        stageSpecificServices.getSchedulingService().transfer(statementIds, servicesContext.getSchedulingService());
    }

    public EPStageDeploymentServiceImpl getDeploymentService() throws EPStageDestroyedException {
        checkDestroyed();
        return deploymentServiceStage;
    }

    public EPStageEventServiceSPI getEventService() throws EPStageDestroyedException {
        checkDestroyed();
        return eventServiceStage;
    }

    public StageSpecificServices getStageSpecificServices() {
        return stageSpecificServices;
    }

    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        if (!stageSpecificServices.getDeploymentLifecycleService().getDeploymentMap().isEmpty()) {
            throw new EPException("Failed to destroy stage '" + stageUri + "': The stage has existing deployments");
        }
        destroyNoCheck();
    }

    public synchronized void destroyNoCheck() {
        if (destroyed) {
            return;
        }
        stageSpecificServices.destroy();
        stageDestroyCallback.destroy();
        destroyed = true;
    }

    public String getURI() {
        return stageUri;
    }

    private void traverseContextPartitions(DeploymentInternal deployment, Consumer<StatementResourceHolder> consumer) {
        for (EPStatement statement : deployment.getStatements()) {
            EPStatementSPI spi = (EPStatementSPI) statement;
            if (spi.getStatementContext().getContextName() == null) {
                StatementResourceHolder holder = spi.getStatementContext().getStatementCPCacheService().makeOrGetEntryCanNull(-1, spi.getStatementContext());
                consumer.accept(holder);
            } else {
                ContextRuntimeDescriptor contextDesc = spi.getStatementContext().getContextRuntimeDescriptor();
                ContextManager contextManager = servicesContext.getContextManagementService().getContextManager(contextDesc.getContextDeploymentId(), contextDesc.getContextName());
                Collection<Integer> agentInstanceIds = contextManager.getRealization().getAgentInstanceIds(ContextPartitionSelectorAll.INSTANCE);
                for (int agentInstanceId : agentInstanceIds) {
                    StatementResourceHolder holder = spi.getStatementContext().getStatementCPCacheService().makeOrGetEntryCanNull(agentInstanceId, spi.getStatementContext());
                    consumer.accept(holder);
                }
            }
        }
    }

    private Set<String> checkArgument(Collection<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new IllegalArgumentException("Null or empty deployment ids");
        }
        for (String deploymentId : deploymentIds) {
            if (deploymentId == null) {
                throw new IllegalArgumentException("Null or empty deployment id");
            }
        }
        return new LinkedHashSet<>(deploymentIds);
    }

    private void traverseStatements(DeploymentInternal deployment, Consumer<StatementContext> consumer, Set<Integer> statementIds) {
        for (EPStatement statement : deployment.getStatements()) {
            EPStatementSPI spi = (EPStatementSPI) statement;
            consumer.accept(spi.getStatementContext());
            statementIds.add(spi.getStatementId());
        }
    }

    private void processStage(StatementResourceHolder holder) {
        AgentInstanceTransferServices xfer = new AgentInstanceTransferServices(holder.getAgentInstanceContext(), stageSpecificServices.getFilterService(), stageSpecificServices.getSchedulingService(), stageSpecificServices.getInternalEventRouter());
        holder.getAgentInstanceStopCallback().transfer(xfer);
        if (holder.getContextManagerRealization() != null) {
            holder.getContextManagerRealization().transfer(xfer);
        }
        holder.getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(stageSpecificServices.getFilterService().getFiltersVersion());
    }

    private void processUnstage(StatementResourceHolder holder) {
        AgentInstanceTransferServices xfer = new AgentInstanceTransferServices(holder.getAgentInstanceContext(), servicesContext.getFilterService(), servicesContext.getSchedulingService(), servicesContext.getInternalEventRouter());
        holder.getAgentInstanceStopCallback().transfer(xfer);
        if (holder.getContextManagerRealization() != null) {
            holder.getContextManagerRealization().transfer(xfer);
        }
        holder.getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(servicesContext.getFilterService().getFiltersVersion());
    }

    private void checkDeploymentIdsExist(Set<String> deploymentIds, DeploymentLifecycleService deploymentLifecycleService) throws EPStageException {
        for (String deploymentId : deploymentIds) {
            DeploymentInternal deployment = deploymentLifecycleService.getDeploymentById(deploymentId);
            if (deployment == null) {
                throw new EPStageException("Deployment '" + deploymentId + "' was not found");
            }
        }
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new EPStageDestroyedException("Stage '" + stageUri + "' is destroyed");
        }
    }

    public int getStageId() {
        return stageId;
    }
}
