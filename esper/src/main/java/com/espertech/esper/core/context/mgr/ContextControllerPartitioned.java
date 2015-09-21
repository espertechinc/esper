/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.*;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.ContextControllerSelectorUtil;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.client.context.ContextPartitionState;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.event.EventAdapterService;

import java.util.*;

public class ContextControllerPartitioned implements ContextController, ContextControllerPartitionedInstanceCreateCallback {

    protected final int pathId;
    protected final ContextControllerLifecycleCallback activationCallback;
    protected final ContextControllerPartitionedFactoryImpl factory;

    protected final List<ContextControllerPartitionedFilterCallback> filterCallbacks = new ArrayList<ContextControllerPartitionedFilterCallback>();
    protected final HashMap<Object, ContextControllerInstanceHandle> partitionKeys = new HashMap<Object, ContextControllerInstanceHandle>();

    private ContextInternalFilterAddendum activationFilterAddendum;
    protected int currentSubpathId;

    public ContextControllerPartitioned(int pathId, ContextControllerLifecycleCallback activationCallback, ContextControllerPartitionedFactoryImpl factory) {
        this.pathId = pathId;
        this.activationCallback = activationCallback;
        this.factory = factory;
    }

    public void importContextPartitions(ContextControllerState state, int pathIdToUse, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector) {
        initializeFromState(null, null, filterAddendum, state, pathIdToUse, agentInstanceSelector);
    }

    public void deletePath(ContextPartitionIdentifier identifier) {
        ContextPartitionIdentifierPartitioned partitioned = (ContextPartitionIdentifierPartitioned) identifier;
        partitionKeys.remove(getKeyObjectForLookup(partitioned.getKeys()));
    }

    public void visitSelectedPartitions(ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor) {
        int nestingLevel = factory.getFactoryContext().getNestingLevel();
        if (contextPartitionSelector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filtered = (ContextPartitionSelectorFiltered) contextPartitionSelector;

            ContextPartitionIdentifierPartitioned identifier = new ContextPartitionIdentifierPartitioned();
            for (Map.Entry<Object, ContextControllerInstanceHandle> entry : partitionKeys.entrySet()) {
                identifier.setContextPartitionId(entry.getValue().getContextPartitionOrPathId());
                Object[] identifierOA = getKeyObjectsAccountForMultikey(entry.getKey());
                identifier.setKeys(identifierOA);

                if (filtered.filter(identifier)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), identifierOA, this, entry.getValue());
                }
            }
            return;
        }
        else if (contextPartitionSelector instanceof ContextPartitionSelectorSegmented) {
            ContextPartitionSelectorSegmented partitioned = (ContextPartitionSelectorSegmented) contextPartitionSelector;
            if (partitioned.getPartitionKeys() == null || partitioned.getPartitionKeys().isEmpty()) {
                return;
            }
            for (Object[] keyObjects : partitioned.getPartitionKeys()) {
                Object key = getKeyObjectForLookup(keyObjects);
                ContextControllerInstanceHandle instanceHandle = partitionKeys.get(key);
                if (instanceHandle != null && instanceHandle.getContextPartitionOrPathId() != null) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), keyObjects, this, instanceHandle);
                }
            }
            return;
        }
        else if (contextPartitionSelector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById filtered = (ContextPartitionSelectorById) contextPartitionSelector;

            for (Map.Entry<Object, ContextControllerInstanceHandle> entry : partitionKeys.entrySet()) {
                if (filtered.getContextPartitionIds().contains(entry.getValue().getContextPartitionOrPathId())) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), getKeyObjectsAccountForMultikey(entry.getKey()), this, entry.getValue());
                }
            }
            return;
        }
        else if (contextPartitionSelector instanceof ContextPartitionSelectorAll) {
            for (Map.Entry<Object, ContextControllerInstanceHandle> entry : partitionKeys.entrySet()) {
                visitor.visit(nestingLevel, pathId, factory.getBinding(), getKeyObjectsAccountForMultikey(entry.getKey()), this, entry.getValue());
            }
            return;
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorSegmented.class}, contextPartitionSelector);
    }

    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState controllerState, ContextInternalFilterAddendum filterAddendum, Integer importPathId) {
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        this.activationFilterAddendum = filterAddendum;

        for (ContextDetailPartitionItem item : factory.getSegmentedSpec().getItems()) {
            ContextControllerPartitionedFilterCallback callback = new ContextControllerPartitionedFilterCallback(factoryContext.getServicesContext(), factoryContext.getAgentInstanceContextCreate(), item, this, filterAddendum);
            filterCallbacks.add(callback);

            if (optionalTriggeringEvent != null) {
                boolean match = StatementAgentInstanceUtil.evaluateFilterForStatement(factoryContext.getServicesContext(), optionalTriggeringEvent, factoryContext.getAgentInstanceContextCreate(), callback.getFilterHandle());

                if (match) {
                    callback.matchFound(optionalTriggeringEvent, null);
                }
            }
        }

        if (factoryContext.getNestingLevel() == 1) {
            controllerState = ContextControllerStateUtil.getRecoveryStates(factory.getFactoryContext().getStateCache(), factoryContext.getOutermostContextName());
        }
        if (controllerState == null) {
            return;
        }

        int pathIdToUse = importPathId != null ? importPathId : pathId;
        initializeFromState(optionalTriggeringEvent, optionalTriggeringPattern, filterAddendum, controllerState, pathIdToUse, null);
    }

    public ContextControllerFactory getFactory() {
        return factory;
    }

    public int getPathId() {
        return pathId;
    }

    public synchronized void deactivate() {
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        for (ContextControllerPartitionedFilterCallback callback : filterCallbacks) {
            callback.destroy(factoryContext.getServicesContext().getFilterService());
        }
        partitionKeys.clear();
        filterCallbacks.clear();
        factory.getFactoryContext().getStateCache().removeContextParentPath(factoryContext.getOutermostContextName(), factoryContext.getNestingLevel(), pathId);
    }

    public synchronized void create(Object key, EventBean theEvent) {
        boolean exists = partitionKeys.containsKey(key);
        if (exists) {
            return;
        }

        currentSubpathId++;

        // determine properties available for querying
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        Map<String, Object> props = ContextPropertyEventType.getPartitionBean(factoryContext.getContextName(), 0, key, factory.getSegmentedSpec().getItems().get(0).getPropertyNames());

        // merge filter addendum, if any
        ContextInternalFilterAddendum filterAddendum = activationFilterAddendum;
        if (factory.hasFiltersSpecsNestedContexts()) {
            filterAddendum = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
            factory.populateContextInternalFilterAddendums(filterAddendum, key);
        }

        ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, theEvent, null, key, props, null, filterAddendum, false, ContextPartitionState.STARTED);

        partitionKeys.put(key, handle);

        Object[] keyObjectSaved = getKeyObjectsAccountForMultikey(key);
        factory.getFactoryContext().getStateCache().addContextPath(factoryContext.getOutermostContextName(), factoryContext.getNestingLevel(), pathId, currentSubpathId, handle.getContextPartitionOrPathId(), keyObjectSaved, factory.getBinding());
    }

    private Object[] getKeyObjectsAccountForMultikey(Object key) {
        if (key instanceof MultiKeyUntyped) {
            return ((MultiKeyUntyped)key).getKeys();
        }
        else {
            return new Object[] {key};
        }
    }

    private Object getKeyObjectForLookup(Object[] keyObjects) {
        if (keyObjects.length > 1) {
            return new MultiKeyUntyped(keyObjects);
        }
        else {
            return keyObjects[0];
        }
    }

    private void initializeFromState(EventBean optionalTriggeringEvent,
                                     Map<String, Object> optionalTriggeringPattern,
                                     ContextInternalFilterAddendum filterAddendum,
                                     ContextControllerState controllerState,
                                     int pathIdToUse,
                                     AgentInstanceSelector agentInstanceSelector) {

        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        TreeMap<ContextStatePathKey, ContextStatePathValue> states = controllerState.getStates();

        // restart if there are states
        int maxSubpathId = Integer.MIN_VALUE;
        NavigableMap<ContextStatePathKey, ContextStatePathValue> childContexts = ContextControllerStateUtil.getChildContexts(factoryContext, pathIdToUse, states);
        EventAdapterService eventAdapterService = factory.getFactoryContext().getServicesContext().getEventAdapterService();

        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : childContexts.entrySet()) {
            Object[] keys = (Object[]) factory.getBinding().byteArrayToObject(entry.getValue().getBlob(), eventAdapterService);
            Object mapKey = getKeyObjectForLookup(keys);

            // merge filter addendum, if any
            ContextInternalFilterAddendum myFilterAddendum = activationFilterAddendum;
            if (factory.hasFiltersSpecsNestedContexts()) {
                filterAddendum = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
                factory.populateContextInternalFilterAddendums(filterAddendum, mapKey);
            }

            // check if exists already
            if (controllerState.isImported()) {
                ContextControllerInstanceHandle existingHandle = partitionKeys.get(mapKey);
                if (existingHandle != null) {
                    activationCallback.contextPartitionNavigate(existingHandle, this, controllerState, entry.getValue().getOptionalContextPartitionId(), myFilterAddendum, agentInstanceSelector, entry.getValue().getBlob());
                    continue;
                }
            }

            Map<String, Object> props = ContextPropertyEventType.getPartitionBean(factoryContext.getContextName(), 0, mapKey, factory.getSegmentedSpec().getItems().get(0).getPropertyNames());

            int assignedSubpathId = !controllerState.isImported() ? entry.getKey().getSubPath() : ++currentSubpathId;
            ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(entry.getValue().getOptionalContextPartitionId(), assignedSubpathId, entry.getKey().getSubPath(), this, optionalTriggeringEvent, optionalTriggeringPattern, mapKey, props, controllerState, myFilterAddendum, factoryContext.isRecoveringResilient(), entry.getValue().getState());
            partitionKeys.put(mapKey, handle);

            if (entry.getKey().getSubPath() > maxSubpathId) {
                maxSubpathId = assignedSubpathId;
            }
        }
        if (!controllerState.isImported()) {
            currentSubpathId = maxSubpathId != Integer.MIN_VALUE ? maxSubpathId : 0;
        }
    }
}