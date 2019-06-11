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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerExceptionType;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryUtil;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextStatementEventEvaluator;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewTerminable;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.view.core.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AgentInstanceUtil {
    private static final Logger log = LoggerFactory.getLogger(AgentInstanceUtil.class);

    public static void evaluateEventForStatement(EventBean theEvent,
                                                 Map<String, Object> optionalTriggeringPattern,
                                                 List<AgentInstance> agentInstances,
                                                 AgentInstanceContext agentInstanceContextCreate) {
        ContextStatementEventEvaluator evaluator = agentInstanceContextCreate.getContextServiceFactory().getContextStatementEventEvaluator();
        if (theEvent != null) {
            evaluator.evaluateEventForStatement(theEvent, agentInstances, agentInstanceContextCreate);
        }
        if (optionalTriggeringPattern != null) {
            // evaluation order definition is up to the originator of the triggering pattern
            for (Map.Entry<String, Object> entry : optionalTriggeringPattern.entrySet()) {
                if (entry.getValue() instanceof EventBean) {
                    evaluator.evaluateEventForStatement((EventBean) entry.getValue(), agentInstances, agentInstanceContextCreate);
                } else if (entry.getValue() instanceof EventBean[]) {
                    EventBean[] eventsArray = (EventBean[]) entry.getValue();
                    for (EventBean eventElement : eventsArray) {
                        evaluator.evaluateEventForStatement(eventElement, agentInstances, agentInstanceContextCreate);
                    }
                }
            }
        }
    }

    public static void contextPartitionTerminate(int agentInstanceId, ContextControllerStatementDesc statementDesc, Map<String, Object> terminationProperties, boolean leaveLocksAcquired, List<AgentInstance> agentInstancesLocksHeld) {
        StatementContext statementContext = statementDesc.getLightweight().getStatementContext();
        StatementResourceHolder holder = statementContext.getStatementCPCacheService().makeOrGetEntryCanNull(agentInstanceId, statementContext);

        if (terminationProperties != null) {
            MappedEventBean mappedEventBean = (MappedEventBean) holder.getAgentInstanceContext().getContextProperties();
            mappedEventBean.getProperties().putAll(terminationProperties);
        }

        // we are not removing statement resources from memory as they may still be used for the same event
        stop(holder.getAgentInstanceStopCallback(), holder.getAgentInstanceContext(), holder.getFinalView(), false, leaveLocksAcquired);
        if (leaveLocksAcquired) {
            agentInstancesLocksHeld.add(new AgentInstance(holder.getAgentInstanceStopCallback(), holder.getAgentInstanceContext(), holder.getFinalView()));
        }
    }

    public static void stop(AgentInstanceStopCallback stopCallback, AgentInstanceContext agentInstanceContext, Viewable finalView, boolean isStatementStop, boolean leaveLocksAcquired) {

        agentInstanceContext.getInstrumentationProvider().qContextPartitionDestroy(agentInstanceContext);

        // obtain statement lock
        StatementAgentInstanceLock lock = agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();
        lock.acquireWriteLock();
        try {
            if (finalView instanceof OutputProcessViewTerminable && !isStatementStop) {
                OutputProcessViewTerminable terminable = (OutputProcessViewTerminable) finalView;
                terminable.terminated();
            }

            stopSafe(stopCallback, agentInstanceContext);

            // release resource
            if (agentInstanceContext.getStatementContext().getStatementAIResourceRegistry() != null) {
                agentInstanceContext.getStatementContext().getStatementAIResourceRegistry().deassign(agentInstanceContext.getAgentInstanceId());
            }

            // cause any remaining schedules, that may concide with the caller's schedule, to be ignored
            agentInstanceContext.getEpStatementAgentInstanceHandle().setDestroyed(true);

            // cause any filters, that may concide with the caller's filters, to be ignored
            agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(Long.MAX_VALUE);

            if (agentInstanceContext.getAgentInstanceId() != -1) {
                agentInstanceContext.getAuditProvider().contextPartition(false, agentInstanceContext);
            }
        } finally {
            if (!leaveLocksAcquired) {
                if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                    agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
                }
                lock.releaseWriteLock();
            }
            agentInstanceContext.getInstrumentationProvider().aContextPartitionDestroy();
        }
    }

    public static void stopSafe(AgentInstanceStopCallback stopMethod, AgentInstanceContext agentInstanceContext) {
        AgentInstanceStopServices stopServices = new AgentInstanceStopServices(agentInstanceContext);

        Collection<AgentInstanceStopCallback> additionalTerminations = agentInstanceContext.getTerminationCallbackRO();
        for (AgentInstanceStopCallback stop : additionalTerminations) {
            try {
                stop.stop(stopServices);
            } catch (RuntimeException e) {
                handleStopException(e, agentInstanceContext);
            }
        }

        try {
            stopMethod.stop(stopServices);
        } catch (RuntimeException e) {
            handleStopException(e, agentInstanceContext);
        }
    }

    public static AgentInstanceStopCallback finalizeSafeStopCallbacks(List<AgentInstanceStopCallback> stopCallbacks) {
        AgentInstanceStopCallback[] stopCallbackArray = stopCallbacks.toArray(new AgentInstanceStopCallback[stopCallbacks.size()]);
        return new AgentInstanceStopCallback() {
            public void stop(AgentInstanceStopServices services) {
                for (AgentInstanceStopCallback callback : stopCallbackArray) {
                    try {
                        callback.stop(services);
                    } catch (RuntimeException e) {
                        handleStopException(e, services.getAgentInstanceContext());
                    }
                }
            }
        };
    }

    private static void handleStopException(RuntimeException e, AgentInstanceContext agentInstanceContext) {
        agentInstanceContext.getExceptionHandlingService().handleException(e, agentInstanceContext.getEpStatementAgentInstanceHandle(), ExceptionHandlerExceptionType.UNDEPLOY, null);
    }

    public static AgentInstance startStatement(StatementContextRuntimeServices services, int assignedContextId, ContextControllerStatementDesc statementDesc, MappedEventBean contextBean, AgentInstanceFilterProxy proxy) {
        StatementAgentInstanceFactoryResult result = AgentInstanceUtil.start(services, statementDesc, assignedContextId, contextBean, proxy, false);
        return new AgentInstance(result.getStopCallback(), result.getAgentInstanceContext(), result.getFinalView());
    }

    public static StatementAgentInstanceFactoryResult start(StatementContextRuntimeServices services,
                                                            ContextControllerStatementDesc statement,
                                                            int agentInstanceId,
                                                            MappedEventBean contextProperties,
                                                            AgentInstanceFilterProxy agentInstanceFilterProxy,
                                                            boolean isRecoveringResilient) {
        StatementContext statementContext = statement.getLightweight().getStatementContext();

        // create handle that comtains lock for use in scheduling and filter callbacks
        StatementAgentInstanceLock lock = statementContext.getStatementAIFactoryProvider().getFactory().obtainAgentInstanceLock(statementContext, agentInstanceId);
        EPStatementAgentInstanceHandle agentInstanceHandle = new EPStatementAgentInstanceHandle(statementContext.getEpStatementHandle(), agentInstanceId, lock);

        AuditProvider auditProvider = statementContext.getStatementInformationals().getAuditProvider();
        InstrumentationCommon instrumentationProvider = statementContext.getStatementInformationals().getInstrumentationProvider();
        AgentInstanceContext agentInstanceContext = new AgentInstanceContext(statementContext, agentInstanceHandle, agentInstanceFilterProxy, contextProperties, auditProvider, instrumentationProvider);
        if (agentInstanceId != -1) {
            agentInstanceContext.getAuditProvider().contextPartition(true, agentInstanceContext);
        }
        StatementAgentInstanceLock statementAgentInstanceLock = agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock();

        agentInstanceContext.getInstrumentationProvider().qContextPartitionAllocate(agentInstanceContext);

        statementAgentInstanceLock.acquireWriteLock();

        try {
            // start
            StatementAgentInstanceFactoryResult startResult = statement.getLightweight().getStatementProvider().getStatementAIFactoryProvider().getFactory().newContext(agentInstanceContext, isRecoveringResilient);

            // hook up with listeners+subscribers
            startResult.getFinalView().setChild(statement.getContextMergeView()); // hook output to merge view

            // assign agents for expression-node based strategies
            StatementAIResourceRegistry aiResourceRegistry = statementContext.getStatementAIResourceRegistry();
            AIRegistryUtil.assignFutures(aiResourceRegistry, agentInstanceId, startResult.getOptionalAggegationService(), startResult.getPriorStrategies(), startResult.getPreviousGetterStrategies(), startResult.getSubselectStrategies(), startResult.getTableAccessStrategies(),
                    startResult.getRowRecogPreviousStrategy());

            // execute preloads, if any
            if (startResult.getPreloadList() != null) {
                for (StatementAgentInstancePreload preload : startResult.getPreloadList()) {
                    preload.executePreload();
                }
            }

            StatementResourceHolder holder = services.getStatementResourceHolderBuilder().build(agentInstanceContext, startResult);
            statementContext.getStatementCPCacheService().getStatementResourceService().setPartitioned(agentInstanceId, holder);

            // instantiate
            return startResult;
        } finally {
            if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                agentInstanceContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            statementAgentInstanceLock.releaseWriteLock();
            agentInstanceContext.getInstrumentationProvider().aContextPartitionAllocate();
        }
    }

    public static boolean evaluateFilterForStatement(EventBean theEvent, AgentInstanceContext agentInstanceContext, FilterHandle filterHandle) {
        // context was created - reevaluate for the given event
        ArrayDeque<FilterHandle> callbacks = new ArrayDeque<FilterHandle>();
        agentInstanceContext.getFilterService().evaluate(theEvent, callbacks, agentInstanceContext.getStatementContext().getStatementId());

        try {
            agentInstanceContext.getVariableManagementService().setLocalVersion();

            // sub-selects always go first
            for (FilterHandle handle : callbacks) {
                if (handle.equals(filterHandle)) {
                    return true;
                }
            }

            agentInstanceContext.getEpStatementAgentInstanceHandle().internalDispatch();

        } catch (RuntimeException ex) {
            agentInstanceContext.getExceptionHandlingService().handleException(ex, agentInstanceContext.getEpStatementAgentInstanceHandle(), ExceptionHandlerExceptionType.PROCESS, theEvent);
        }

        return false;
    }

    public static StatementAgentInstanceLock newLock(StatementContext statementContext) {
        return statementContext.getStatementAgentInstanceLockFactory().getStatementLock(statementContext.getStatementName(), statementContext.getAnnotations(), statementContext.isStatelessSelect(), statementContext.getStatementType());
    }
}
