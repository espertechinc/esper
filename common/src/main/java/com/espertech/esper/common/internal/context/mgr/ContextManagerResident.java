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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryFactoryMap;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryUtil;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.cpidsvc.ContextPartitionIdService;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class ContextManagerResident implements ContextManager, ContextIteratorHandler {

    private final ContextDefinition contextDefinition;
    private final Map<Integer, ContextControllerStatementDesc> statements = new LinkedHashMap<>(); // retain order of statement creation
    private ContextRuntimeDescriptor contextRuntimeDescriptor;
    protected CopyOnWriteArrayList<ContextPartitionStateListener> listenersLazy;

    private StatementContext statementContextCreate;
    private DataInputOutputSerde[] contextPartitionKeySerdes;
    private ContextPartitionIdService contextPartitionIdService;

    public ContextManagerResident(String deploymentId, ContextDefinition contextDefinition) {
        this.contextDefinition = contextDefinition;
        this.contextRuntimeDescriptor = new ContextRuntimeDescriptor(contextDefinition.getContextName(), deploymentId, this);
    }

    public void setStatementContext(StatementContext statementContext) {
        this.statementContextCreate = statementContext;
        this.contextPartitionKeySerdes = statementContextCreate.getContextServiceFactory().getContextPartitionKeyBindings(contextDefinition);
        this.contextPartitionIdService = statementContextCreate.getContextServiceFactory().getContextPartitionIdService(statementContextCreate, contextPartitionKeySerdes);
    }

    public void addStatement(ContextControllerStatementDesc statement, boolean recovery) {
        StatementContext statementContextOfStatement = statement.getLightweight().getStatementContext();
        statements.put(statementContextOfStatement.getStatementId(), statement);

        // dispatch event
        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextStatementAdded(statementContextCreate.getRuntimeURI(), contextRuntimeDescriptor.getContextDeploymentId(), contextDefinition.getContextName(), statementContextOfStatement.getDeploymentId(), statementContextOfStatement.getStatementName()), ContextPartitionStateListener::onContextStatementAdded);

        if (recovery) {
            if (statement.getLightweight().getStatementInformationals().getStatementType() == StatementType.CREATE_VARIABLE) {
                getRealization().activateCreateVariableStatement(statement);
            }
            return;
        }

        // activate if this is the first statement
        if (statements.size() == 1) {
            getRealization().startContext();
            ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextActivated(statementContextCreate.getRuntimeURI(), contextRuntimeDescriptor.getContextDeploymentId(), contextDefinition.getContextName()), ContextPartitionStateListener::onContextActivated);
        } else {
            // activate statement in respect to existing context partitions
            getRealization().startLateStatement(statement);
        }
    }

    public void stopStatement(ContextControllerStatementDesc statement) {
        int statementId = statement.getLightweight().getStatementContext().getStatementId();
        if (!statements.containsKey(statementId)) {
            return;
        }
        removeStatement(statementId);
        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextStatementRemoved(statementContextCreate.getRuntimeURI(), contextRuntimeDescriptor.getContextDeploymentId(), contextRuntimeDescriptor.getContextName(), statement.getLightweight().getStatementContext().getDeploymentId(), statement.getLightweight().getStatementContext().getStatementName()), ContextPartitionStateListener::onContextStatementRemoved);
        if (statements.isEmpty()) {
            getRealization().stopContext();
            contextPartitionIdService.clear();
            ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextDeactivated(statementContextCreate.getRuntimeURI(), contextRuntimeDescriptor.getContextDeploymentId(), contextRuntimeDescriptor.getContextName()), ContextPartitionStateListener::onContextDeactivated);
        }
    }

    public int countStatements(Function<StatementContext, Boolean> filter) {
        int count = 0;
        for (Map.Entry<Integer, ContextControllerStatementDesc> entry : statements.entrySet()) {
            if (filter.apply(entry.getValue().getLightweight().getStatementContext())) {
                count++;
            }
        }
        return count;
    }

    public ContextDefinition getContextDefinition() {
        return contextDefinition;
    }

    public ContextManagerRealization getRealization() {
        StatementResourceHolder statementResourceHolder = statementContextCreate.getStatementCPCacheService().makeOrGetEntryCanNull(-1, statementContextCreate);
        return statementResourceHolder.getContextManagerRealization();
    }

    public void destroyContext() {
        if (!statements.isEmpty()) {
            throw new IllegalStateException("Cannot invoke destroy with statements still attached");
        }
        if (contextPartitionIdService == null) {
            return;
        }
        getRealization().safeDestroyContext();
        contextPartitionIdService.destroy();
        contextPartitionIdService = null;
    }

    public ContextManagerRealization allocateNewRealization(AgentInstanceContext agentInstanceContext) {
        return new ContextManagerRealization(this, agentInstanceContext);
    }

    private void removeStatement(int statementId) {
        ContextControllerStatementDesc statementDesc = statements.get(statementId);
        if (statementDesc == null) {
            return;
        }
        getRealization().removeStatement(statementDesc);
        statements.remove(statementId);
    }

    public Map<Integer, ContextControllerStatementDesc> getStatements() {
        return statements;
    }

    public StatementContext getStatementContextCreate() {
        return statementContextCreate;
    }

    public ContextAgentInstanceInfo getContextAgentInstanceInfo(StatementContext statementContextOfStatement, int agentInstanceId) {
        Object[] partitionKeys = contextPartitionIdService.getPartitionKeys(agentInstanceId);
        if (partitionKeys == null) {
            return null;
        }

        ContextControllerStatementDesc statement = statements.get(statementContextOfStatement.getStatementId());
        MappedEventBean props = ContextManagerUtil.buildContextProperties(agentInstanceId, partitionKeys, contextDefinition, statementContextCreate);
        AgentInstanceFilterProxy proxy = computeFilterAddendum(statement, partitionKeys);
        return new ContextAgentInstanceInfo(props, proxy);
    }

    public AgentInstanceFilterProxy computeFilterAddendum(ContextControllerStatementDesc statement, Object[] contextPartitionKeys) {
        Function<AgentInstanceContext, IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]>> generator = agentInstanceContext ->
                ContextManagerUtil.computeAddendumForStatement(statement, statements, contextDefinition.getControllerFactories(), contextPartitionKeys, agentInstanceContext);
        return new AgentInstanceFilterProxyImpl(generator);
    }

    public ContextRuntimeDescriptor getContextRuntimeDescriptor() {
        return contextRuntimeDescriptor;
    }

    public Iterator<EventBean> iterator(int statementId) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId, new ContextPartitionSelectorAll());
        return new AgentInstanceArrayIterator(instances);
    }

    public SafeIterator<EventBean> safeIterator(int statementId) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId, new ContextPartitionSelectorAll());
        return new AgentInstanceArraySafeIterator(instances);
    }

    public Iterator<EventBean> iterator(int statementId, ContextPartitionSelector selector) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId, selector);
        return new AgentInstanceArrayIterator(instances);
    }

    public SafeIterator<EventBean> safeIterator(int statementId, ContextPartitionSelector selector) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId, selector);
        return new AgentInstanceArraySafeIterator(instances);
    }

    public Map<String, Object> getContextPartitions(int contextPartitionId) {
        for (Map.Entry<Integer, ContextControllerStatementDesc> entry : statements.entrySet()) {
            StatementContext statementContext = entry.getValue().getLightweight().getStatementContext();
            StatementCPCacheService resourceService = statementContext.getStatementCPCacheService();
            StatementResourceHolder holder = resourceService.makeOrGetEntryCanNull(contextPartitionId, statementContext);
            if (holder != null) {
                return ((MappedEventBean) holder.getAgentInstanceContext().getContextProperties()).getProperties();
            }
        }
        return null;
    }

    public MappedEventBean getContextPropertiesEvent(int contextPartitionId) {
        Map<String, Object> props = getContextPartitions(contextPartitionId);
        return statementContextCreate.getEventBeanTypedEventFactory().adapterForTypedMap(props, contextDefinition.getEventTypeContextProperties());
    }

    public ContextPartitionIdentifier getContextPartitionIdentifier(Object[] partitionKeys) {
        if (contextDefinition.getControllerFactories().length == 1) {
            return contextDefinition.getControllerFactories()[0].getContextPartitionIdentifier(partitionKeys[0]);
        }
        ContextPartitionIdentifier[] identifiers = new ContextPartitionIdentifier[partitionKeys.length];
        for (int i = 0; i < partitionKeys.length; i++) {
            identifiers[i] = contextDefinition.getControllerFactories()[i].getContextPartitionIdentifier(partitionKeys[i]);
        }
        return new ContextPartitionIdentifierNested(identifiers);
    }

    public ContextPartitionCollection getContextPartitions(ContextPartitionSelector selector) {
        if (selector instanceof ContextPartitionSelectorAll) {
            Map<Integer, ContextPartitionIdentifier> map = new HashMap<>();
            Collection<Integer> ids = contextPartitionIdService.getIds();
            for (int id : ids) {
                Object[] partitionKeys = contextPartitionIdService.getPartitionKeys(id);
                if (partitionKeys != null) {
                    ContextPartitionIdentifier identifier = getContextPartitionIdentifier(partitionKeys);
                    map.put(id, identifier);
                }
            }
            return new ContextPartitionCollection(map);
        }

        Collection<Integer> ids = getRealization().getAgentInstanceIds(selector);
        Map<Integer, ContextPartitionIdentifier> identifiers = new HashMap<>();
        for (int id : ids) {
            Object[] partitionKeys = contextPartitionIdService.getPartitionKeys(id);
            if (partitionKeys == null) {
                continue;
            }
            ContextPartitionIdentifier identifier = getContextPartitionIdentifier(partitionKeys);
            identifiers.put(id, identifier);
        }
        return new ContextPartitionCollection(identifiers);
    }

    public Set<Integer> getContextPartitionIds(ContextPartitionSelector selector) {
        return new LinkedHashSet<>(contextPartitionIdService.getIds());
    }

    public long getContextPartitionCount() {
        return contextPartitionIdService.getCount();
    }

    public ContextPartitionIdentifier getContextIdentifier(int agentInstanceId) {
        Object[] partitionKeys = contextPartitionIdService.getPartitionKeys(agentInstanceId);
        return partitionKeys == null ? null : getContextPartitionIdentifier(partitionKeys);
    }

    public Collection<Integer> getAgentInstanceIds(ContextPartitionSelector selector) {
        return getRealization().getAgentInstanceIds(selector);
    }

    public StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements) {
        if (contextDefinition.getControllerFactories().length == 1) {
            return contextDefinition.getControllerFactories()[0].allocateAgentInstanceResourceRegistry(registryRequirements);
        }
        return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMap.INSTANCE);
    }

    public ContextPartitionIdService getContextPartitionIdService() {
        return contextPartitionIdService;
    }

    public DataInputOutputSerde[] getContextPartitionKeySerdes() {
        return contextPartitionKeySerdes;
    }

    public int getNumNestingLevels() {
        return contextDefinition.getControllerFactories().length;
    }

    public DataInputOutputSerde[] getContextPartitionKeySerdeSubset(int nestingLevel) {
        DataInputOutputSerde[] serdes = new DataInputOutputSerde[nestingLevel - 1];
        for (int i = 0; i < nestingLevel - 1; i++) {
            serdes[i] = contextPartitionKeySerdes[i];
        }
        return serdes;
    }

    public synchronized void addListener(ContextPartitionStateListener listener) {
        if (listenersLazy == null) {
            listenersLazy = new CopyOnWriteArrayList<>();
        }
        listenersLazy.add(listener);
    }

    public void removeListener(ContextPartitionStateListener listener) {
        if (listenersLazy == null) {
            return;
        }
        listenersLazy.remove(listener);
    }

    public CopyOnWriteArrayList<ContextPartitionStateListener> getListenersMayNull() {
        return listenersLazy;
    }

    public Iterator<ContextPartitionStateListener> getListeners() {
        if (listenersLazy == null) {
            return Collections.emptyIterator();
        }
        return listenersLazy.iterator();
    }

    public void removeListeners() {
        if (listenersLazy == null) {
            return;
        }
        listenersLazy.clear();
    }

    public boolean handleFilterFault(EventBean theEvent, long version) {
        return getRealization().handleFilterFault(theEvent, version);
    }

    public void clearCaches() {
        if (contextPartitionIdService != null) {
            contextPartitionIdService.clearCaches();
        }
    }

    private AgentInstance[] getAgentInstancesForStmt(int statementId, ContextPartitionSelector selector) {
        Collection<Integer> agentInstanceIds = getAgentInstanceIds(selector);
        if (agentInstanceIds == null || agentInstanceIds.isEmpty()) {
            return new AgentInstance[0];
        }

        for (Map.Entry<Integer, ContextControllerStatementDesc> entry : statements.entrySet()) {
            if (entry.getValue().getLightweight().getStatementContext().getStatementId() == statementId) {
                List<AgentInstance> agentInstances = ContextManagerUtil.getAgentInstances(entry.getValue(), agentInstanceIds);
                return agentInstances.toArray(new AgentInstance[agentInstances.size()]);
            }
        }

        return null;
    }
}
