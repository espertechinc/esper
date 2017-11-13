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
import com.espertech.esper.client.context.*;
import com.espertech.esper.core.context.util.ContextControllerSelectorUtil;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.epl.spec.ContextDetailHashItem;

import java.util.*;

public class ContextControllerHash implements ContextController, ContextControllerHashedInstanceCallback {

    protected final int pathId;
    protected final ContextControllerLifecycleCallback activationCallback;
    protected final ContextControllerHashFactoryImpl factory;

    protected final List<ContextControllerHashedFilterCallback> filterCallbacks = new ArrayList<ContextControllerHashedFilterCallback>();
    protected final Map<Integer, ContextControllerInstanceHandle> partitionKeys = new LinkedHashMap<Integer, ContextControllerInstanceHandle>();

    protected ContextInternalFilterAddendum activationFilterAddendum;
    protected int currentSubpathId;

    public ContextControllerHash(int pathId, ContextControllerLifecycleCallback activationCallback, ContextControllerHashFactoryImpl factory) {
        this.pathId = pathId;
        this.activationCallback = activationCallback;
        this.factory = factory;
    }

    public void importContextPartitions(ContextControllerState state, int pathIdToUse, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector) {
        initializeFromState(null, null, state, pathIdToUse, agentInstanceSelector, true);
    }

    public void deletePath(ContextPartitionIdentifier identifier) {
        ContextPartitionIdentifierHash hash = (ContextPartitionIdentifierHash) identifier;
        partitionKeys.remove(hash.getHash());
    }

    public void visitSelectedPartitions(ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor) {
        int nestingLevel = factory.getFactoryContext().getNestingLevel();

        if (contextPartitionSelector instanceof ContextPartitionSelectorHash) {
            ContextPartitionSelectorHash hash = (ContextPartitionSelectorHash) contextPartitionSelector;
            if (hash.getHashes() == null || hash.getHashes().isEmpty()) {
                return;
            }
            for (int hashCode : hash.getHashes()) {
                ContextControllerInstanceHandle handle = partitionKeys.get(hashCode);
                if (handle != null) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), hashCode, this, handle);
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filter = (ContextPartitionSelectorFiltered) contextPartitionSelector;
            ContextPartitionIdentifierHash identifierHash = new ContextPartitionIdentifierHash();
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : partitionKeys.entrySet()) {
                identifierHash.setHash(entry.getKey());
                identifierHash.setContextPartitionId(entry.getValue().getContextPartitionOrPathId());
                if (filter.filter(identifierHash)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorAll) {
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : partitionKeys.entrySet()) {
                visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById byId = (ContextPartitionSelectorById) contextPartitionSelector;
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : partitionKeys.entrySet()) {
                int cpid = entry.getValue().getContextPartitionOrPathId();
                if (byId.getContextPartitionIds().contains(cpid)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
                }
            }
            return;
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorHash.class}, contextPartitionSelector);
    }

    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState controllerState, ContextInternalFilterAddendum activationFilterAddendum, Integer importPathId) {
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        this.activationFilterAddendum = activationFilterAddendum;

        if (factoryContext.getNestingLevel() == 1) {
            controllerState = ContextControllerStateUtil.getRecoveryStates(factory.getFactoryContext().getStateCache(), factoryContext.getOutermostContextName());
        }
        if (controllerState == null) {

            // handle preallocate
            if (factory.getHashedSpec().isPreallocate()) {
                for (int i = 0; i < factory.getHashedSpec().getGranularity(); i++) {
                    Map<String, Object> properties = ContextPropertyEventType.getHashBean(factoryContext.getContextName(), i);
                    currentSubpathId++;

                    // merge filter addendum, if any
                    ContextInternalFilterAddendum filterAddendumToUse = activationFilterAddendum;
                    if (factory.hasFiltersSpecsNestedContexts()) {
                        filterAddendumToUse = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
                        factory.populateContextInternalFilterAddendums(filterAddendumToUse, i);
                    }

                    final int hash = i;
                    ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, optionalTriggeringEvent, null, i, properties, controllerState, filterAddendumToUse, factory.getFactoryContext().isRecoveringResilient(), ContextPartitionState.STARTED, () -> new ContextPartitionIdentifierHash(hash));
                    partitionKeys.put(i, handle);

                    factory.getFactoryContext().getStateCache().addContextPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId, currentSubpathId, handle.getContextPartitionOrPathId(), i, factory.getBinding());
                }
                return;
            }

            // start filters if not preallocated
            activateFilters(optionalTriggeringEvent);

            return;
        }

        // initialize from existing state
        int pathIdToUse = importPathId != null ? importPathId : pathId;
        initializeFromState(optionalTriggeringEvent, optionalTriggeringPattern, controllerState, pathIdToUse, null, false);

        // activate filters
        if (!factory.getHashedSpec().isPreallocate()) {
            activateFilters(null);
        }
    }

    protected void activateFilters(EventBean optionalTriggeringEvent) {
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        for (ContextDetailHashItem item : factory.getHashedSpec().getItems()) {
            ContextControllerHashedFilterCallback callback = new ContextControllerHashedFilterCallback(factoryContext.getServicesContext(), factoryContext.getAgentInstanceContextCreate(), item, this, activationFilterAddendum);
            filterCallbacks.add(callback);

            if (optionalTriggeringEvent != null) {
                boolean match = StatementAgentInstanceUtil.evaluateFilterForStatement(factoryContext.getServicesContext(), optionalTriggeringEvent, factoryContext.getAgentInstanceContextCreate(), callback.getFilterHandle());

                if (match) {
                    callback.matchFound(optionalTriggeringEvent, null);
                }
            }
        }
    }

    public synchronized void create(int id, EventBean theEvent) {
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        if (partitionKeys.containsKey(id)) {
            return;
        }

        Map<String, Object> properties = ContextPropertyEventType.getHashBean(factoryContext.getContextName(), id);
        currentSubpathId++;

        // merge filter addendum, if any
        ContextInternalFilterAddendum filterAddendumToUse = activationFilterAddendum;
        if (factory.hasFiltersSpecsNestedContexts()) {
            filterAddendumToUse = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
            factory.populateContextInternalFilterAddendums(filterAddendumToUse, id);
        }

        ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, theEvent, null, id, properties, null, filterAddendumToUse, factory.getFactoryContext().isRecoveringResilient(), ContextPartitionState.STARTED, () -> new ContextPartitionIdentifierHash(id));
        partitionKeys.put(id, handle);

        factory.getFactoryContext().getStateCache().addContextPath(factoryContext.getOutermostContextName(), factoryContext.getNestingLevel(), pathId, currentSubpathId, handle.getContextPartitionOrPathId(), id, factory.getBinding());

        // update the filter version for this handle
        long filterVersion = factoryContext.getServicesContext().getFilterService().getFiltersVersion();
        factory.getFactoryContext().getAgentInstanceContextCreate().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filterVersion);
    }

    public ContextControllerFactory getFactory() {
        return factory;
    }

    public int getPathId() {
        return pathId;
    }

    public void deactivate() {
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        for (ContextControllerHashedFilterCallback callback : filterCallbacks) {
            callback.destroy(factoryContext.getServicesContext().getFilterService());
        }
        partitionKeys.clear();
        filterCallbacks.clear();
        factory.getFactoryContext().getStateCache().removeContextParentPath(factoryContext.getOutermostContextName(), factoryContext.getNestingLevel(), pathId);
    }

    private void initializeFromState(EventBean optionalTriggeringEvent,
                                     Map<String, Object> optionalTriggeringPattern,
                                     ContextControllerState controllerState,
                                     int pathIdToUse,
                                     AgentInstanceSelector agentInstanceSelector,
                                     boolean loadingExistingState) {

        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        TreeMap<ContextStatePathKey, ContextStatePathValue> states = controllerState.getStates();
        NavigableMap<ContextStatePathKey, ContextStatePathValue> childContexts = ContextControllerStateUtil.getChildContexts(factoryContext, pathIdToUse, states);

        int maxSubpathId = Integer.MIN_VALUE;

        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : childContexts.entrySet()) {

            Integer hashAlgoGeneratedId = (Integer) factory.getBinding().byteArrayToObject(entry.getValue().getBlob(), null);

            // merge filter addendum, if any
            ContextInternalFilterAddendum filterAddendumToUse = activationFilterAddendum;
            if (factory.hasFiltersSpecsNestedContexts()) {
                filterAddendumToUse = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
                factory.populateContextInternalFilterAddendums(filterAddendumToUse, hashAlgoGeneratedId);
            }

            // check if exists already
            if (controllerState.isImported()) {
                ContextControllerInstanceHandle existingHandle = partitionKeys.get(hashAlgoGeneratedId);
                if (existingHandle != null) {
                    activationCallback.contextPartitionNavigate(existingHandle, this, controllerState, entry.getValue().getOptionalContextPartitionId(), filterAddendumToUse, agentInstanceSelector, entry.getValue().getBlob(), loadingExistingState);
                    continue;
                }
            }

            Map<String, Object> properties = ContextPropertyEventType.getHashBean(factoryContext.getContextName(), hashAlgoGeneratedId);

            int assignedSubPathId = !controllerState.isImported() ? entry.getKey().getSubPath() : ++currentSubpathId;
            ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(entry.getValue().getOptionalContextPartitionId(), assignedSubPathId, entry.getKey().getSubPath(), this, optionalTriggeringEvent, optionalTriggeringPattern, hashAlgoGeneratedId, properties, controllerState, filterAddendumToUse, loadingExistingState || factoryContext.isRecoveringResilient(), entry.getValue().getState(), () -> new ContextPartitionIdentifierHash(hashAlgoGeneratedId));
            partitionKeys.put(hashAlgoGeneratedId, handle);

            if (entry.getKey().getSubPath() > maxSubpathId) {
                maxSubpathId = assignedSubPathId;
            }
        }
        if (!controllerState.isImported()) {
            currentSubpathId = maxSubpathId != Integer.MIN_VALUE ? maxSubpathId : 0;
        }
    }
}
