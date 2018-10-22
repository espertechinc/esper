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
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerExceptionType;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

import java.util.*;

public class ContextStatementEventEvaluatorDefault implements ContextStatementEventEvaluator {
    public final static ContextStatementEventEvaluatorDefault INSTANCE = new ContextStatementEventEvaluatorDefault();

    private ContextStatementEventEvaluatorDefault() {
    }

    public void evaluateEventForStatement(EventBean theEvent, List<AgentInstance> agentInstances, AgentInstanceContext agentInstanceContextCreate) {
        // context was created - reevaluate for the given event
        ArrayDeque<FilterHandle> callbacks = new ArrayDeque<FilterHandle>(2);
        agentInstanceContextCreate.getFilterService().evaluate(theEvent, callbacks);   // evaluates for ALL statements
        if (callbacks.isEmpty()) {
            return;
        }

        // there is a single callback and a single context, if they match we are done
        if (agentInstances.size() == 1 && callbacks.size() == 1) {
            AgentInstance agentInstance = agentInstances.get(0);
            AgentInstanceContext agentInstanceContext = agentInstance.getAgentInstanceContext();
            FilterHandle callback = callbacks.getFirst();
            if (agentInstanceContext.getStatementId() == callback.getStatementId() && agentInstanceContext.getAgentInstanceId() == callback.getAgentInstanceId()) {
                process(agentInstance, callbacks, theEvent);
            }
            return;
        }

        // use the right sorted/unsorted Map keyed by AgentInstance to sort
        boolean isPrioritized = agentInstanceContextCreate.getRuntimeSettingsService().getConfigurationRuntime().getExecution().isPrioritized();
        Map<AgentInstance, Object> stmtCallbacks;
        if (!isPrioritized) {
            stmtCallbacks = new HashMap<>();
        } else {
            stmtCallbacks = new TreeMap<>(AgentInstanceComparator.INSTANCE);
        }

        // process all callbacks
        for (FilterHandle filterHandle : callbacks) {
            EPStatementHandleCallbackFilter handleCallback = (EPStatementHandleCallbackFilter) filterHandle;

            // determine if this filter entry applies to any of the affected agent instances
            int statementId = filterHandle.getStatementId();
            AgentInstance agentInstanceFound = null;
            for (AgentInstance agentInstance : agentInstances) {
                AgentInstanceContext agentInstanceContext = agentInstance.getAgentInstanceContext();
                if (agentInstanceContext.getStatementId() == statementId && agentInstanceContext.getAgentInstanceId() == handleCallback.getAgentInstanceId()) {
                    agentInstanceFound = agentInstance;
                    break;
                }
            }
            if (agentInstanceFound == null) {   // when the callback is for some other stmt
                continue;
            }

            EPStatementAgentInstanceHandle handle = handleCallback.getAgentInstanceHandle();

            // Self-joins require that the internal dispatch happens after all streams are evaluated.
            // Priority or preemptive settings also require special ordering.
            if (handle.isCanSelfJoin() || isPrioritized) {
                Object stmtCallback = stmtCallbacks.get(agentInstanceFound);
                if (stmtCallback == null) {
                    stmtCallbacks.put(agentInstanceFound, handleCallback);
                } else if (stmtCallback instanceof ArrayDeque) {
                    ArrayDeque<EPStatementHandleCallbackFilter> q = (ArrayDeque<EPStatementHandleCallbackFilter>) stmtCallback;
                    if (!q.contains(handleCallback)) { // De-duplicate for Filter OR expression paths
                        q.add(handleCallback);
                    }
                } else {
                    ArrayDeque<EPStatementHandleCallbackFilter> q = new ArrayDeque<EPStatementHandleCallbackFilter>(4);
                    q.add((EPStatementHandleCallbackFilter) stmtCallback);
                    if (stmtCallback != handleCallback) { // De-duplicate for Filter OR expression paths
                        q.add(handleCallback);
                    }
                    stmtCallbacks.put(agentInstanceFound, q);
                }
                continue;
            }

            // no need to be sorted, process
            process(agentInstanceFound, Collections.<FilterHandle>singletonList(handleCallback), theEvent);
        }

        if (stmtCallbacks.isEmpty()) {
            return;
        }

        // Process self-join or sorted prioritized callbacks
        for (Map.Entry<AgentInstance, Object> entry : stmtCallbacks.entrySet()) {
            AgentInstance agentInstance = entry.getKey();
            Object callbackList = entry.getValue();
            if (callbackList instanceof ArrayDeque) {
                process(agentInstance, (Collection<FilterHandle>) callbackList, theEvent);
            } else {
                process(agentInstance, Collections.<FilterHandle>singletonList((FilterHandle) callbackList), theEvent);
            }
            if (agentInstance.getAgentInstanceContext().getEpStatementAgentInstanceHandle().isPreemptive()) {
                return;
            }
        }
    }

    private static void process(AgentInstance agentInstance,
                                Collection<FilterHandle> callbacks,
                                EventBean theEvent) {
        AgentInstanceContext agentInstanceContext = agentInstance.getAgentInstanceContext();
        agentInstance.getAgentInstanceContext().getAgentInstanceLock().acquireWriteLock();
        try {
            agentInstance.getAgentInstanceContext().getVariableManagementService().setLocalVersion();

            // sub-selects always go first
            for (FilterHandle handle : callbacks) {
                EPStatementHandleCallbackFilter callback = (EPStatementHandleCallbackFilter) handle;
                if (callback.getAgentInstanceHandle() != agentInstanceContext.getEpStatementAgentInstanceHandle()) {
                    continue;
                }
                callback.getFilterCallback().matchFound(theEvent, null);
            }

            agentInstanceContext.getEpStatementAgentInstanceHandle().internalDispatch();
        } catch (RuntimeException ex) {
            agentInstanceContext.getExceptionHandlingService().handleException(ex, agentInstanceContext.getEpStatementAgentInstanceHandle(), ExceptionHandlerExceptionType.PROCESS, theEvent);
        } finally {
            if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            agentInstanceContext.getAgentInstanceLock().releaseWriteLock();
        }
    }
}
