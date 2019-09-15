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
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.collection.IntSeqKeyRoot;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactory;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerLifecycleCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;
import java.util.function.Function;

public class ContextManagerRealization implements ContextControllerLifecycleCallback, FilterFaultHandler {
    private final ContextManagerResident contextManager;
    private final AgentInstanceContext agentInstanceContextCreate;
    private final ContextController[] contextControllers;

    public ContextManagerRealization(ContextManagerResident contextManager, AgentInstanceContext agentInstanceContextCreate) {
        this.contextManager = contextManager;
        this.agentInstanceContextCreate = agentInstanceContextCreate;

        // create controllers
        ContextControllerFactory[] controllerFactories = contextManager.getContextDefinition().getControllerFactories();
        contextControllers = new ContextController[controllerFactories.length];
        for (int i = 0; i < controllerFactories.length; i++) {
            ContextControllerFactory contextControllerFactory = controllerFactories[i];
            contextControllers[i] = contextControllerFactory.create(this);
        }
    }

    public ContextController[] getContextControllers() {
        return contextControllers;
    }

    public void startContext() {
        contextControllers[0].activate(IntSeqKeyRoot.INSTANCE, new Object[0], null, null);
    }

    public void stopContext() {
        contextControllers[0].deactivate(IntSeqKeyRoot.INSTANCE, contextControllers.length > 1);
    }

    public void safeDestroyContext() {
        // destroy context controllers
        for (ContextController controllerPage : contextControllers) {
            controllerPage.destroy();
        }

        // remove realization
        StatementContext statementContext = agentInstanceContextCreate.getStatementContext();
        statementContext.getStatementCPCacheService().clear(statementContext);
    }

    public ContextPartitionInstantiationResult contextPartitionInstantiate(IntSeqKey controllerPathId,
                                                                           int subpathId,
                                                                           ContextController originator,
                                                                           EventBean optionalTriggeringEvent,
                                                                           Map<String, Object> optionalPatternForInclusiveEval,
                                                                           Object[] parentPartitionKeys,
                                                                           Object partitionKey) {


        // detect non-leaf
        ContextControllerFactoryEnv controllerEnv = originator.getFactory().getFactoryEnv();
        if (controllerPathId.length() != controllerEnv.getNestingLevel() - 1) {
            throw new IllegalStateException("Unexpected controller path");
        }
        if (parentPartitionKeys.length != controllerEnv.getNestingLevel() - 1) {
            throw new IllegalStateException("Unexpected partition key size");
        }

        int nestingLevel = controllerEnv.getNestingLevel();   // starts at 1 for root
        if (nestingLevel < contextControllers.length) {

            // next sub-sontext
            ContextController nextContext = contextControllers[nestingLevel];

            // add a partition key
            Object[] nestedPartitionKeys = addPartitionKey(nestingLevel, parentPartitionKeys, partitionKey);

            // now post-initialize, this may actually call back
            IntSeqKey childPath = controllerPathId.addToEnd(subpathId);
            nextContext.activate(childPath, nestedPartitionKeys, optionalTriggeringEvent, optionalPatternForInclusiveEval);

            return new ContextPartitionInstantiationResult(subpathId, Collections.emptyList());
        }

        // assign context id
        Object[] allPartitionKeys = CollectionUtil.addValue(parentPartitionKeys, partitionKey);
        int assignedContextId = contextManager.getContextPartitionIdService().allocateId(allPartitionKeys);

        // build built-in context properties
        MappedEventBean contextBean = ContextManagerUtil.buildContextProperties(assignedContextId, allPartitionKeys, contextManager.getContextDefinition(), agentInstanceContextCreate.getStatementContext());

        // handle leaf creation
        List<AgentInstance> startedInstances = new ArrayList<AgentInstance>(2);
        for (Map.Entry<Integer, ContextControllerStatementDesc> statementEntry : contextManager.getStatements().entrySet()) {
            ContextControllerStatementDesc statementDesc = statementEntry.getValue();

            Function<AgentInstanceContext, IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]>> generator = agentInstanceContext ->
                    ContextManagerUtil.computeAddendumForStatement(statementDesc, contextManager.getStatements(), contextManager.getContextDefinition().getControllerFactories(), allPartitionKeys, agentInstanceContext);
            AgentInstanceFilterProxy proxy = new AgentInstanceFilterProxyImpl(generator);

            AgentInstance agentInstance = AgentInstanceUtil.startStatement(contextManager.getStatementContextCreate().getStatementContextRuntimeServices(), assignedContextId, statementDesc, contextBean, proxy);
            startedInstances.add(agentInstance);
        }

        // for all new contexts: evaluate this event for this statement
        if (optionalTriggeringEvent != null || optionalPatternForInclusiveEval != null) {
            // comment-in: log.info("Thread " + Thread.currentThread().getId() + " event " + optionalTriggeringEvent.getUnderlying() + " evaluateEventForStatement assignedContextId=" + assignedContextId);
            AgentInstanceUtil.evaluateEventForStatement(optionalTriggeringEvent, optionalPatternForInclusiveEval, startedInstances, agentInstanceContextCreate);
        }

        if (contextManager.getListenersMayNull() != null) {
            ContextPartitionIdentifier identifier = contextManager.getContextPartitionIdentifier(allPartitionKeys);
            ContextStateEventUtil.dispatchPartition(contextManager.getListenersMayNull(), () -> new ContextStateEventContextPartitionAllocated(agentInstanceContextCreate.getRuntimeURI(), contextManager.getContextRuntimeDescriptor().getContextDeploymentId(), contextManager.getContextDefinition().getContextName(), assignedContextId, identifier), ContextPartitionStateListener::onContextPartitionAllocated);
        }

        return new ContextPartitionInstantiationResult(assignedContextId, startedInstances);
    }

    public void contextPartitionTerminate(IntSeqKey controllerPath, int subpathIdOrCPId, ContextController originator, Map<String, Object> terminationProperties, boolean leaveLocksAcquired, List<AgentInstance> agentInstancesLocksHeld) {

        if (controllerPath.length() != originator.getFactory().getFactoryEnv().getNestingLevel() - 1) {
            throw new IllegalStateException("Unrecognized controller path");
        }

        // detect non-leaf
        ContextControllerFactoryEnv controllerEnv = originator.getFactory().getFactoryEnv();
        int nestingLevel = controllerEnv.getNestingLevel();   // starts at 1 for root
        if (nestingLevel < contextControllers.length) {
            ContextController childController = contextControllers[nestingLevel];
            IntSeqKey path = controllerPath.addToEnd(subpathIdOrCPId);
            childController.deactivate(path, true);
            return;
        }

        int agentInstanceId = subpathIdOrCPId;

        // stop - in reverse order of statements, to allow termination to use tables+named-windows
        ListIterator<ContextControllerStatementDesc> iterator = new ArrayList<>(contextManager.getStatements().values()).listIterator(contextManager.getStatements().size());
        while (iterator.hasPrevious()) {
            ContextControllerStatementDesc statementDesc = iterator.previous();
            AgentInstanceUtil.contextPartitionTerminate(agentInstanceId, statementDesc, terminationProperties, leaveLocksAcquired, agentInstancesLocksHeld);
        }

        // remove all context partition statement resources
        for (Map.Entry<Integer, ContextControllerStatementDesc> statementEntry : contextManager.getStatements().entrySet()) {
            ContextControllerStatementDesc statementDesc = statementEntry.getValue();
            StatementResourceService svc = statementDesc.getLightweight().getStatementContext().getStatementCPCacheService().getStatementResourceService();
            StatementResourceHolder holder = svc.deallocatePartitioned(agentInstanceId);
        }

        // remove id
        contextManager.getContextPartitionIdService().removeId(agentInstanceId);
        if (contextManager.getListenersMayNull() != null) {
            ContextStateEventUtil.dispatchPartition(contextManager.getListenersMayNull(), () -> new ContextStateEventContextPartitionDeallocated(agentInstanceContextCreate.getRuntimeURI(), contextManager.getContextRuntimeDescriptor().getContextDeploymentId(), contextManager.getContextDefinition().getContextName(), agentInstanceId), ContextPartitionStateListener::onContextPartitionDeallocated);
        }
    }

    public void startLateStatement(ContextControllerStatementDesc statement) {
        Collection<Integer> ids = contextManager.getContextPartitionIdService().getIds();
        for (int cpid : ids) {
            Object[] partitionKeys = contextManager.getContextPartitionIdService().getPartitionKeys(cpid);

            // create context properties bean
            MappedEventBean contextBean = ContextManagerUtil.buildContextProperties(cpid, partitionKeys, contextManager.getContextDefinition(), agentInstanceContextCreate.getStatementContext());

            // create filter proxies
            Function<AgentInstanceContext, IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]>> generator = agentInstanceContext ->
                    ContextManagerUtil.computeAddendumForStatement(statement, contextManager.getStatements(), contextManager.getContextDefinition().getControllerFactories(), partitionKeys, agentInstanceContext);
            AgentInstanceFilterProxy proxy = new AgentInstanceFilterProxyImpl(generator);

            // start
            AgentInstanceUtil.startStatement(contextManager.getStatementContextCreate().getStatementContextRuntimeServices(), cpid, statement, contextBean, proxy);
        }
    }

    public AgentInstanceContext getAgentInstanceContextCreate() {
        return agentInstanceContextCreate;
    }

    public Collection<Integer> getAgentInstanceIds(ContextPartitionSelector selector) {
        if (selector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById byId = (ContextPartitionSelectorById) selector;
            Set<Integer> ids = byId.getContextPartitionIds();
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }
            ArrayList<Integer> agentInstanceIds = new ArrayList<Integer>(ids);
            agentInstanceIds.retainAll(contextManager.getContextPartitionIdService().getIds());
            return agentInstanceIds;
        } else if (selector instanceof ContextPartitionSelectorAll) {
            return contextManager.getContextPartitionIdService().getIds();
        } else if (selector instanceof ContextPartitionSelectorNested) {
            if (contextControllers.length == 1) {
                throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorNested.class}, selector, true);
            }
            ContextPartitionSelectorNested nested = (ContextPartitionSelectorNested) selector;
            ContextPartitionVisitorAgentInstanceId visitor = new ContextPartitionVisitorAgentInstanceId(contextControllers.length);
            for (ContextPartitionSelector[] stack : nested.getSelectors()) {
                contextControllers[0].visitSelectedPartitions(IntSeqKeyRoot.INSTANCE, stack[0], visitor, stack);
            }
            return visitor.getIds();
        } else {
            if (contextControllers.length > 1) {
                throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorAll.class, ContextPartitionSelectorById.class, ContextPartitionSelectorNested.class}, selector, true);
            }
            ContextPartitionVisitorAgentInstanceId visitor = new ContextPartitionVisitorAgentInstanceId(contextControllers.length);
            contextControllers[0].visitSelectedPartitions(IntSeqKeyRoot.INSTANCE, selector, visitor, new ContextPartitionSelector[]{selector});
            return visitor.getIds();
        }
    }

    public void removeStatement(ContextControllerStatementDesc statementDesc) {
        Collection<Integer> ids = contextManager.getContextPartitionIdService().getIds();
        for (Integer id : ids) {
            AgentInstanceUtil.contextPartitionTerminate(id, statementDesc, null, false, null);
        }
    }

    public void contextPartitionRecursiveVisit(IntSeqKey controllerPath, int subpathOrAgentInstanceId, ContextController originator, ContextPartitionVisitor visitor, ContextPartitionSelector[] selectorPerLevel) {
        if (controllerPath.length() != originator.getFactory().getFactoryEnv().getNestingLevel() - 1) {
            throw new IllegalStateException("Unrecognized controller path");
        }
        int nestingLevel = originator.getFactory().getFactoryEnv().getNestingLevel();   // starts at 1 for root
        if (nestingLevel < contextControllers.length) {
            ContextController childController = contextControllers[nestingLevel];
            IntSeqKey subPath = controllerPath.addToEnd(subpathOrAgentInstanceId);
            childController.visitSelectedPartitions(subPath, selectorPerLevel[nestingLevel], visitor, selectorPerLevel);
            return;
        }
        visitor.add(subpathOrAgentInstanceId, originator.getFactory().getFactoryEnv().getNestingLevel());
    }

    public ContextManagerResident getContextManager() {
        return contextManager;
    }

    public void activateCreateVariableStatement(ContextControllerStatementDesc statement) {
        Collection<Integer> ids = contextManager.getContextPartitionIdService().getIds();
        ContextManagerUtil.getAgentInstances(statement, ids);
    }

    public boolean handleFilterFault(EventBean theEvent, long version) {
        // We handle context-management filter faults always the same way.
        // Statement-partition filter faults are specific to the controller.
        //
        // Hashed-context without preallocate: every time a new bucket shows up the filter version changes, faulting for those that are not aware of the new bucket
        // Example:
        //   T0 sends {key='A', id='E1'}
        //   T1 sends {key='A', id='E1'}
        //   T0 receives create-ctx-ai-lock, processes "matchFound", allocates stmt, which adds filter, sets filter version
        //   T1 encounteres newer filter version, invokes filter fault handler, evaluates event against filters, passes event to statement-partition
        //
        // To avoid duplicate processing into the statement-partition, filter by comparing statement-partition filter version.
        Collection<Integer> ids = contextManager.getContextPartitionIdService().getIds();
        for (Map.Entry<Integer, ContextControllerStatementDesc> stmt : contextManager.getStatements().entrySet()) {
            List<AgentInstance> agentInstances = ContextManagerUtil.getAgentInstancesFiltered(stmt.getValue(), ids, agentInstance -> agentInstance.getAgentInstanceContext().getFilterVersionAfterAllocation() >= version);
            AgentInstanceUtil.evaluateEventForStatement(theEvent, null, agentInstances, agentInstanceContextCreate);
        }
        return false;
    }

    private Object[] addPartitionKey(int nestingLevel, Object[] parentPartitionKeys, Object partitionKey) {
        Object[] keysPerContext = new Object[nestingLevel];
        if (nestingLevel > 1) {
            System.arraycopy(parentPartitionKeys, 0, keysPerContext, 0, parentPartitionKeys.length);
        }
        keysPerContext[nestingLevel - 1] = partitionKey;
        return keysPerContext;
    }
}
