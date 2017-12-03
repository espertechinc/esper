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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.context.*;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.ContextControllerSelectorUtil;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.epl.spec.ContextDetailConditionFilter;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.filter.FilterFaultHandler;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;

import java.util.*;

public class ContextControllerPartitioned implements ContextController, ContextControllerPartitionedInstanceManageCallback {

    protected final int pathId;
    protected final ContextControllerLifecycleCallback activationCallback;
    protected final ContextControllerPartitionedFactoryImpl factory;

    protected final List<ContextControllerPartitionedFilterCallback> filterCallbacks = new ArrayList<>();
    protected final HashMap<Object, ContextControllerPartitionedEntry> partitionKeys = new HashMap<>();

    private ContextInternalFilterAddendum activationFilterAddendum;
    protected int currentSubpathId;
    private EventBean lastTerminatingEvent;

    public ContextControllerPartitioned(int pathId, ContextControllerLifecycleCallback activationCallback, ContextControllerPartitionedFactoryImpl factory) {
        this.pathId = pathId;
        this.activationCallback = activationCallback;
        this.factory = factory;
    }

    public void importContextPartitions(ContextControllerState state, int pathIdToUse, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector) {
        initializeFromState(null, null, filterAddendum, state, pathIdToUse, agentInstanceSelector, true);
    }

    public void deletePath(ContextPartitionIdentifier identifier) {
        ContextPartitionIdentifierPartitioned partitioned = (ContextPartitionIdentifierPartitioned) identifier;
        ContextControllerPartitionedEntry entry = partitionKeys.remove(getKeyObjectForLookup(partitioned.getKeys()));
        if (entry != null && entry.getOptionalTermination() != null) {
            entry.getOptionalTermination().deactivate();
        }
    }

    public void visitSelectedPartitions(ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor) {
        int nestingLevel = factory.getFactoryContext().getNestingLevel();
        if (contextPartitionSelector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filtered = (ContextPartitionSelectorFiltered) contextPartitionSelector;

            ContextPartitionIdentifierPartitioned identifier = new ContextPartitionIdentifierPartitioned();
            for (Map.Entry<Object, ContextControllerPartitionedEntry> entry : partitionKeys.entrySet()) {
                identifier.setContextPartitionId(entry.getValue().getInstanceHandle().getContextPartitionOrPathId());
                Object[] identifierOA = getKeyObjectsAccountForMultikey(entry.getKey());
                identifier.setKeys(identifierOA);

                if (filtered.filter(identifier)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), identifierOA, this, entry.getValue().getInstanceHandle());
                }
            }
            return;
        } else if (contextPartitionSelector instanceof ContextPartitionSelectorSegmented) {
            ContextPartitionSelectorSegmented partitioned = (ContextPartitionSelectorSegmented) contextPartitionSelector;
            if (partitioned.getPartitionKeys() == null || partitioned.getPartitionKeys().isEmpty()) {
                return;
            }
            for (Object[] keyObjects : partitioned.getPartitionKeys()) {
                Object key = getKeyObjectForLookup(keyObjects);
                ContextControllerPartitionedEntry entry = partitionKeys.get(key);
                ContextControllerInstanceHandle instanceHandle = entry == null ? null : entry.getInstanceHandle();
                if (instanceHandle != null && instanceHandle.getContextPartitionOrPathId() != null) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), keyObjects, this, instanceHandle);
                }
            }
            return;
        } else if (contextPartitionSelector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById filtered = (ContextPartitionSelectorById) contextPartitionSelector;

            for (Map.Entry<Object, ContextControllerPartitionedEntry> entry : partitionKeys.entrySet()) {
                if (filtered.getContextPartitionIds().contains(entry.getValue().getInstanceHandle().getContextPartitionOrPathId())) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), new ContextControllerPartitionedState(getKeyObjectsAccountForMultikey(entry.getKey()), Collections.emptyMap()), this, entry.getValue().getInstanceHandle());
                }
            }
            return;
        } else if (contextPartitionSelector instanceof ContextPartitionSelectorAll) {
            for (Map.Entry<Object, ContextControllerPartitionedEntry> entry : partitionKeys.entrySet()) {
                visitor.visit(nestingLevel, pathId, factory.getBinding(), getKeyObjectsAccountForMultikey(entry.getKey()), this, entry.getValue().getInstanceHandle());
            }
            return;
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorSegmented.class}, contextPartitionSelector);
    }

    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState controllerState, ContextInternalFilterAddendum filterAddendum, Integer importPathId) {
        this.activationFilterAddendum = filterAddendum;

        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        activateFilters(factoryContext, optionalTriggeringEvent, filterAddendum);

        if (factoryContext.getNestingLevel() == 1) {
            controllerState = ContextControllerStateUtil.getRecoveryStates(factory.getFactoryContext().getStateCache(), factoryContext.getOutermostContextName());
        }
        if (controllerState == null) {
            return;
        }

        int pathIdToUse = importPathId != null ? importPathId : pathId;
        initializeFromState(optionalTriggeringEvent, optionalTriggeringPattern, filterAddendum, controllerState, pathIdToUse, null, false);
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
        if (factory.getSegmentedSpec().getOptionalTermination() != null) {
            for (Map.Entry<Object, ContextControllerPartitionedEntry> entry : partitionKeys.entrySet()) {
                entry.getValue().getOptionalTermination().deactivate();
            }
        }
        partitionKeys.clear();
        filterCallbacks.clear();
        factory.getFactoryContext().getStateCache().removeContextParentPath(factoryContext.getOutermostContextName(), factoryContext.getNestingLevel(), pathId);
    }

    public synchronized void createKey(Object key, EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches, String initConditionAsName) {
        boolean exists = partitionKeys.containsKey(key);
        if (exists || theEvent == lastTerminatingEvent) {  // if all-matches is more than one, the termination has also fired
            return;
        }

        lastTerminatingEvent = null;
        currentSubpathId++;

        // determine properties available for querying
        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        Map<String, Object> initEvents = initConditionAsName != null ? Collections.singletonMap(initConditionAsName, theEvent) : Collections.emptyMap();
        Map<String, Object> props = ContextPropertyEventType.getPartitionBean(factoryContext.getContextName(), 0, key, factory.getSegmentedSpec().getItems().get(0).getPropertyNames(), initEvents);

        // merge filter addendum, if any
        ContextInternalFilterAddendum filterAddendum = activationFilterAddendum;
        if (factory.hasFiltersSpecsNestedContexts()) {
            filterAddendum = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
            factory.populateContextInternalFilterAddendums(filterAddendum, key);
        }

        // instantiate
        ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, theEvent, null, key, props, null, filterAddendum, false, ContextPartitionState.STARTED, () -> new ContextPartitionIdentifierPartitioned(getKeyObjectsAccountForMultikey(key)));

        // handle termination filter
        ContextControllerCondition terminationCondition = null;
        if (factory.getSegmentedSpec().getOptionalTermination() != null) {
            terminationCondition = activateTermination(key, props, theEvent, handle, initConditionAsName);
        }

        ContextControllerPartitionedEntry partition = new ContextControllerPartitionedEntry(handle, terminationCondition);
        partitionKeys.put(key, partition);

        // update the filter version for this handle
        long filterVersion = factoryContext.getServicesContext().getFilterService().getFiltersVersion();
        factory.getFactoryContext().getAgentInstanceContextCreate().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filterVersion);

        ContextControllerPartitionedState state = new ContextControllerPartitionedState(getKeyObjectsAccountForMultikey(key), initConditionAsName, theEvent);
        factory.getFactoryContext().getStateCache().addContextPath(factoryContext.getOutermostContextName(), factoryContext.getNestingLevel(), pathId, currentSubpathId, handle.getContextPartitionOrPathId(), state, factory.getBinding());
    }

    private ContextControllerCondition activateTermination(Object key, Map<String, Object> props, EventBean optionalTriggeringEvent, ContextControllerInstanceHandle handle, String initCondAsName) {
        ContextControllerConditionCallback callback = new ContextControllerConditionCallback() {
            public void rangeNotification(Map<String, Object> builtinProperties, ContextControllerCondition originEndpoint, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern, ContextInternalFilterAddendum filterAddendum) {
                ContextControllerPartitionedEntry entry = partitionKeys.remove(key);
                if (entry == null) {
                    return;
                }
                // remember the terminating event, we don't want it to initiate a new partition
                ContextControllerPartitioned.this.lastTerminatingEvent = optionalTriggeringEvent != null ? optionalTriggeringEvent : optionalTriggeringEventPattern;
                activationCallback.contextPartitionTerminate(entry.getInstanceHandle(), props, false, null);
                entry.getOptionalTermination().deactivate();
                factory.getFactoryContext().getStateCache().removeContextPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId, entry.getInstanceHandle().getSubPathId());
            }
        };

        ContextInternalFilterAddendum terminationAddendum = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
        factory.populateContextInternalFilterAddendumsTermination(terminationAddendum, key);

        ContextControllerCondition terminationCondition = ContextControllerConditionFactory.getEndpoint(factory.getFactoryContext().getContextName(), factory.getFactoryContext().getServicesContext(), factory.getFactoryContext().getAgentInstanceContextCreate(),
                factory.getSegmentedSpec().getOptionalTermination(), callback, terminationAddendum, false,
                factory.getFactoryContext().getNestingLevel(), pathId, currentSubpathId);

        MatchedEventMap priorMatches = null;
        if (initCondAsName != null) {
            Object[] propsMatchedEventMap = new Object[factory.getTermConditionMatchEventMap().getTagsPerIndex().length];
            int count = 0;
            for (String name : factory.getTermConditionMatchEventMap().getTagsPerIndex()) {
                propsMatchedEventMap[count++] = props.get(name);
            }
            priorMatches = new MatchedEventMapImpl(factory.getTermConditionMatchEventMap(), propsMatchedEventMap);
        }
        terminationCondition.activate(optionalTriggeringEvent, priorMatches, 0, false);

        for (AgentInstance agentInstance : handle.getInstances().getAgentInstances()) {
            agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().setFilterFaultHandler(ContextControllerWTerminationFilterFaultHandler.INSTANCE);
        }

        return terminationCondition;
    }

    private Object[] getKeyObjectsAccountForMultikey(Object key) {
        if (key instanceof MultiKeyUntyped) {
            return ((MultiKeyUntyped) key).getKeys();
        } else {
            return new Object[]{key};
        }
    }

    private Object getKeyObjectForLookup(Object[] keyObjects) {
        if (keyObjects.length > 1) {
            return new MultiKeyUntyped(keyObjects);
        } else {
            return keyObjects[0];
        }
    }

    private void initializeFromState(EventBean optionalTriggeringEvent,
                                     Map<String, Object> optionalTriggeringPattern,
                                     ContextInternalFilterAddendum filterAddendum,
                                     ContextControllerState controllerState,
                                     int pathIdToUse,
                                     AgentInstanceSelector agentInstanceSelector,
                                     boolean loadingExistingState) {

        ContextControllerFactoryContext factoryContext = factory.getFactoryContext();
        TreeMap<ContextStatePathKey, ContextStatePathValue> states = controllerState.getStates();

        // restart if there are states
        int maxSubpathId = Integer.MIN_VALUE;
        NavigableMap<ContextStatePathKey, ContextStatePathValue> childContexts = ContextControllerStateUtil.getChildContexts(factoryContext, pathIdToUse, states);
        EventAdapterService eventAdapterService = factory.getFactoryContext().getServicesContext().getEventAdapterService();

        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : childContexts.entrySet()) {
            ContextControllerPartitionedState state = (ContextControllerPartitionedState) factory.getBinding().byteArrayToObject(entry.getValue().getBlob(), eventAdapterService);
            Object mapKey = getKeyObjectForLookup(state.getPartitionKey());

            // merge filter addendum, if any
            ContextInternalFilterAddendum myFilterAddendum = activationFilterAddendum;
            if (factory.hasFiltersSpecsNestedContexts()) {
                filterAddendum = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
                factory.populateContextInternalFilterAddendums(filterAddendum, mapKey);
            }

            // check if exists already
            if (controllerState.isImported()) {
                ContextControllerPartitionedEntry existingHandle = partitionKeys.get(mapKey);
                if (existingHandle != null) {
                    activationCallback.contextPartitionNavigate(existingHandle.getInstanceHandle(), this, controllerState, entry.getValue().getOptionalContextPartitionId(), myFilterAddendum, agentInstanceSelector, entry.getValue().getBlob(), loadingExistingState);
                    continue;
                }
            }

            Map<String, Object> props = ContextPropertyEventType.getPartitionBean(factoryContext.getContextName(), 0, mapKey, factory.getSegmentedSpec().getItems().get(0).getPropertyNames(), state.getInitEvents());

            int assignedSubpathId = !controllerState.isImported() ? entry.getKey().getSubPath() : ++currentSubpathId;
            ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(entry.getValue().getOptionalContextPartitionId(), assignedSubpathId, entry.getKey().getSubPath(), this, optionalTriggeringEvent, optionalTriggeringPattern, mapKey, props, controllerState, myFilterAddendum, loadingExistingState || factoryContext.isRecoveringResilient(), entry.getValue().getState(), () -> new ContextPartitionIdentifierPartitioned(getKeyObjectsAccountForMultikey(state.getPartitionKey())));

            // handle termination filter
            ContextControllerCondition terminationCondition = null;
            if (factory.getSegmentedSpec().getOptionalTermination() != null) {
                Map.Entry<String, Object> initEvent = state.getInitEvents().isEmpty() ? null : state.getInitEvents().entrySet().iterator().next();
                terminationCondition = activateTermination(mapKey, props, initEvent == null ? null : (EventBean) initEvent.getValue(), handle, initEvent == null ? null : initEvent.getKey());
            }

            ContextControllerPartitionedEntry partition = new ContextControllerPartitionedEntry(handle, terminationCondition);
            partitionKeys.put(mapKey, partition);

            if (entry.getKey().getSubPath() > maxSubpathId) {
                maxSubpathId = assignedSubpathId;
            }
        }
        if (!controllerState.isImported()) {
            currentSubpathId = maxSubpathId != Integer.MIN_VALUE ? maxSubpathId : 0;
        }
    }

    private void activateFilters(ContextControllerFactoryContext factoryContext, EventBean optionalTriggeringEvent, ContextInternalFilterAddendum filterAddendum) {
        List<ContextDetailConditionFilter> optionalInit = factory.getSegmentedSpec().getOptionalInit();
        if (optionalInit == null || optionalInit.isEmpty()) {
            activateFiltersFromPartitionKeys(factoryContext, optionalTriggeringEvent, filterAddendum);
        } else {
            activateFiltersFromInit(factoryContext, optionalTriggeringEvent, filterAddendum);
        }
    }

    private void activateFiltersFromPartitionKeys(ContextControllerFactoryContext factoryContext, EventBean optionalTriggeringEvent, ContextInternalFilterAddendum filterAddendum) {
        for (ContextDetailPartitionItem item : factory.getSegmentedSpec().getItems()) {
            activateFilter(factoryContext, optionalTriggeringEvent, item.getGetters(), item.getFilterSpecCompiled(), filterAddendum, item.getAliasName());
        }
    }

    private void activateFiltersFromInit(ContextControllerFactoryContext factoryContext, EventBean optionalTriggeringEvent, ContextInternalFilterAddendum filterAddendum) {
        List<ContextDetailConditionFilter> inits = factory.getSegmentedSpec().getOptionalInit();
        for (ContextDetailConditionFilter init : inits) {

            ContextDetailPartitionItem found = null;
            for (ContextDetailPartitionItem item : factory.getSegmentedSpec().getItems()) {
                if (item.getFilterSpecCompiled().getFilterForEventType() == init.getFilterSpecCompiled().getFilterForEventType()) {
                    found = item;
                    break;
                }
            }
            if (found == null) {
                throw new IllegalArgumentException("Failed to find matching partition for type '" + init.getFilterSpecCompiled().getFilterForEventType());
            }

            activateFilter(factoryContext, optionalTriggeringEvent, found.getGetters(), init.getFilterSpecCompiled(), filterAddendum, init.getOptionalFilterAsName());
        }
    }

    private void activateFilter(ContextControllerFactoryContext factoryContext, EventBean optionalTriggeringEvent, EventPropertyGetter[] getters, FilterSpecCompiled filterSpecCompiled, ContextInternalFilterAddendum filterAddendum, String optionalInitConditionAsName) {
        ContextControllerPartitionedFilterCallback callback = new ContextControllerPartitionedFilterCallback(factoryContext.getServicesContext(), factoryContext.getAgentInstanceContextCreate(), getters, filterSpecCompiled, this, filterAddendum, optionalInitConditionAsName);
        filterCallbacks.add(callback);

        if (optionalTriggeringEvent != null) {
            boolean match = StatementAgentInstanceUtil.evaluateFilterForStatement(factoryContext.getServicesContext(), optionalTriggeringEvent, factoryContext.getAgentInstanceContextCreate(), callback.getFilterHandle());

            if (match) {
                callback.matchFound(optionalTriggeringEvent, null);
            }
        }
    }

    private static class ContextControllerWTerminationFilterFaultHandler implements FilterFaultHandler {
        public static final FilterFaultHandler INSTANCE = new ContextControllerWTerminationFilterFaultHandler();

        public boolean handleFilterFault(EventBean theEvent, long version) {
            return true;
        }
    }

}