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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.*;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionInstantiationResult;
import com.espertech.esper.common.internal.context.util.AgentInstance;
import com.espertech.esper.common.internal.context.util.AgentInstanceUtil;
import com.espertech.esper.common.internal.context.util.FilterFaultHandler;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ContextControllerKeyedImpl extends ContextControllerKeyed {

    protected final ContextControllerKeyedSvc keyedSvc;

    public ContextControllerKeyedImpl(ContextControllerKeyedFactory factory, ContextManagerRealization realization) {
        super(realization, factory);
        keyedSvc = ContextControllerKeyedUtil.getService(factory, realization);
    }

    public void activate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern) {
        keyedSvc.mgmtCreate(path, parentPartitionKeys);
        ContextControllerFilterEntry[] filterEntries = activateFilters(optionalTriggeringEvent, path, parentPartitionKeys);
        keyedSvc.mgmtSetFilters(path, filterEntries);
    }

    public void deactivate(IntSeqKey path, boolean terminateChildContexts) {
        if (path.length() != factory.getFactoryEnv().getNestingLevel() - 1) {
            throw new IllegalStateException("Unrecognized controller path");
        }
        ContextControllerFilterEntry[] filters = keyedSvc.mgmtGetFilters(path);
        for (ContextControllerFilterEntry callback : filters) {
            ((ContextControllerKeyedFilterEntry) callback).destroy();
        }

        if (factory.getKeyedSpec().getOptionalTermination() != null) {
            List<ContextControllerConditionNonHA> terminationConditions = keyedSvc.keyGetTermConditions(path);
            for (ContextControllerConditionNonHA condition : terminationConditions) {
                condition.deactivate();
            }
        }

        Collection<Integer> subpaths = keyedSvc.deactivate(path);
        if (terminateChildContexts) {
            for (int subpathId : subpaths) {
                realization.contextPartitionTerminate(path, subpathId, this, null, false, null);
            }
        }
    }

    public void matchFound(ContextControllerDetailKeyedItem item, EventBean theEvent, IntSeqKey controllerPath, String optionalInitCondAsName) {
        if (controllerPath.length() != factory.getFactoryEnv().getNestingLevel() - 1) {
            throw new IllegalStateException("Unrecognized controller path");
        }

        Object getterKey = item.getGetter().get(theEvent);
        boolean exists = keyedSvc.keyHasSeen(controllerPath, getterKey);
        if (exists || theEvent == lastTerminatingEvent) {  // if all-matches is more than one, the termination has also fired
            return;
        }
        lastTerminatingEvent = null;

        Object partitionKey = getterKey;
        if (factory.keyedSpec.isHasAsName()) {
            partitionKey = new ContextControllerKeyedPartitionKeyWInit(getterKey, optionalInitCondAsName, optionalInitCondAsName == null ? null : theEvent);
        }

        Object[] parentPartitionKeys = keyedSvc.mgmtGetPartitionKeys(controllerPath);

        // get next subpath id
        int subpathId = keyedSvc.mgmtGetIncSubpath(controllerPath);

        // instantiate
        ContextPartitionInstantiationResult result = realization.contextPartitionInstantiate(controllerPath, subpathId, this, theEvent, null, parentPartitionKeys, partitionKey);
        int subpathIdOrCPId = result.getSubpathOrCPId();

        // handle termination filter
        ContextControllerConditionNonHA terminationCondition = null;
        if (factory.getKeyedSpec().getOptionalTermination() != null) {
            IntSeqKey conditionPath = controllerPath.addToEnd(subpathIdOrCPId);
            terminationCondition = activateTermination(theEvent, parentPartitionKeys, partitionKey, conditionPath, optionalInitCondAsName);

            for (AgentInstance agentInstance : result.getAgentInstances()) {
                agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().setFilterFaultHandler(ContextControllerWTerminationFilterFaultHandler.INSTANCE);
            }
        }

        keyedSvc.keyAdd(controllerPath, getterKey, subpathIdOrCPId, terminationCondition);

        // update the filter version for this handle
        long filterVersionAfterStart = realization.getAgentInstanceContextCreate().getFilterService().getFiltersVersion();
        realization.getAgentInstanceContextCreate().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filterVersionAfterStart);
    }

    protected void visitPartitions(IntSeqKey controllerPath, BiConsumer<Object, Integer> keyAndSubpathOrCPId) {
        keyedSvc.keyVisit(controllerPath, keyAndSubpathOrCPId);
    }

    protected int getSubpathOrCPId(IntSeqKey path, Object keyForLookup) {
        return keyedSvc.keyGetSubpathOrCPId(path, keyForLookup);
    }

    public void destroy() {
        keyedSvc.destroy();
    }

    private ContextControllerConditionNonHA activateTermination(EventBean triggeringEvent, Object[] parentPartitionKeys, Object partitionKey, IntSeqKey conditionPath, String optionalInitCondAsName) {
        ContextControllerConditionCallback callback = new ContextControllerConditionCallback() {
            public void rangeNotification(IntSeqKey conditionPath, ContextControllerConditionNonHA originEndpoint, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern, Map<String, Object> optionalPatternForInclusiveEval) {
                IntSeqKey parentPath = conditionPath.removeFromEnd();
                Object getterKey = factory.getGetterKey(partitionKey);
                ContextControllerKeyedSvcEntry removed = keyedSvc.keyRemove(parentPath, getterKey);
                if (removed == null) {
                    return;
                }
                // remember the terminating event, we don't want it to initiate a new partition
                ContextControllerKeyedImpl.this.lastTerminatingEvent = optionalTriggeringEvent != null ? optionalTriggeringEvent : optionalTriggeringEventPattern;
                realization.contextPartitionTerminate(conditionPath.removeFromEnd(), removed.getSubpathOrCPId(), ContextControllerKeyedImpl.this, null, false, null);
                removed.getTerminationCondition().deactivate();
            }
        };

        Object[] partitionKeys = CollectionUtil.addValue(parentPartitionKeys, partitionKey);
        ContextControllerConditionNonHA terminationCondition = ContextControllerConditionFactory.getEndpoint(conditionPath, partitionKeys, factory.keyedSpec.getOptionalTermination(), callback, this, false);

        ContextControllerEndConditionMatchEventProvider endConditionMatchEventProvider = null;
        if (optionalInitCondAsName != null) {
            endConditionMatchEventProvider = new ContextControllerEndConditionMatchEventProvider() {
                public void populateEndConditionFromTrigger(MatchedEventMap map, EventBean triggeringEvent) {
                    ContextControllerKeyedUtil.populatePriorMatch(optionalInitCondAsName, map, triggeringEvent);
                }

                public void populateEndConditionFromTrigger(MatchedEventMap map, Map<String, Object> triggeringPattern) {
                    // not required for keyed controller
                }
            };
        }

        terminationCondition.activate(triggeringEvent, endConditionMatchEventProvider, null);

        return terminationCondition;
    }

    private ContextControllerFilterEntry[] activateFilters(EventBean optionalTriggeringEvent, IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        ContextConditionDescriptor[] optionalInit = factory.getKeyedSpec().getOptionalInit();
        if (optionalInit == null || optionalInit.length == 0) {
            return activateFiltersFromPartitionKeys(optionalTriggeringEvent, controllerPath, parentPartitionKeys);
        } else {
            return activateFiltersFromInit(optionalTriggeringEvent, controllerPath, parentPartitionKeys);
        }
    }

    private ContextControllerFilterEntry[] activateFiltersFromInit(EventBean optionalTriggeringEvent, IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        ContextConditionDescriptorFilter[] inits = factory.getKeyedSpec().getOptionalInit();
        ContextControllerFilterEntry[] filterEntries = new ContextControllerFilterEntry[inits.length];
        for (int i = 0; i < inits.length; i++) {
            ContextConditionDescriptorFilter init = inits[i];
            ContextControllerDetailKeyedItem found = ContextControllerKeyedUtil.findInitMatchingKey(factory.getKeyedSpec().getItems(), init);
            filterEntries[i] = activateFilterWithInit(init, found, optionalTriggeringEvent, controllerPath, parentPartitionKeys);
        }
        return filterEntries;
    }

    private ContextControllerFilterEntry[] activateFiltersFromPartitionKeys(EventBean optionalTriggeringEvent, IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        ContextControllerDetailKeyedItem[] items = factory.getKeyedSpec().getItems();
        ContextControllerFilterEntry[] filterEntries = new ContextControllerFilterEntry[items.length];
        for (int i = 0; i < items.length; i++) {
            filterEntries[i] = activateFilterNoInit(items[i], optionalTriggeringEvent, controllerPath, parentPartitionKeys);
        }
        return filterEntries;
    }

    private ContextControllerFilterEntry activateFilterNoInit(ContextControllerDetailKeyedItem item, EventBean optionalTriggeringEvent, IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        ContextControllerKeyedFilterEntryNoInit callback = new ContextControllerKeyedFilterEntryNoInit(this, controllerPath, parentPartitionKeys, item);
        if (optionalTriggeringEvent != null) {
            boolean match = AgentInstanceUtil.evaluateFilterForStatement(optionalTriggeringEvent, realization.getAgentInstanceContextCreate(), callback.getFilterHandle());

            if (match) {
                callback.matchFound(optionalTriggeringEvent, null);
            }
        }
        return callback;
    }

    private ContextControllerFilterEntry activateFilterWithInit(ContextConditionDescriptorFilter filter, ContextControllerDetailKeyedItem item, EventBean optionalTriggeringEvent, IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        return new ContextControllerKeyedFilterEntryWInit(this, controllerPath, item, parentPartitionKeys, filter);
    }

    public static class ContextControllerWTerminationFilterFaultHandler implements FilterFaultHandler {
        public static final FilterFaultHandler INSTANCE = new ContextControllerWTerminationFilterFaultHandler();

        private ContextControllerWTerminationFilterFaultHandler() {
        }

        public boolean handleFilterFault(EventBean theEvent, long version) {
            return true;
        }
    }
}
