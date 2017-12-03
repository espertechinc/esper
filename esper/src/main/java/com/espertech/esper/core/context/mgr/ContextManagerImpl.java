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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.context.*;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryResult;
import com.espertech.esper.core.context.stmt.StatementAIResourceRegistryFactory;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.context.util.ContextIteratorHandler;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.core.service.resource.StatementResourceService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.filter.FilterFaultHandler;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.*;
import java.util.function.Supplier;

public class ContextManagerImpl extends ContextManagerBase implements ContextManager, ContextControllerLifecycleCallback, ContextIteratorHandler, FilterFaultHandler {

    private final ContextControllerFactory factory;
    private final Map<Integer, ContextControllerStatementDesc> statements = new LinkedHashMap<Integer, ContextControllerStatementDesc>(); // retain order of statement creation
    private final ContextDescriptor contextDescriptor;
    private final Map<Integer, ContextControllerTreeAgentInstanceList> agentInstances = new LinkedHashMap<Integer, ContextControllerTreeAgentInstanceList>();

    /**
     * The single root context.
     * This represents the context declared first.
     */
    private ContextController rootContext;
    private final ContextPartitionIdManager contextPartitionIdManager;

    public ContextManagerImpl(ContextControllerFactoryServiceContext factoryServiceContext)
            throws ExprValidationException {
        super(factoryServiceContext.getContextName(), factoryServiceContext.getServicesContext());
        this.factory = factoryServiceContext.getAgentInstanceContextCreate().getStatementContext().getContextControllerFactoryService().getFactory(factoryServiceContext)[0];
        this.rootContext = factory.createNoCallback(0, this);   // single instance: created here and activated/deactivated later
        this.contextPartitionIdManager = factoryServiceContext.getAgentInstanceContextCreate().getStatementContext().getContextControllerFactoryService().allocatePartitionIdMgr(contextName, factoryServiceContext.getAgentInstanceContextCreate().getStatementContext().getStatementId());

        StatementAIResourceRegistryFactory resourceRegistryFactory = factory.getStatementAIResourceRegistryFactory();

        Map<String, Object> contextProps = factory.getContextBuiltinProps();
        EventType contextPropsType = servicesContext.getEventAdapterService().createAnonymousMapType(contextName, contextProps, true);
        ContextPropertyRegistryImpl registry = new ContextPropertyRegistryImpl(factory.getContextDetailPartitionItems(), contextPropsType);
        contextDescriptor = new ContextDescriptor(contextName, factory.isSingleInstanceContext(), registry, resourceRegistryFactory, this, factory.getContextDetail());
    }

    public int getNumNestingLevels() {
        return 1;
    }

    public Map<Integer, ContextControllerStatementDesc> getStatements() {
        return statements;
    }

    public ContextDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    public ContextStateCache getContextStateCache() {
        return factory.getStateCache();
    }

    public void addStatement(ContextControllerStatementBase statement, boolean isRecoveringResilient) throws ExprValidationException {

        // validation down the hierarchy
        ContextControllerStatementCtxCache caches = factory.validateStatement(statement);

        // add statement
        ContextControllerStatementDesc desc = new ContextControllerStatementDesc(statement, new ContextControllerStatementCtxCache[]{caches});
        statements.put(statement.getStatementContext().getStatementId(), desc);
        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextStatementAdded(servicesContext.getEngineURI(), contextName, statement.getStatementContext().getStatementName()), ContextPartitionStateListener::onContextStatementAdded);

        // activate if this is the first statement
        if (statements.size() == 1) {
            activate();     // this may itself trigger a callback
        } else {
            // activate statement in respect to existing context partitions
            for (Map.Entry<Integer, ContextControllerTreeAgentInstanceList> entry : agentInstances.entrySet()) {
                if (entry.getValue().getState() == ContextPartitionState.STARTED) {
                    AgentInstance agentInstance = startStatement(entry.getKey(), desc, rootContext, entry.getValue().getInitPartitionKey(), entry.getValue().getInitContextProperties(), isRecoveringResilient);
                    entry.getValue().getAgentInstances().add(agentInstance);
                }
            }
        }
    }

    public synchronized void stopStatement(String statementName, int statementId) {
        destroyStatement(statementName, statementId);
    }

    public synchronized void destroyStatement(String statementName, int statementId) {
        if (!statements.containsKey(statementId)) {
            return;
        }
        if (statements.size() == 1) {
            safeDestroy();
        } else {
            removeStatement(statementId);
        }
        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextStatementRemoved(servicesContext.getEngineURI(), contextName, statementName), ContextPartitionStateListener::onContextStatementRemoved);
    }

    public void safeDestroy() {
        if (rootContext != null) {
            // deactivate
            rootContext.deactivate();
            factory.getFactoryContext().getStateCache().removeContext(contextName);

            for (Map.Entry<Integer, ContextControllerTreeAgentInstanceList> entryCP : agentInstances.entrySet()) {
                StatementAgentInstanceUtil.stopAgentInstances(entryCP.getValue().getAgentInstances(), null, servicesContext, true, false, listenersLazy, entryCP.getKey(), contextName);
            }
            agentInstances.clear();
            contextPartitionIdManager.clear();
            boolean hasStatements = statements.size() > 0;
            statements.clear();
            if (hasStatements) {
                ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextDeactivated(servicesContext.getEngineURI(), contextName), ContextPartitionStateListener::onContextDeactivated);
            }
        }
    }

    public synchronized ContextControllerInstanceHandle contextPartitionInstantiate(
            Integer optionalContextPartitionId,
            int subPathId,
            Integer importSubpathId,
            ContextController originator,
            EventBean optionalTriggeringEvent,
            Map<String, Object> optionalTriggeringPattern,
            Object partitionKey,
            Map<String, Object> contextProperties,
            ContextControllerState states,
            ContextInternalFilterAddendum filterAddendum,
            boolean isRecoveringResilient,
            ContextPartitionState state,
            Supplier<ContextPartitionIdentifier> identifier) {

        // assign context id
        int assignedContextId;
        if (optionalContextPartitionId != null && !states.isImported()) {
            assignedContextId = optionalContextPartitionId;
            contextPartitionIdManager.addExisting(optionalContextPartitionId);
        } else {
            assignedContextId = contextPartitionIdManager.allocateId();
            if (states != null && states.getPartitionImportCallback() != null && optionalContextPartitionId != null) {
                states.getPartitionImportCallback().allocated(assignedContextId, optionalContextPartitionId);
            }
        }

        // handle leaf creation
        List<AgentInstance> newInstances = new ArrayList<AgentInstance>();
        if (state == ContextPartitionState.STARTED) {
            for (Map.Entry<Integer, ContextControllerStatementDesc> statementEntry : statements.entrySet()) {
                ContextControllerStatementDesc statementDesc = statementEntry.getValue();
                AgentInstance instance = startStatement(assignedContextId, statementDesc, originator, partitionKey, contextProperties, isRecoveringResilient);
                newInstances.add(instance);
            }
        }

        // for all new contexts: evaluate this event for this statement
        if (optionalTriggeringEvent != null || optionalTriggeringPattern != null) {
            StatementAgentInstanceUtil.evaluateEventForStatement(servicesContext, optionalTriggeringEvent, optionalTriggeringPattern, newInstances);
        }

        // save leaf
        long filterVersion = servicesContext.getFilterService().getFiltersVersion();
        ContextControllerTreeAgentInstanceList agentInstanceList = new ContextControllerTreeAgentInstanceList(filterVersion, partitionKey, contextProperties, newInstances, state);
        agentInstances.put(assignedContextId, agentInstanceList);

        // dispatch event
        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextPartitionAllocated(servicesContext.getEngineURI(), contextName, assignedContextId, identifier.get()), ContextPartitionStateListener::onContextPartitionAllocated);

        return new ContextNestedHandleImpl(subPathId, assignedContextId, agentInstanceList);
    }

    public synchronized void contextPartitionTerminate(ContextControllerInstanceHandle contextNestedHandle, Map<String, Object> terminationProperties, boolean leaveLocksAcquired, List<AgentInstance> agentInstancesCollected) {
        ContextNestedHandleImpl handle = (ContextNestedHandleImpl) contextNestedHandle;
        ContextControllerTreeAgentInstanceList entry = agentInstances.remove(handle.getContextPartitionOrPathId());
        if (entry != null) {
            StatementAgentInstanceUtil.stopAgentInstances(entry.getAgentInstances(), terminationProperties, servicesContext, false, leaveLocksAcquired, listenersLazy, handle.getContextPartitionOrPathId(), contextName);
            if (agentInstancesCollected != null) {
                agentInstancesCollected.addAll(entry.getAgentInstances());
            }
            entry.getAgentInstances().clear();
            contextPartitionIdManager.removeId(contextNestedHandle.getContextPartitionOrPathId());
        }
    }

    public void contextPartitionNavigate(ContextControllerInstanceHandle existingHandle, ContextController originator, ContextControllerState controllerState, int exportedCPOrPathId, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector, byte[] payload, boolean isRecoveringResilient) {
        ContextControllerTreeAgentInstanceList entry = agentInstances.get(existingHandle.getContextPartitionOrPathId());
        if (entry == null) {
            return;
        }

        if (entry.getState() == ContextPartitionState.STOPPED) {
            entry.setState(ContextPartitionState.STARTED);
            entry.getAgentInstances().clear();
            for (Map.Entry<Integer, ContextControllerStatementDesc> statement : statements.entrySet()) {
                AgentInstance instance = startStatement(existingHandle.getContextPartitionOrPathId(), statement.getValue(), originator, entry.getInitPartitionKey(), entry.getInitContextProperties(), false);
                entry.getAgentInstances().add(instance);
            }
            ContextStatePathKey key = new ContextStatePathKey(1, 0, existingHandle.getSubPathId());
            ContextStatePathValue value = new ContextStatePathValue(existingHandle.getContextPartitionOrPathId(), payload, ContextPartitionState.STARTED);
            rootContext.getFactory().getFactoryContext().getStateCache().updateContextPath(contextName, key, value);
        } else {
            List<AgentInstance> removed = new ArrayList<AgentInstance>(2);
            List<AgentInstance> added = new ArrayList<AgentInstance>(2);
            for (AgentInstance agentInstance : entry.getAgentInstances()) {
                if (!agentInstanceSelector.select(agentInstance)) {
                    continue;
                }

                // remove
                StatementAgentInstanceUtil.stopAgentInstanceRemoveResources(agentInstance, null, servicesContext, false, false);
                removed.add(agentInstance);

                // start
                ContextControllerStatementDesc statementDesc = statements.get(agentInstance.getAgentInstanceContext().getStatementId());
                AgentInstance instance = startStatement(existingHandle.getContextPartitionOrPathId(), statementDesc, originator, entry.getInitPartitionKey(), entry.getInitContextProperties(), isRecoveringResilient);
                added.add(instance);

                if (controllerState.getPartitionImportCallback() != null) {
                    controllerState.getPartitionImportCallback().existing(existingHandle.getContextPartitionOrPathId(), exportedCPOrPathId);
                }
            }
            entry.getAgentInstances().removeAll(removed);
            entry.getAgentInstances().addAll(added);
        }
    }

    public ExprFilterSpecLookupable getFilterLookupable(EventType eventType) {
        return factory.getFilterLookupable(eventType);
    }

    public synchronized Iterator<EventBean> iterator(int statementId) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId);
        return new AgentInstanceArrayIterator(instances);
    }

    public synchronized SafeIterator<EventBean> safeIterator(int statementId) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId);
        return new AgentInstanceArraySafeIterator(instances);
    }

    public synchronized Iterator<EventBean> iterator(int statementId, ContextPartitionSelector selector) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId, selector);
        return new AgentInstanceArrayIterator(instances);
    }

    public synchronized SafeIterator<EventBean> safeIterator(int statementId, ContextPartitionSelector selector) {
        AgentInstance[] instances = getAgentInstancesForStmt(statementId, selector);
        return new AgentInstanceArraySafeIterator(instances);
    }

    public Collection<Integer> getAgentInstanceIds(ContextPartitionSelector selector) {
        if (selector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById byId = (ContextPartitionSelectorById) selector;
            Set<Integer> ids = byId.getContextPartitionIds();
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }
            ArrayList agentInstanceIds = new ArrayList<Integer>(ids);
            agentInstanceIds.retainAll(agentInstances.keySet());
            return agentInstanceIds;
        } else if (selector instanceof ContextPartitionSelectorAll) {
            return new ArrayList<Integer>(agentInstances.keySet());
        } else {
            ContextPartitionVisitorAgentInstanceId visitor = new ContextPartitionVisitorAgentInstanceId(1);
            rootContext.visitSelectedPartitions(selector, visitor);
            return visitor.getAgentInstanceIds();
        }
    }

    public ContextStatePathDescriptor extractPaths(ContextPartitionSelector selector) {
        ContextPartitionVisitorState visitor = new ContextPartitionVisitorState();
        rootContext.visitSelectedPartitions(selector, visitor);
        return new ContextStatePathDescriptor(visitor.getStates(), visitor.getContextPartitionInfo());
    }

    public ContextStatePathDescriptor extractStopPaths(ContextPartitionSelector selector) {
        ContextStatePathDescriptor states = extractPaths(selector);
        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : states.getPaths().entrySet()) {
            int agentInstanceId = entry.getValue().getOptionalContextPartitionId();
            ContextControllerTreeAgentInstanceList list = agentInstances.get(agentInstanceId);
            list.setState(ContextPartitionState.STOPPED);
            StatementAgentInstanceUtil.stopAgentInstances(list.getAgentInstances(), null, servicesContext, false, false, listenersLazy, agentInstanceId, contextName);
            list.clearAgentInstances();
            entry.getValue().setState(ContextPartitionState.STOPPED);
            rootContext.getFactory().getFactoryContext().getStateCache().updateContextPath(contextName, entry.getKey(), entry.getValue());
        }
        return states;
    }

    public ContextStatePathDescriptor extractDestroyPaths(ContextPartitionSelector selector) {
        ContextStatePathDescriptor states = extractPaths(selector);
        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : states.getPaths().entrySet()) {
            int agentInstanceId = entry.getValue().getOptionalContextPartitionId();
            ContextPartitionDescriptor descriptor = states.getContextPartitionInformation().get(agentInstanceId);
            rootContext.deletePath(descriptor.getIdentifier());
            ContextControllerTreeAgentInstanceList list = agentInstances.remove(agentInstanceId);
            StatementAgentInstanceUtil.stopAgentInstances(list.getAgentInstances(), null, servicesContext, false, false, listenersLazy, agentInstanceId, contextName);
            list.clearAgentInstances();
            rootContext.getFactory().getFactoryContext().getStateCache().removeContextPath(contextName, entry.getKey().getLevel(), entry.getKey().getParentPath(), entry.getKey().getSubPath());
        }
        return states;
    }

    public Map<Integer, ContextPartitionDescriptor> startPaths(ContextPartitionSelector selector) {
        ContextStatePathDescriptor states = extractPaths(selector);
        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : states.getPaths().entrySet()) {
            int agentInstanceId = entry.getValue().getOptionalContextPartitionId();
            ContextControllerTreeAgentInstanceList list = agentInstances.get(agentInstanceId);
            if (list.getState() == ContextPartitionState.STARTED) {
                continue;
            }
            list.setState(ContextPartitionState.STARTED);
            entry.getValue().setState(ContextPartitionState.STARTED);
            for (Map.Entry<Integer, ContextControllerStatementDesc> statement : statements.entrySet()) {
                AgentInstance instance = startStatement(agentInstanceId, statement.getValue(), rootContext, list.getInitPartitionKey(), list.getInitContextProperties(), false);
                list.getAgentInstances().add(instance);
            }
            rootContext.getFactory().getFactoryContext().getStateCache().updateContextPath(contextName, entry.getKey(), entry.getValue());
        }
        setState(states.getContextPartitionInformation(), ContextPartitionState.STARTED);
        return states.getContextPartitionInformation();
    }

    public void importStartPaths(ContextControllerState state, AgentInstanceSelector agentInstanceSelector) {
        rootContext.importContextPartitions(state, 0, null, agentInstanceSelector);
    }

    public synchronized boolean handleFilterFault(EventBean theEvent, long version) {
        StatementAgentInstanceUtil.handleFilterFault(theEvent, version, servicesContext, agentInstances);
        return false;
    }

    private void activate() {
        rootContext.activate(null, null, null, null, null);
        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextActivated(servicesContext.getEngineURI(), contextName), ContextPartitionStateListener::onContextActivated);
    }

    private AgentInstance[] getAgentInstancesForStmt(int statementId, ContextPartitionSelector selector) {
        Collection<Integer> agentInstanceIds = getAgentInstanceIds(selector);
        if (agentInstanceIds == null || agentInstanceIds.isEmpty()) {
            return new AgentInstance[0];
        }

        List<AgentInstance> instances = new ArrayList<AgentInstance>(agentInstanceIds.size());
        for (Integer agentInstanceId : agentInstanceIds) {
            ContextControllerTreeAgentInstanceList instancesList = agentInstances.get(agentInstanceId);
            if (instancesList != null) {
                Iterator<AgentInstance> instanceIt = instancesList.getAgentInstances().iterator();
                for (; instanceIt.hasNext(); ) {
                    AgentInstance instance = instanceIt.next();
                    if (instance.getAgentInstanceContext().getStatementContext().getStatementId() == statementId) {
                        instances.add(instance);
                    }
                }
            }
        }
        return instances.toArray(new AgentInstance[instances.size()]);
    }

    private AgentInstance[] getAgentInstancesForStmt(int statementId) {
        List<AgentInstance> instances = new ArrayList<AgentInstance>();
        for (Map.Entry<Integer, ContextControllerTreeAgentInstanceList> contextPartitionEntry : agentInstances.entrySet()) {
            Iterator<AgentInstance> instanceIt = contextPartitionEntry.getValue().getAgentInstances().iterator();
            for (; instanceIt.hasNext(); ) {
                AgentInstance instance = instanceIt.next();
                if (instance.getAgentInstanceContext().getStatementContext().getStatementId() == statementId) {
                    instances.add(instance);
                }
            }
        }
        return instances.toArray(new AgentInstance[instances.size()]);
    }

    private void removeStatement(int statementId) {
        ContextControllerStatementDesc statementDesc = statements.get(statementId);
        if (statementDesc == null) {
            return;
        }

        for (Map.Entry<Integer, ContextControllerTreeAgentInstanceList> contextPartitionEntry : agentInstances.entrySet()) {
            Iterator<AgentInstance> instanceIt = contextPartitionEntry.getValue().getAgentInstances().iterator();
            for (; instanceIt.hasNext(); ) {
                AgentInstance instance = instanceIt.next();
                if (instance.getAgentInstanceContext().getStatementContext().getStatementId() != statementId) {
                    continue;
                }
                StatementAgentInstanceUtil.stop(instance.getStopCallback(), instance.getAgentInstanceContext(), instance.getFinalView(), servicesContext, true, false, true);
                instanceIt.remove();
            }
        }

        statements.remove(statementId);
    }

    private AgentInstance startStatement(int contextId, ContextControllerStatementDesc statementDesc, ContextController originator, Object partitionKey, Map<String, Object> contextProperties, boolean isRecoveringResilient) {

        // build filters
        IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum = new IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]>();
        originator.getFactory().populateFilterAddendums(filterAddendum, statementDesc, partitionKey, contextId);
        AgentInstanceFilterProxy proxy = new AgentInstanceFilterProxyImpl(filterAddendum);

        // build built-in context properties
        contextProperties.put(ContextPropertyEventType.PROP_CTX_NAME, contextName);
        contextProperties.put(ContextPropertyEventType.PROP_CTX_ID, contextId);
        MappedEventBean contextBean = (MappedEventBean) servicesContext.getEventAdapterService().adapterForTypedMap(contextProperties, contextDescriptor.getContextPropertyRegistry().getContextEventType());

        // activate
        StatementAgentInstanceFactoryResult result = StatementAgentInstanceUtil.start(servicesContext, statementDesc.getStatement(), false, contextId, contextBean, proxy, isRecoveringResilient);

        // save only instance data
        return new AgentInstance(result.getStopCallback(), result.getAgentInstanceContext(), result.getFinalView());
    }

    protected static void setState(Map<Integer, ContextPartitionDescriptor> original, ContextPartitionState state) {
        for (Map.Entry<Integer, ContextPartitionDescriptor> entry : original.entrySet()) {
            entry.getValue().setState(state);
        }
    }

    public Map<String, Object> getContextProperties(int contextPartitionId) {
        return getContextPropertiesFirstStmt(contextPartitionId, statements);
    }

    static Map<String, Object> getContextPropertiesFirstStmt(int contextPartitionId, Map<Integer, ContextControllerStatementDesc> statements) {
        for (Map.Entry<Integer, ContextControllerStatementDesc> entry : statements.entrySet()) {
            StatementContext statementContext = entry.getValue().getStatement().getStatementContext();
            StatementResourceService resourceService = statementContext.getStatementExtensionServicesContext().getStmtResources();
            StatementResourceHolder holder = resourceService.getPartitioned(contextPartitionId);
            if (holder != null) {
                return holder.getAgentInstanceContext().getContextProperties().getProperties();
            }
        }
        return null;
    }

    public static class ContextNestedHandleImpl implements ContextControllerInstanceHandle {
        private final int subPathId;
        private final int contextPartitionId;
        private final ContextControllerTreeAgentInstanceList instances;

        public ContextNestedHandleImpl(int subPathId, int contextPartitionId, ContextControllerTreeAgentInstanceList instances) {
            this.subPathId = subPathId;
            this.contextPartitionId = contextPartitionId;
            this.instances = instances;
        }

        public Integer getContextPartitionOrPathId() {
            return contextPartitionId;
        }

        public ContextControllerTreeAgentInstanceList getInstances() {
            return instances;
        }

        public int getSubPathId() {
            return subPathId;
        }
    }
}
