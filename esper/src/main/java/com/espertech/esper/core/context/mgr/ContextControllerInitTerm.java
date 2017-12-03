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
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextControllerSelectorUtil;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.spec.ContextDetailCondition;
import com.espertech.esper.epl.spec.ContextDetailConditionCrontab;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.filter.FilterFaultHandler;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;
import com.espertech.esper.schedule.ScheduleComputeHelper;
import com.espertech.esper.schedule.ScheduleSpec;

import java.util.*;

public class ContextControllerInitTerm implements ContextController, ContextControllerConditionCallback {

    protected final int pathId;
    protected final ContextControllerLifecycleCallback activationCallback;
    protected final ContextControllerInitTermFactoryImpl factory;

    protected ContextControllerCondition startCondition;
    private Map<Object, EventBean> distinctContexts;
    private EventBean nonDistinctLastTrigger;
    private EventBean[] eventsPerStream = new EventBean[1];

    protected Map<ContextControllerCondition, ContextControllerInitTermInstance> endConditions = new LinkedHashMap<ContextControllerCondition, ContextControllerInitTermInstance>();

    protected int currentSubpathId;

    public ContextControllerInitTerm(int pathId, ContextControllerLifecycleCallback lifecycleCallback, ContextControllerInitTermFactoryImpl factory) {
        this.pathId = pathId;
        this.activationCallback = lifecycleCallback;
        this.factory = factory;
        if (factory.getContextDetail().getDistinctExpressions() != null && factory.getContextDetail().getDistinctExpressions().length > 0) {
            distinctContexts = new HashMap<Object, EventBean>();
        }
    }

    public void importContextPartitions(ContextControllerState state, int pathIdToUse, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector) {
        initializeFromState(null, null, filterAddendum, state, pathIdToUse, agentInstanceSelector, true);
    }

    public void deletePath(ContextPartitionIdentifier identifier) {
        ContextPartitionIdentifierInitiatedTerminated initterm = (ContextPartitionIdentifierInitiatedTerminated) identifier;
        for (Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> entry : endConditions.entrySet()) {
            if (ContextControllerInitTerm.compare(initterm.getStartTime(), initterm.getProperties(), initterm.getEndTime(),
                    entry.getValue().getStartTime(), entry.getValue().getStartProperties(), entry.getValue().getEndTime())) {
                entry.getKey().deactivate();
                endConditions.remove(entry.getKey());
                removeDistinctKey(entry.getValue());
                break;
            }
        }
    }

    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState controllerState, ContextInternalFilterAddendum filterAddendum, Integer importPathId) {

        if (factory.getFactoryContext().getNestingLevel() == 1) {
            controllerState = ContextControllerStateUtil.getRecoveryStates(factory.getFactoryContext().getStateCache(), factory.getFactoryContext().getOutermostContextName());
        }
        if (controllerState == null) {
            startCondition = makeEndpoint(factory.getContextDetail().getStart(), filterAddendum, true, 0);

            // if this is single-instance mode, check if we are currently running according to schedule
            boolean currentlyRunning = startCondition.isImmediate();
            if (!factory.getContextDetail().isOverlapping()) {
                currentlyRunning = determineCurrentlyRunning(startCondition);
            }

            if (currentlyRunning) {
                currentSubpathId++;
                ContextControllerCondition endEndpoint = makeEndpoint(factory.getContextDetail().getEnd(), filterAddendum, false, currentSubpathId);
                endEndpoint.activate(optionalTriggeringEvent, null, 0, factory.getFactoryContext().isRecoveringResilient());
                long startTime = factory.getSchedulingService().getTime();
                Long endTime = endEndpoint.getExpectedEndTime();
                Map<String, Object> builtinProps = getBuiltinProperties(factory.getFactoryContext().getContextName(), startTime, endTime, Collections.<String, Object>emptyMap());
                ContextControllerInstanceHandle instanceHandle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, optionalTriggeringEvent, optionalTriggeringPattern, null, builtinProps, controllerState, filterAddendum, factory.getFactoryContext().isRecoveringResilient(), ContextPartitionState.STARTED, () -> new ContextPartitionIdentifierInitiatedTerminated(builtinProps, startTime, endTime));
                endConditions.put(endEndpoint, new ContextControllerInitTermInstance(instanceHandle, null, startTime, endTime, currentSubpathId));

                ContextControllerInitTermState state = new ContextControllerInitTermState(factory.getFactoryContext().getServicesContext().getSchedulingService().getTime(), builtinProps);
                factory.getFactoryContext().getStateCache().addContextPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId, currentSubpathId, instanceHandle.getContextPartitionOrPathId(), state, factory.getBinding());
            }

            // non-overlapping and not currently running, or overlapping
            if ((!factory.getContextDetail().isOverlapping() && !currentlyRunning) ||
                    factory.getContextDetail().isOverlapping()) {
                startCondition.activate(optionalTriggeringEvent, null, 0, factory.getFactoryContext().isRecoveringResilient());
            }
            return;
        }

        startCondition = makeEndpoint(factory.getContextDetail().getStart(), filterAddendum, true, 0);

        // if this is single-instance mode, check if we are currently running according to schedule
        boolean currentlyRunning = false;
        if (!factory.getContextDetail().isOverlapping()) {
            currentlyRunning = determineCurrentlyRunning(startCondition);
        }
        if (!currentlyRunning) {
            startCondition.activate(optionalTriggeringEvent, null, 0, factory.getFactoryContext().isRecoveringResilient());
        }

        int pathIdToUse = importPathId != null ? importPathId : pathId;
        initializeFromState(optionalTriggeringEvent, optionalTriggeringPattern, filterAddendum, controllerState, pathIdToUse, null, false);
    }

    protected ContextControllerCondition makeEndpoint(ContextDetailCondition endpoint, ContextInternalFilterAddendum filterAddendum, boolean isStartEndpoint, int subPathId) {
        return ContextControllerConditionFactory.getEndpoint(factory.getFactoryContext().getContextName(), factory.getFactoryContext().getServicesContext(), factory.getFactoryContext().getAgentInstanceContextCreate(),
                endpoint, this, filterAddendum, isStartEndpoint,
                factory.getFactoryContext().getNestingLevel(), pathId, subPathId);
    }

    public void visitSelectedPartitions(ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor) {
        int nestingLevel = factory.getFactoryContext().getNestingLevel();
        if (contextPartitionSelector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filter = (ContextPartitionSelectorFiltered) contextPartitionSelector;
            for (Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> entry : endConditions.entrySet()) {
                ContextControllerInitTermInstance initTerm = entry.getValue();
                ContextPartitionIdentifierInitiatedTerminated identifier = new ContextPartitionIdentifierInitiatedTerminated(entry.getValue().getStartProperties(), initTerm.getStartTime(), initTerm.getEndTime());
                identifier.setContextPartitionId(initTerm.getInstanceHandle().getContextPartitionOrPathId());
                if (filter.filter(identifier)) {
                    ContextControllerInitTermState state = new ContextControllerInitTermState(factory.getFactoryContext().getServicesContext().getSchedulingService().getTime(), entry.getValue().getStartProperties());
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), state, this, entry.getValue().getInstanceHandle());
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById filter = (ContextPartitionSelectorById) contextPartitionSelector;
            for (Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> entry : endConditions.entrySet()) {
                if (filter.getContextPartitionIds().contains(entry.getValue().getInstanceHandle().getContextPartitionOrPathId())) {
                    ContextControllerInitTermState state = new ContextControllerInitTermState(factory.getFactoryContext().getServicesContext().getSchedulingService().getTime(), entry.getValue().getStartProperties());
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), state, this, entry.getValue().getInstanceHandle());
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorAll) {
            for (Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> entry : endConditions.entrySet()) {
                ContextControllerInitTermState state = new ContextControllerInitTermState(factory.getFactoryContext().getServicesContext().getSchedulingService().getTime(), entry.getValue().getStartProperties());
                visitor.visit(nestingLevel, pathId, factory.getBinding(), state, this, entry.getValue().getInstanceHandle());
            }
            return;
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[0], contextPartitionSelector);
    }

    public void rangeNotification(Map<String, Object> builtinProperties, ContextControllerCondition originCondition, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, EventBean optionalTriggeringEventPattern, ContextInternalFilterAddendum filterAddendum) {
        boolean endConditionNotification = originCondition != startCondition;
        boolean startNow = startCondition instanceof ContextControllerConditionImmediate;
        List<AgentInstance> agentInstancesLocksHeld = null;
        this.nonDistinctLastTrigger = optionalTriggeringEvent;

        if (startNow) {
            factory.getFactoryContext().getServicesContext().getFilterService().acquireWriteLock();
        }

        try {
            if (endConditionNotification) {

                if (originCondition.isRunning()) {
                    originCondition.deactivate();
                }

                // indicate terminate
                ContextControllerInitTermInstance instance = endConditions.remove(originCondition);
                if (instance == null) {
                    return;
                }

                // For start-now (non-overlapping only) we hold the lock of the existing agent instance
                // until the new one is ready.
                if (startNow) {
                    agentInstancesLocksHeld = new ArrayList<AgentInstance>();
                    optionalTriggeringEvent = null;  // since we are restarting, we don't want to evaluate the event twice
                    optionalTriggeringPattern = null;
                }
                activationCallback.contextPartitionTerminate(instance.getInstanceHandle(), builtinProperties, startNow, agentInstancesLocksHeld);

                // remove distinct key
                removeDistinctKey(instance);

                // re-activate start condition if not overlapping
                if (!factory.getContextDetail().isOverlapping()) {
                    startCondition.activate(optionalTriggeringEvent, null, 0, false);
                }

                factory.getFactoryContext().getStateCache().removeContextPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId, instance.getSubPathId());
            }

            // handle start-condition notification
            if (!endConditionNotification || startNow) {

                // Check if this is distinct-only and the key already exists
                if (distinctContexts != null) {
                    boolean added = addDistinctKey(optionalTriggeringEvent);
                    if (!added) {
                        return;
                    }
                }

                // For single-instance mode, deactivate
                if (!factory.getContextDetail().isOverlapping()) {
                    if (startCondition.isRunning()) {
                        startCondition.deactivate();
                    }
                } else {
                    // For overlapping mode, make sure we activate again or stay activated
                    if (!startCondition.isRunning()) {
                        startCondition.activate(null, null, 0, factory.getFactoryContext().isRecoveringResilient());
                    }
                }

                currentSubpathId++;
                ContextControllerCondition endEndpoint = makeEndpoint(factory.getContextDetail().getEnd(), filterAddendum, false, currentSubpathId);
                MatchedEventMap matchedEventMap = getMatchedEventMap(builtinProperties);
                endEndpoint.activate(null, matchedEventMap, 0, false);
                long startTime = factory.getSchedulingService().getTime();
                Long endTime = endEndpoint.getExpectedEndTime();
                Map<String, Object> builtinProps = getBuiltinProperties(factory.getFactoryContext().getContextName(), startTime, endTime, builtinProperties);
                ContextControllerInstanceHandle instanceHandle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, optionalTriggeringEvent, optionalTriggeringPattern, new ContextControllerInitTermState(factory.getSchedulingService().getTime(), matchedEventMap.getMatchingEventsAsMap()), builtinProps, null, filterAddendum, factory.getFactoryContext().isRecoveringResilient(), ContextPartitionState.STARTED, () -> new ContextPartitionIdentifierInitiatedTerminated(builtinProperties, startTime, endTime));
                endConditions.put(endEndpoint, new ContextControllerInitTermInstance(instanceHandle, builtinProperties, startTime, endTime, currentSubpathId));

                // install filter fault handlers, if necessary
                installFilterFaultHandler(instanceHandle);

                ContextControllerInitTermState state = new ContextControllerInitTermState(factory.getFactoryContext().getServicesContext().getSchedulingService().getTime(), builtinProperties);
                factory.getFactoryContext().getStateCache().addContextPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId, currentSubpathId, instanceHandle.getContextPartitionOrPathId(), state, factory.getBinding());
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
                factory.getFactoryContext().getServicesContext().getFilterService().releaseWriteLock();
            }
        }
    }

    private void installFilterFaultHandler(ContextControllerInstanceHandle instanceHandle) {
        FilterFaultHandler myFaultHandler = null;
        if (distinctContexts != null) {
            myFaultHandler = new DistinctFilterFaultHandler(this);
        } else {
            if (startCondition instanceof ContextControllerConditionFilter) {
                myFaultHandler = new NonDistinctFilterFaultHandler(this);
            }
        }

        if (myFaultHandler != null && instanceHandle.getInstances() != null) {
            for (AgentInstance agentInstance : instanceHandle.getInstances().getAgentInstances()) {
                agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().setFilterFaultHandler(myFaultHandler);
            }
        }
    }

    protected MatchedEventMap getMatchedEventMap(Map<String, Object> builtinProperties) {
        Object[] props = new Object[factory.getMatchedEventMapMeta().getTagsPerIndex().length];
        int count = 0;
        for (String name : factory.getMatchedEventMapMeta().getTagsPerIndex()) {
            props[count++] = builtinProperties.get(name);
        }
        return new MatchedEventMapImpl(factory.getMatchedEventMapMeta(), props);
    }

    protected boolean determineCurrentlyRunning(ContextControllerCondition startCondition) {

        // we are not currently running if either of the endpoints is not crontab-triggered
        if ((factory.getContextDetail().getStart() instanceof ContextDetailConditionCrontab) &&
                ((factory.getContextDetail().getEnd() instanceof ContextDetailConditionCrontab))) {
            ScheduleSpec scheduleStart = ((ContextDetailConditionCrontab) factory.getContextDetail().getStart()).getSchedule();
            ScheduleSpec scheduleEnd = ((ContextDetailConditionCrontab) factory.getContextDetail().getEnd()).getSchedule();
            EngineImportService engineImportService = factory.getStatementContext().getEngineImportService();
            long nextScheduledStartTime = ScheduleComputeHelper.computeNextOccurance(scheduleStart, factory.getTimeProvider().getTime(), engineImportService.getTimeZone(), engineImportService.getTimeAbacus());
            long nextScheduledEndTime = ScheduleComputeHelper.computeNextOccurance(scheduleEnd, factory.getTimeProvider().getTime(), engineImportService.getTimeZone(), engineImportService.getTimeAbacus());
            return nextScheduledStartTime >= nextScheduledEndTime;
        }

        if (startCondition instanceof ContextControllerConditionTimePeriod) {
            ContextControllerConditionTimePeriod condition = (ContextControllerConditionTimePeriod) startCondition;
            Long endTime = condition.getExpectedEndTime();
            if (endTime != null && endTime <= 0) {
                return true;
            }
        }

        return startCondition instanceof ContextControllerConditionImmediate;
    }

    public ContextControllerFactory getFactory() {
        return factory;
    }

    public int getPathId() {
        return pathId;
    }

    public void deactivate() {
        if (startCondition != null) {
            if (startCondition.isRunning()) {
                startCondition.deactivate();
            }
        }

        for (Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> entry : endConditions.entrySet()) {
            if (entry.getKey().isRunning()) {
                entry.getKey().deactivate();
            }
        }
        endConditions.clear();
        factory.getFactoryContext().getStateCache().removeContextParentPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId);
    }

    public static Map<String, Object> getBuiltinProperties(String contextName, long startTime, Long endTime, Map<String, Object> startEndpointData) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ContextPropertyEventType.PROP_CTX_NAME, contextName);
        props.put(ContextPropertyEventType.PROP_CTX_STARTTIME, startTime);
        props.put(ContextPropertyEventType.PROP_CTX_ENDTIME, endTime);
        props.putAll(startEndpointData);
        return props;
    }

    private void initializeFromState(EventBean optionalTriggeringEvent,
                                     Map<String, Object> optionalTriggeringPattern,
                                     ContextInternalFilterAddendum filterAddendum,
                                     ContextControllerState controllerState,
                                     int pathIdToUse,
                                     AgentInstanceSelector agentInstanceSelector,
                                     boolean loadingExistingState) {

        TreeMap<ContextStatePathKey, ContextStatePathValue> states = controllerState.getStates();
        NavigableMap<ContextStatePathKey, ContextStatePathValue> childContexts = ContextControllerStateUtil.getChildContexts(factory.getFactoryContext(), pathIdToUse, states);
        EventAdapterService eventAdapterService = factory.getFactoryContext().getServicesContext().getEventAdapterService();

        int maxSubpathId = Integer.MIN_VALUE;
        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : childContexts.entrySet()) {
            ContextControllerInitTermState state = (ContextControllerInitTermState) factory.getBinding().byteArrayToObject(entry.getValue().getBlob(), eventAdapterService);

            if (distinctContexts != null) {
                ContextControllerConditionFilter filter = (ContextControllerConditionFilter) startCondition;
                EventBean event = (EventBean) state.getPatternData().get(filter.getEndpointFilterSpec().getOptionalFilterAsName());
                addDistinctKey(event);
            }

            if (controllerState.isImported()) {
                Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> existing = null;
                for (Map.Entry<ContextControllerCondition, ContextControllerInitTermInstance> entryExisting : endConditions.entrySet()) {
                    if (compare(state.getStartTime(), state.getPatternData(), null,
                            entryExisting.getValue().getStartTime(), entryExisting.getValue().getStartProperties(), null)) {
                        existing = entryExisting;
                        break;
                    }
                }
                if (existing != null) {
                    ContextControllerInstanceHandle existingHandle = existing.getValue().getInstanceHandle();
                    if (existingHandle != null) {
                        activationCallback.contextPartitionNavigate(existingHandle, this, controllerState, entry.getValue().getOptionalContextPartitionId(), filterAddendum, agentInstanceSelector, entry.getValue().getBlob(), loadingExistingState);
                        continue;
                    }
                }
            }

            ContextControllerCondition endEndpoint = makeEndpoint(factory.getContextDetail().getEnd(), filterAddendum, false, entry.getKey().getSubPath());
            long timeOffset = factory.getFactoryContext().getServicesContext().getSchedulingService().getTime() - state.getStartTime();

            endEndpoint.activate(optionalTriggeringEvent, null, timeOffset, factory.getFactoryContext().isRecoveringResilient());
            long startTime = state.getStartTime();
            Long endTime = endEndpoint.getExpectedEndTime();
            Map<String, Object> builtinProps = getBuiltinProperties(factory.getFactoryContext().getContextName(), startTime, endTime, state.getPatternData());
            int contextPartitionId = entry.getValue().getOptionalContextPartitionId();

            int assignedSubPathId = !controllerState.isImported() ? entry.getKey().getSubPath() : ++currentSubpathId;
            ContextControllerInstanceHandle instanceHandle = activationCallback.contextPartitionInstantiate(contextPartitionId, assignedSubPathId, entry.getKey().getSubPath(), this, optionalTriggeringEvent, optionalTriggeringPattern, new ContextControllerInitTermState(startTime, state.getPatternData()), builtinProps, controllerState, filterAddendum, loadingExistingState || factory.getFactoryContext().isRecoveringResilient(), entry.getValue().getState(), () -> new ContextPartitionIdentifierInitiatedTerminated(builtinProps, startTime, endTime));
            endConditions.put(endEndpoint, new ContextControllerInitTermInstance(instanceHandle, state.getPatternData(), startTime, endTime, assignedSubPathId));

            if (entry.getKey().getSubPath() > maxSubpathId) {
                maxSubpathId = assignedSubPathId;
            }
        }

        if (!controllerState.isImported()) {
            currentSubpathId = maxSubpathId != Integer.MIN_VALUE ? maxSubpathId : 0;
        }
    }

    public static boolean compare(long savedStartTime,
                                  Map<String, Object> savedProperties,
                                  Long savedEndTime,
                                  long existingStartTime,
                                  Map<String, Object> existingProperties,
                                  Long existingEndTime) {

        if (savedStartTime != existingStartTime) {
            return false;
        }
        if (savedEndTime != null && existingEndTime != null && !savedEndTime.equals(existingEndTime)) {
            return false;
        }

        for (Map.Entry<String, Object> savedEntry : savedProperties.entrySet()) {
            Object existingValue = existingProperties.get(savedEntry.getKey());
            Object savedValue = savedEntry.getValue();
            if (savedValue == null && existingValue == null) {
                continue;
            }
            if (savedValue == null || existingValue == null) {
                return false;
            }
            if (existingValue.equals(savedValue)) {
                continue;
            }
            if (existingValue instanceof EventBean && savedValue instanceof EventBean) {
                if (((EventBean) existingValue).getUnderlying().equals(((EventBean) savedValue).getUnderlying())) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    private boolean addDistinctKey(EventBean optionalTriggeringEvent) {
        Object key = getDistinctKey(optionalTriggeringEvent);
        if (distinctContexts.containsKey(key)) {
            return false;
        }
        distinctContexts.put(key, optionalTriggeringEvent);
        return true;
    }

    private void removeDistinctKey(ContextControllerInitTermInstance value) {
        if (distinctContexts == null) {
            return;
        }
        ContextControllerConditionFilter filter = (ContextControllerConditionFilter) startCondition;
        EventBean event = (EventBean) value.getStartProperties().get(filter.getEndpointFilterSpec().getOptionalFilterAsName());
        Object key = getDistinctKey(event);
        distinctContexts.remove(key);
    }

    private Object getDistinctKey(EventBean optionalTriggeringEvent) {
        eventsPerStream[0] = optionalTriggeringEvent;
        ExprEvaluator[] distinctEvaluators = factory.getDistinctEvaluators();
        if (distinctEvaluators.length == 1) {
            return distinctEvaluators[0].evaluate(eventsPerStream, true, factory.getFactoryContext().getAgentInstanceContextCreate());
        }

        Object[] results = new Object[factory.getDistinctEvaluators().length];
        int count = 0;
        for (ExprEvaluator expr : distinctEvaluators) {
            results[count] = expr.evaluate(eventsPerStream, true, factory.getFactoryContext().getAgentInstanceContextCreate());
            count++;
        }
        return new MultiKeyUntyped(results);
    }

    private static class DistinctFilterFaultHandler implements FilterFaultHandler {
        private final ContextControllerInitTerm contextControllerInitTerm;

        private DistinctFilterFaultHandler(ContextControllerInitTerm contextControllerInitTerm) {
            this.contextControllerInitTerm = contextControllerInitTerm;
        }

        public boolean handleFilterFault(EventBean theEvent, long version) {

            /**
             * Handle filter faults such as
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
            AgentInstanceContext aiCreate = contextControllerInitTerm.getFactory().getFactoryContext().getAgentInstanceContextCreate();
            StatementAgentInstanceLock lock = aiCreate.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
            lock.acquireWriteLock();
            try {
                Object key = contextControllerInitTerm.getDistinctKey(theEvent);
                EventBean trigger = contextControllerInitTerm.distinctContexts.get(key);

                // see if we find that context partition
                if (trigger != null) {
                    // true for we have already handled this event
                    // false for filter fault
                    return trigger.equals(theEvent);
                }

                // not found: evaluate against context
                StatementAgentInstanceUtil.evaluateEventForStatement(contextControllerInitTerm.getFactory().getFactoryContext().getServicesContext(),
                        theEvent, null, Collections.singletonList(new AgentInstance(null, aiCreate, null)));

                return true; // we handled the event
            } finally {
                lock.releaseWriteLock();
            }
        }
    }

    private static class NonDistinctFilterFaultHandler implements FilterFaultHandler {
        private final ContextControllerInitTerm contextControllerInitTerm;

        private NonDistinctFilterFaultHandler(ContextControllerInitTerm contextControllerInitTerm) {
            this.contextControllerInitTerm = contextControllerInitTerm;
        }

        public boolean handleFilterFault(EventBean theEvent, long version) {

            /**
             * Handle filter faults such as
             *   - a) App thread determines event E1 applies to CP1
             *     b) Timer thread destroys CP1
             *     c) App thread processes E1 for CP1, filter-faulting and ending up reprocessing the event against CTX because of this handler
             */
            AgentInstanceContext aiCreate = contextControllerInitTerm.getFactory().getFactoryContext().getAgentInstanceContextCreate();
            StatementAgentInstanceLock lock = aiCreate.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
            lock.acquireWriteLock();
            try {
                EventBean trigger = contextControllerInitTerm.nonDistinctLastTrigger;
                if (theEvent != trigger) {
                    StatementAgentInstanceUtil.evaluateEventForStatement(contextControllerInitTerm.getFactory().getFactoryContext().getServicesContext(),
                            theEvent, null, Collections.singletonList(new AgentInstance(null, aiCreate, null)));
                }

                return true; // we handled the event
            } finally {
                lock.releaseWriteLock();
            }
        }
    }
}
