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
package com.espertech.esper.core.context.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.ContextPartitionStateListener;
import com.espertech.esper.client.context.ContextStateEventContextPartitionDeallocated;
import com.espertech.esper.client.hook.ExceptionHandlerExceptionType;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryResult;
import com.espertech.esper.core.context.factory.StatementAgentInstancePreload;
import com.espertech.esper.core.context.mgr.*;
import com.espertech.esper.core.context.stmt.AIRegistryAggregation;
import com.espertech.esper.core.context.stmt.AIRegistryExpr;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.service.*;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.core.start.EPStatementStopMethodImpl;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.epl.script.AgentInstanceScriptContext;
import com.espertech.esper.epl.spec.OnTriggerDesc;
import com.espertech.esper.epl.spec.OnTriggerWindowDesc;
import com.espertech.esper.epl.view.OutputProcessViewTerminable;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.filter.FilterHandle;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatementAgentInstanceUtil {

    private static final Logger log = LoggerFactory.getLogger(EPStatementStopMethodImpl.class);

    public static void handleFilterFault(EventBean theEvent, long version, EPServicesContext servicesContext, Map<Integer, ContextControllerTreeAgentInstanceList> agentInstanceListMap) {
        for (Map.Entry<Integer, ContextControllerTreeAgentInstanceList> agentInstanceEntry : agentInstanceListMap.entrySet()) {
            if (agentInstanceEntry.getValue().getFilterVersionAfterAllocation() > version) {
                StatementAgentInstanceUtil.evaluateEventForStatement(servicesContext, theEvent, null, agentInstanceEntry.getValue().getAgentInstances());
            }
        }
    }

    public static void stopAgentInstances(List<AgentInstance> agentInstances, Map<String, Object> terminationProperties, EPServicesContext servicesContext, boolean isStatementStop, boolean leaveLocksAcquired, CopyOnWriteArrayList<ContextPartitionStateListener> listenersLazy, int cpid, String contextName) {
        if (agentInstances == null) {
            return;
        }
        for (AgentInstance instance : agentInstances) {
            stopAgentInstanceRemoveResources(instance, terminationProperties, servicesContext, isStatementStop, leaveLocksAcquired);
        }

        ContextStateEventUtil.dispatchPartition(listenersLazy, () -> new ContextStateEventContextPartitionDeallocated(servicesContext.getEngineURI(), contextName, cpid), ContextPartitionStateListener::onContextPartitionDeallocated);
    }

    public static void stopAgentInstanceRemoveResources(AgentInstance agentInstance, Map<String, Object> terminationProperties, EPServicesContext servicesContext, boolean isStatementStop, boolean leaveLocksAcquired) {
        if (terminationProperties != null) {
            agentInstance.getAgentInstanceContext().getContextProperties().getProperties().putAll(terminationProperties);
        }
        StatementAgentInstanceUtil.stop(agentInstance.getStopCallback(), agentInstance.getAgentInstanceContext(), agentInstance.getFinalView(), servicesContext, isStatementStop, leaveLocksAcquired, true);
    }

    public static void stopSafe(Collection<StopCallback> terminationCallbacks, StopCallback[] stopCallbacks, StatementContext statementContext) {
        StopCallback[] terminationArr = terminationCallbacks.toArray(new StopCallback[terminationCallbacks.size()]);
        stopSafe(terminationArr, statementContext);
        stopSafe(stopCallbacks, statementContext);
    }

    public static void stopSafe(StopCallback[] stopMethods, StatementContext statementContext) {
        for (StopCallback stopCallback : stopMethods) {
            stopSafe(stopCallback, statementContext);
        }
    }

    public static void stopSafe(StopCallback stopMethod, StatementContext statementContext) {
        try {
            stopMethod.stop();
        } catch (RuntimeException e) {
            statementContext.getExceptionHandlingService().handleException(e, statementContext.getStatementName(), statementContext.getExpression(), ExceptionHandlerExceptionType.STOP, null);
        }
    }

    public static void stop(StopCallback stopCallback, AgentInstanceContext agentInstanceContext, Viewable finalView, EPServicesContext servicesContext, boolean isStatementStop, boolean leaveLocksAcquired, boolean removedStatementResources) {

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qContextPartitionDestroy(agentInstanceContext);
        }
        // obtain statement lock
        StatementAgentInstanceLock lock = agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
        lock.acquireWriteLock();
        try {
            if (finalView instanceof OutputProcessViewTerminable && !isStatementStop) {
                OutputProcessViewTerminable terminable = (OutputProcessViewTerminable) finalView;
                terminable.terminated();
            }

            stopSafe(stopCallback, agentInstanceContext.getStatementContext());

            // release resource
            agentInstanceContext.getStatementContext().getStatementAgentInstanceRegistry().deassign(agentInstanceContext.getAgentInstanceId());

            // cause any remaining schedules, that may concide with the caller's schedule, to be ignored
            agentInstanceContext.getEpStatementAgentInstanceHandle().setDestroyed(true);

            // cause any filters, that may concide with the caller's filters, to be ignored
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(Long.MAX_VALUE);

            if (removedStatementResources &&
                    agentInstanceContext.getStatementContext().getStatementExtensionServicesContext() != null &&
                    agentInstanceContext.getStatementContext().getStatementExtensionServicesContext().getStmtResources() != null) {
                agentInstanceContext.getStatementContext().getStatementExtensionServicesContext().getStmtResources().deallocatePartitioned(agentInstanceContext.getAgentInstanceId());
            }
        } finally {
            if (!leaveLocksAcquired) {
                if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                    agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
                }
                lock.releaseWriteLock();
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aContextPartitionDestroy();
            }
        }
    }

    public static StatementAgentInstanceFactoryResult start(EPServicesContext servicesContext,
                                                            ContextControllerStatementBase statement,
                                                            boolean isSingleInstanceContext,
                                                            int agentInstanceId,
                                                            MappedEventBean agentInstanceProperties,
                                                            AgentInstanceFilterProxy agentInstanceFilterProxy,
                                                            boolean isRecoveringResilient) {
        StatementContext statementContext = statement.getStatementContext();

        // for on-trigger statements against named windows we must use the named window lock
        OnTriggerDesc optOnTriggerDesc = statement.getStatementSpec().getOnTriggerDesc();
        String namedWindowName = null;
        if ((optOnTriggerDesc != null) && (optOnTriggerDesc instanceof OnTriggerWindowDesc)) {
            String windowName = ((OnTriggerWindowDesc) optOnTriggerDesc).getWindowName();
            if (servicesContext.getTableService().getTableMetadata(windowName) == null) {
                namedWindowName = windowName;
            }
        }

        // determine lock to use
        StatementAgentInstanceLock agentInstanceLock;
        if (namedWindowName != null) {
            NamedWindowProcessor processor = servicesContext.getNamedWindowMgmtService().getProcessor(namedWindowName);
            NamedWindowProcessorInstance instance = processor.getProcessorInstance(agentInstanceId);
            agentInstanceLock = instance.getRootViewInstance().getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
        } else {
            if (isSingleInstanceContext) {
                agentInstanceLock = statementContext.getDefaultAgentInstanceLock();
            } else {
                agentInstanceLock = servicesContext.getStatementLockFactory().getStatementLock(statementContext.getStatementName(), statementContext.getAnnotations(), statementContext.isStatelessSelect());
            }
        }

        // share the filter version between agent instance handle (callbacks) and agent instance context
        StatementAgentInstanceFilterVersion filterVersion = new StatementAgentInstanceFilterVersion();

        // create handle that comtains lock for use in scheduling and filter callbacks
        EPStatementAgentInstanceHandle agentInstanceHandle = new EPStatementAgentInstanceHandle(statementContext.getEpStatementHandle(), agentInstanceLock, agentInstanceId, filterVersion, statementContext.getFilterFaultHandlerFactory());

        // create agent instance context
        AgentInstanceScriptContext agentInstanceScriptContext = null;
        if (statementContext.getDefaultAgentInstanceScriptContext() != null) {
            agentInstanceScriptContext = AgentInstanceScriptContext.from(statementContext.getEventAdapterService());
        }
        AgentInstanceContext agentInstanceContext = new AgentInstanceContext(statementContext, agentInstanceHandle, agentInstanceId, agentInstanceFilterProxy, agentInstanceProperties, agentInstanceScriptContext);
        StatementAgentInstanceLock statementAgentInstanceLock = agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qContextPartitionAllocate(agentInstanceContext);
        }
        statementAgentInstanceLock.acquireWriteLock();

        try {
            // start
            StatementAgentInstanceFactoryResult startResult = statement.getFactory().newContext(agentInstanceContext, isRecoveringResilient);

            // hook up with listeners+subscribers
            startResult.getFinalView().addView(statement.getMergeView()); // hook output to merge view

            // assign agents for expression-node based strategies
            AIRegistryExpr aiExprSvc = statementContext.getStatementAgentInstanceRegistry().getAgentInstanceExprService();
            AIRegistryAggregation aiAggregationSvc = statementContext.getStatementAgentInstanceRegistry().getAgentInstanceAggregationService();

            // allocate aggregation service
            if (startResult.getOptionalAggegationService() != null) {
                aiAggregationSvc.assignService(agentInstanceId, startResult.getOptionalAggegationService());
            }

            // allocate subquery
            for (Map.Entry<ExprSubselectNode, SubSelectStrategyHolder> item : startResult.getSubselectStrategies().entrySet()) {
                ExprSubselectNode node = item.getKey();
                SubSelectStrategyHolder strategyHolder = item.getValue();

                aiExprSvc.getSubselectService(node).assignService(agentInstanceId, strategyHolder.getStategy());
                aiExprSvc.getSubselectAggregationService(node).assignService(agentInstanceId, strategyHolder.getSubselectAggregationService());

                // allocate prior within subquery
                for (Map.Entry<ExprPriorNode, ExprPriorEvalStrategy> priorEntry : strategyHolder.getPriorStrategies().entrySet()) {
                    aiExprSvc.getPriorServices(priorEntry.getKey()).assignService(agentInstanceId, priorEntry.getValue());
                }

                // allocate previous within subquery
                for (Map.Entry<ExprPreviousNode, ExprPreviousEvalStrategy> prevEntry : strategyHolder.getPreviousNodeStrategies().entrySet()) {
                    aiExprSvc.getPreviousServices(prevEntry.getKey()).assignService(agentInstanceId, prevEntry.getValue());
                }
            }

            // allocate prior-expressions
            for (Map.Entry<ExprPriorNode, ExprPriorEvalStrategy> item : startResult.getPriorNodeStrategies().entrySet()) {
                aiExprSvc.getPriorServices(item.getKey()).assignService(agentInstanceId, item.getValue());
            }

            // allocate previous-expressions
            for (Map.Entry<ExprPreviousNode, ExprPreviousEvalStrategy> item : startResult.getPreviousNodeStrategies().entrySet()) {
                aiExprSvc.getPreviousServices(item.getKey()).assignService(agentInstanceId, item.getValue());
            }

            // allocate match-recognize previous expressions
            RegexExprPreviousEvalStrategy regexExprPreviousEvalStrategy = startResult.getRegexExprPreviousEvalStrategy();
            aiExprSvc.getMatchRecognizePrevious().assignService(agentInstanceId, regexExprPreviousEvalStrategy);

            // allocate table-access-expressions
            for (Map.Entry<ExprTableAccessNode, ExprTableAccessEvalStrategy> item : startResult.getTableAccessEvalStrategies().entrySet()) {
                aiExprSvc.getTableAccessServices(item.getKey()).assignService(agentInstanceId, item.getValue());
            }

            // execute preloads, if any
            for (StatementAgentInstancePreload preload : startResult.getPreloadList()) {
                preload.executePreload(agentInstanceContext);
            }

            if (statementContext.getStatementExtensionServicesContext() != null && statementContext.getStatementExtensionServicesContext().getStmtResources() != null) {
                StatementResourceHolder holder = statementContext.getStatementExtensionServicesContext().extractStatementResourceHolder(startResult);
                statementContext.getStatementExtensionServicesContext().getStmtResources().setPartitioned(agentInstanceId, holder);
            }

            // instantiate
            return startResult;
        } finally {
            if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            statementAgentInstanceLock.releaseWriteLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aContextPartitionAllocate();
            }
        }
    }

    public static void evaluateEventForStatement(EPServicesContext servicesContext, EventBean theEvent, Map<String, Object> optionalTriggeringPattern, List<AgentInstance> agentInstances) {
        if (theEvent != null) {
            evaluateEventForStatementInternal(servicesContext, theEvent, agentInstances);
        }
        if (optionalTriggeringPattern != null) {
            // evaluation order definition is up to the originator of the triggering pattern
            for (Map.Entry<String, Object> entry : optionalTriggeringPattern.entrySet()) {
                if (entry.getValue() instanceof EventBean) {
                    evaluateEventForStatementInternal(servicesContext, (EventBean) entry.getValue(), agentInstances);
                } else if (entry.getValue() instanceof EventBean[]) {
                    EventBean[] eventsArray = (EventBean[]) entry.getValue();
                    for (EventBean eventElement : eventsArray) {
                        evaluateEventForStatementInternal(servicesContext, eventElement, agentInstances);
                    }
                }
            }
        }
    }

    private static void evaluateEventForStatementInternal(EPServicesContext servicesContext, EventBean theEvent, List<AgentInstance> agentInstances) {
        // context was created - reevaluate for the given event
        ArrayDeque<FilterHandle> callbacks = new ArrayDeque<FilterHandle>(2);
        servicesContext.getFilterService().evaluate(theEvent, callbacks);   // evaluates for ALL statements
        if (callbacks.isEmpty()) {
            return;
        }

        // there is a single callback and a single context, if they match we are done
        if (agentInstances.size() == 1 && callbacks.size() == 1) {
            AgentInstance agentInstance = agentInstances.get(0);
            if (agentInstance.getAgentInstanceContext().getStatementId() == callbacks.getFirst().getStatementId()) {
                process(agentInstance, servicesContext, callbacks, theEvent);
            }
            return;
        }

        // use the right sorted/unsorted Map keyed by AgentInstance to sort
        boolean isPrioritized = servicesContext.getConfigSnapshot().getEngineDefaults().getExecution().isPrioritized();
        Map<AgentInstance, Object> stmtCallbacks;
        if (!isPrioritized) {
            stmtCallbacks = new HashMap<AgentInstance, Object>();
        } else {
            stmtCallbacks = new TreeMap<AgentInstance, Object>(AgentInstanceComparator.INSTANCE);
        }

        // process all callbacks
        for (FilterHandle filterHandle : callbacks) {
            // determine if this filter entry applies to any of the affected agent instances
            int statementId = filterHandle.getStatementId();
            AgentInstance agentInstanceFound = null;
            for (AgentInstance agentInstance : agentInstances) {
                if (agentInstance.getAgentInstanceContext().getStatementId() == statementId) {
                    agentInstanceFound = agentInstance;
                    break;
                }
            }
            if (agentInstanceFound == null) {   // when the callback is for some other stmt
                continue;
            }

            EPStatementHandleCallback handleCallback = (EPStatementHandleCallback) filterHandle;
            EPStatementAgentInstanceHandle handle = handleCallback.getAgentInstanceHandle();

            // Self-joins require that the internal dispatch happens after all streams are evaluated.
            // Priority or preemptive settings also require special ordering.
            if (handle.isCanSelfJoin() || isPrioritized) {
                Object stmtCallback = stmtCallbacks.get(agentInstanceFound);
                if (stmtCallback == null) {
                    stmtCallbacks.put(agentInstanceFound, handleCallback);
                } else if (stmtCallback instanceof ArrayDeque) {
                    ArrayDeque<EPStatementHandleCallback> q = (ArrayDeque<EPStatementHandleCallback>) stmtCallback;
                    if (!q.contains(handleCallback)) { // De-duplicate for Filter OR expression paths
                        q.add(handleCallback);
                    }
                } else {
                    ArrayDeque<EPStatementHandleCallback> q = new ArrayDeque<EPStatementHandleCallback>(4);
                    q.add((EPStatementHandleCallback) stmtCallback);
                    if (stmtCallback != handleCallback) { // De-duplicate for Filter OR expression paths
                        q.add(handleCallback);
                    }
                    stmtCallbacks.put(agentInstanceFound, q);
                }
                continue;
            }

            // no need to be sorted, process
            process(agentInstanceFound, servicesContext, Collections.<FilterHandle>singletonList(handleCallback), theEvent);
        }

        if (stmtCallbacks.isEmpty()) {
            return;
        }

        // Process self-join or sorted prioritized callbacks
        for (Map.Entry<AgentInstance, Object> entry : stmtCallbacks.entrySet()) {
            AgentInstance agentInstance = entry.getKey();
            Object callbackList = entry.getValue();
            if (callbackList instanceof ArrayDeque) {
                process(agentInstance, servicesContext, (Collection<FilterHandle>) callbackList, theEvent);
            } else {
                process(agentInstance, servicesContext, Collections.<FilterHandle>singletonList((FilterHandle) callbackList), theEvent);
            }
            if (agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().isPreemptive()) {
                return;
            }
        }
    }

    public static boolean evaluateFilterForStatement(EPServicesContext servicesContext, EventBean theEvent, AgentInstanceContext agentInstanceContext, FilterHandle filterHandle) {
        // context was created - reevaluate for the given event
        ArrayDeque<FilterHandle> callbacks = new ArrayDeque<FilterHandle>();
        servicesContext.getFilterService().evaluate(theEvent, callbacks, agentInstanceContext.getStatementContext().getStatementId());

        try {
            servicesContext.getVariableService().setLocalVersion();

            // sub-selects always go first
            for (FilterHandle handle : callbacks) {
                if (handle.equals(filterHandle)) {
                    return true;
                }
            }

            agentInstanceContext.getEpStatementAgentInstanceHandle().internalDispatch();

        } catch (RuntimeException ex) {
            servicesContext.getExceptionHandlingService().handleException(ex, agentInstanceContext.getEpStatementAgentInstanceHandle(), ExceptionHandlerExceptionType.PROCESS, theEvent);
        }

        return false;
    }

    public static StopCallback getStopCallback(List<StopCallback> stopCallbacks, final AgentInstanceContext agentInstanceContext) {
        final StopCallback[] stopCallbackArr = stopCallbacks.toArray(new StopCallback[stopCallbacks.size()]);
        return new StopCallback() {
            public void stop() {
                StatementAgentInstanceUtil.stopSafe(agentInstanceContext.getTerminationCallbackRO(), stopCallbackArr, agentInstanceContext.getStatementContext());
            }
        };
    }

    private static void process(AgentInstance agentInstance,
                                EPServicesContext servicesContext,
                                Collection<FilterHandle> callbacks,
                                EventBean theEvent) {
        AgentInstanceContext agentInstanceContext = agentInstance.getAgentInstanceContext();
        agentInstance.getAgentInstanceContext().getAgentInstanceLock().acquireWriteLock();
        try {
            servicesContext.getVariableService().setLocalVersion();

            // sub-selects always go first
            for (FilterHandle handle : callbacks) {
                EPStatementHandleCallback callback = (EPStatementHandleCallback) handle;
                if (callback.getAgentInstanceHandle() != agentInstanceContext.getEpStatementAgentInstanceHandle()) {
                    continue;
                }
                callback.getFilterCallback().matchFound(theEvent, null);
            }

            agentInstanceContext.getEpStatementAgentInstanceHandle().internalDispatch();
        } catch (RuntimeException ex) {
            servicesContext.getExceptionHandlingService().handleException(ex, agentInstanceContext.getEpStatementAgentInstanceHandle(), ExceptionHandlerExceptionType.PROCESS, theEvent);
        } finally {
            if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            agentInstanceContext.getAgentInstanceLock().releaseWriteLock();
        }
    }
}
