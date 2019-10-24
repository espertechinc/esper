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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.collection.LRUCache;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionFactory;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContextControllerInitTermOverlap extends ContextControllerInitTermBase implements ContextControllerInitTermWDistinct {

    private final ContextControllerInitTermDistinctSvc distinctSvc;
    private final LRUCache<Object, EventBean> distinctLastTriggerEvents;
    private final EventBean[] eventsPerStreamDistinct;

    public ContextControllerInitTermOverlap(ContextControllerInitTermFactory factory, ContextManagerRealization realization) {
        super(factory, realization);
        if (factory.getInitTermSpec().getDistinctEval() != null) {
            if (factory.getFactoryEnv().getNumNestingLevels() == 1) {
                distinctSvc = new ContextControllerInitTermDistinctSvcNonNested();
            } else {
                distinctSvc = new ContextControllerInitTermDistinctSvcNested();
            }
            eventsPerStreamDistinct = new EventBean[1];
            distinctLastTriggerEvents = new LRUCache<>(16);
        } else {
            distinctSvc = null;
            distinctLastTriggerEvents = null;
            eventsPerStreamDistinct = null;
        }
    }

    public LRUCache<Object, EventBean> getDistinctLastTriggerEvents() {
        return distinctLastTriggerEvents;
    }

    public void activate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern) {
        initTermSvc.mgmtCreate(path, parentPartitionKeys);

        ContextControllerConditionNonHA startCondition = ContextControllerConditionFactory.getEndpoint(path, parentPartitionKeys, factory.initTermSpec.getStartCondition(), this, this, true);
        boolean isTriggeringEventMatchesFilter = startCondition.activate(optionalTriggeringEvent, null, optionalTriggeringPattern);
        initTermSvc.mgmtUpdSetStartCondition(path, startCondition);

        if (isTriggeringEventMatchesFilter || startCondition.isImmediate()) {
            instantiateAndActivateEndCondition(path, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringPattern, startCondition);
        }
    }

    @Override
    public void deactivate(IntSeqKey path, boolean terminateChildContexts) {
        super.deactivate(path, terminateChildContexts);
        if (distinctSvc != null) {
            distinctSvc.clear(path);
        }
    }

    public void destroy() {
        super.destroy();
        if (distinctSvc != null) {
            distinctSvc.destroy();
        }
    }

    public void rangeNotification(IntSeqKey conditionPath, ContextControllerConditionNonHA originCondition, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern, Map<String, Object> optionalPatternForInclusiveEval) {
        boolean endConditionNotification = originCondition.getDescriptor() != factory.getInitTermSpec().getStartCondition();
        if (endConditionNotification) {
            rangeNotificationEnd(conditionPath, originCondition, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringEventPattern);
        } else {
            rangeNotificationStart(conditionPath, originCondition, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringEventPattern);
        }
    }

    public ContextControllerInitTermDistinctSvc getDistinctSvc() {
        return distinctSvc;
    }

    private void rangeNotificationStart(IntSeqKey controllerPath, ContextControllerConditionNonHA startCondition, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern) {
        if (distinctSvc != null) {
            boolean added = addDistinctKey(controllerPath, optionalTriggeringEvent);
            if (!added) {
                return;
            }
        }

        // For overlapping mode, make sure we activate again or stay activated
        if (!startCondition.isRunning()) {
            startCondition.activate(optionalTriggeringEvent, null, optionalTriggeringPattern);
        }

        List<AgentInstance> agentInstances = instantiateAndActivateEndCondition(controllerPath, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringPattern, startCondition);
        installFilterFaultHandler(agentInstances, controllerPath);
    }

    private void rangeNotificationEnd(IntSeqKey conditionPath, ContextControllerConditionNonHA endCondition, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern) {
        if (endCondition.isRunning()) {
            endCondition.deactivate();
        }
        ContextControllerInitTermSvcEntry instance = initTermSvc.endDelete(conditionPath);
        if (instance == null) {
            return;
        }

        if (distinctSvc != null) {
            removeDistinctKey(conditionPath.removeFromEnd(), instance);
        }
        realization.contextPartitionTerminate(conditionPath.removeFromEnd(), instance.getSubpathIdOrCPId(), this, optionalTriggeringPattern, false, null);
    }

    private boolean addDistinctKey(IntSeqKey controllerPath, EventBean optionalTriggeringEvent) {
        if (optionalTriggeringEvent == null) {
            throw new IllegalStateException("No trgiggering event provided");
        }
        Object key = getDistinctKey(optionalTriggeringEvent);
        distinctLastTriggerEvents.put(key, optionalTriggeringEvent);
        return distinctSvc.addUnlessExists(controllerPath, key);
    }

    private void removeDistinctKey(IntSeqKey controllerPath, ContextControllerInitTermSvcEntry value) {
        EventBean event = value.getPartitionKey().getTriggeringEvent();
        Object key = getDistinctKey(event);
        distinctSvc.remove(controllerPath, key);
    }

    public Object getDistinctKey(EventBean eventBean) {
        eventsPerStreamDistinct[0] = eventBean;
        return factory.getInitTermSpec().getDistinctEval().evaluate(eventsPerStreamDistinct, true, realization.getAgentInstanceContextCreate());
    }

    private void installFilterFaultHandler(List<AgentInstance> agentInstances, IntSeqKey controllerPath) {
        if (agentInstances.isEmpty()) {
            return;
        }
        if (distinctSvc == null) {
            return;
        }
        FilterFaultHandler myFaultHandler = new DistinctFilterFaultHandler(this, controllerPath);
        for (AgentInstance agentInstance : agentInstances) {
            agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().setFilterFaultHandler(myFaultHandler);
        }
    }

    public static class DistinctFilterFaultHandler implements FilterFaultHandler {
        private final ContextControllerInitTermWDistinct contextControllerInitTerm;
        private final IntSeqKey controllerPath;

        public DistinctFilterFaultHandler(ContextControllerInitTermWDistinct contextControllerInitTerm, IntSeqKey controllerPath) {
            this.contextControllerInitTerm = contextControllerInitTerm;
            this.controllerPath = controllerPath;
        }

        public boolean handleFilterFault(EventBean theEvent, long version) {
            /**
             * Handle filter faults such as, for hashed non-preallocated-context, for example:
             *   - a) App thread determines event E1 applies to CTX + CP1
             *     b) Timer thread destroys CP1
             *     c) App thread processes E1 for CTX allocating CP2, processing E1 for CP2
             *     d) App thread processes E1 for CP1, filter-faulting and ending up dropping the event for CP1 because of this handler
             *
             *   - a) App thread determines event E1 applies to CTX + CP1
             *     b) App thread processes E1 for CTX, no action
             *     c) Timer thread destroys CP1
             *     d) App thread processes E1 for CP1, filter-faulting and ending up processing E1 into CTX because of this handler
             */
            AgentInstanceContext aiCreate = contextControllerInitTerm.getRealization().getAgentInstanceContextCreate();
            StatementAgentInstanceLock lock = aiCreate.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
            lock.acquireWriteLock();
            try {
                Object key = contextControllerInitTerm.getDistinctKey(theEvent);
                EventBean trigger = contextControllerInitTerm.getDistinctLastTriggerEvents().get(key);

                // see if we find that context partition
                if (trigger != null) {
                    // true for we have already handled this event
                    // false for filter fault
                    return trigger.equals(theEvent);
                }

                // not found: evaluate against context
                AgentInstanceUtil.evaluateEventForStatement(theEvent, null, Collections.singletonList(new AgentInstance(null, aiCreate, null)), aiCreate);

                return true; // we handled the event
            } finally {
                lock.releaseWriteLock();
            }
        }
    }
}
