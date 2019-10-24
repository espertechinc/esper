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
import com.espertech.esper.common.internal.context.controller.condition.*;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.context.controller.initterm.ContextControllerInitTermUtil.determineCurrentlyRunning;

public class ContextControllerInitTermNonOverlap extends ContextControllerInitTermBase implements ContextControllerInitTermWLastTrigger {

    private EventBean lastTriggerEvent;

    public ContextControllerInitTermNonOverlap(ContextControllerInitTermFactory factory, ContextManagerRealization realization) {
        super(factory, realization);
    }

    public void activate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern) {
        initTermSvc.mgmtCreate(path, parentPartitionKeys);

        ContextControllerConditionNonHA startCondition = ContextControllerConditionFactory.getEndpoint(path, parentPartitionKeys, factory.initTermSpec.getStartCondition(), this, this, true);
        boolean currentlyRunning = determineCurrentlyRunning(startCondition, this);

        if (!currentlyRunning) {
            initTermSvc.mgmtUpdSetStartCondition(path, startCondition);
            boolean isTriggeringEventMatchesFilter = startCondition.activate(optionalTriggeringEvent, null, optionalTriggeringPattern);
            if (isTriggeringEventMatchesFilter) {
                rangeNotificationStart(path, optionalTriggeringEvent, null, null, null);
            }
        } else {
            instantiateAndActivateEndCondition(path, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringPattern, startCondition);
        }
    }

    public void rangeNotification(IntSeqKey conditionPath, ContextControllerConditionNonHA originCondition, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern, Map<String, Object> optionalPatternForInclusiveEval) {
        boolean endConditionNotification = originCondition.getDescriptor() != factory.getInitTermSpec().getStartCondition();
        if (endConditionNotification) {
            rangeNotificationEnd(conditionPath, originCondition, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringEventPattern);
        } else {
            this.lastTriggerEvent = optionalTriggeringEvent;
            rangeNotificationStart(conditionPath, optionalTriggeringEvent, optionalTriggeringPattern, optionalTriggeringEventPattern, optionalPatternForInclusiveEval);
        }
    }

    public EventBean getLastTriggerEvent() {
        return lastTriggerEvent;
    }

    private void rangeNotificationStart(IntSeqKey controllerPath, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern, Map<String, Object> optionalPatternForInclusiveEval) {
        ContextControllerConditionNonHA startCondition = initTermSvc.mgmtUpdClearStartCondition(controllerPath);
        if (startCondition.isRunning()) {
            startCondition.deactivate();
        }
        List<AgentInstance> agentInstances = instantiateAndActivateEndCondition(controllerPath, optionalTriggeringEvent, optionalTriggeringPattern, optionalPatternForInclusiveEval, startCondition);
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

        // start "@now" we maintain the locks
        boolean startNow = factory.getInitTermSpec().getStartCondition() instanceof ContextConditionDescriptorImmediate;
        List<AgentInstance> agentInstancesLocksHeld = null;
        if (startNow) {
            realization.getAgentInstanceContextCreate().getFilterService().acquireWriteLock();
            agentInstancesLocksHeld = new ArrayList<>(2);
        }

        realization.contextPartitionTerminate(conditionPath.removeFromEnd(), instance.getSubpathIdOrCPId(), this, optionalTriggeringPattern, startNow, agentInstancesLocksHeld);

        try {
            IntSeqKey controllerPath = conditionPath.removeFromEnd();
            Object[] partitionKeys = initTermSvc.mgmtGetParentPartitionKeys(controllerPath);

            ContextConditionDescriptor startDesc = factory.initTermSpec.getStartCondition();
            ContextControllerConditionNonHA startCondition = ContextControllerConditionFactory.getEndpoint(controllerPath, partitionKeys, startDesc, this, this, true);
            if (!startCondition.isImmediate()) {
                startCondition.activate(optionalTriggeringEvent, null, optionalTriggeringPattern);
                initTermSvc.mgmtUpdSetStartCondition(controllerPath, startCondition);
            } else {
                // we do not forward triggering events of termination
                instantiateAndActivateEndCondition(controllerPath, null, null, null, startCondition);
            }
        } finally {
            if (agentInstancesLocksHeld != null) {
                for (AgentInstance agentInstance : agentInstancesLocksHeld) {
                    agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(Long.MAX_VALUE);
                    if (agentInstance.getAgentInstanceContext().getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                        agentInstance.getAgentInstanceContext().getTableExprEvaluatorContext().releaseAcquiredLocks();
                    }
                    agentInstance.getAgentInstanceContext().getAgentInstanceLock().releaseWriteLock();
                }
            }

            if (startNow) {
                realization.getAgentInstanceContextCreate().getFilterService().releaseWriteLock();
            }
        }
    }

    private void installFilterFaultHandler(List<AgentInstance> agentInstances, IntSeqKey controllerPath) {
        if (agentInstances.isEmpty()) {
            return;
        }
        if (!(factory.getInitTermSpec().getStartCondition() instanceof ContextConditionDescriptorFilter)) {
            return;
        }
        FilterFaultHandler myFaultHandler = new NonOverlapWFIlterStartFilterFaultHandler(this);
        for (AgentInstance agentInstance : agentInstances) {
            agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().setFilterFaultHandler(myFaultHandler);
        }
    }

    public static class NonOverlapWFIlterStartFilterFaultHandler implements FilterFaultHandler {
        private final ContextControllerInitTermWLastTrigger contextControllerInitTerm;

        public NonOverlapWFIlterStartFilterFaultHandler(ContextControllerInitTermWLastTrigger contextControllerInitTerm) {
            this.contextControllerInitTerm = contextControllerInitTerm;
        }

        public boolean handleFilterFault(EventBean theEvent, long version) {

            /**
             * Handle filter faults such as
             *   - a) App thread determines event E1 applies to CP1
             *     b) Timer thread destroys CP1
             *     c) App thread processes E1 for CP1, filter-faulting and ending up reprocessing the event against CTX because of this handler
             */
            AgentInstanceContext aiCreate = contextControllerInitTerm.getRealization().getAgentInstanceContextCreate();
            StatementAgentInstanceLock lock = aiCreate.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
            lock.acquireWriteLock();
            try {
                EventBean trigger = contextControllerInitTerm.getLastTriggerEvent();
                if (theEvent != trigger) {
                    AgentInstanceUtil.evaluateEventForStatement(theEvent, null, Collections.singletonList(new AgentInstance(null, aiCreate, null)), aiCreate);
                }

                return true; // we handled the event
            } finally {
                lock.releaseWriteLock();
            }
        }
    }
}
