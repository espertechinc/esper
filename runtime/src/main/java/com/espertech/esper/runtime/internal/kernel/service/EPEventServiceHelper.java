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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerExceptionType;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.collection.ArrayBackedCollection;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

public class EPEventServiceHelper {
    /**
     * Processing multiple schedule matches for a statement.
     *
     * @param handle         statement handle
     * @param callbackObject object containing matches
     * @param services       runtime services
     */
    public static void processStatementScheduleMultiple(EPStatementAgentInstanceHandle handle, Object callbackObject, EPServicesEvaluation services) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTimeCP(handle, services.getSchedulingService().getTime());
        }

        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try {
            if (!handle.isDestroyed()) {
                if (handle.isHasVariables()) {
                    services.getVariableManagementService().setLocalVersion();
                }

                if (callbackObject instanceof ArrayDeque) {
                    ArrayDeque<ScheduleHandleCallback> callbackList = (ArrayDeque<ScheduleHandleCallback>) callbackObject;
                    for (ScheduleHandleCallback callback : callbackList) {
                        callback.scheduledTrigger();
                    }
                } else {
                    ScheduleHandleCallback callback = (ScheduleHandleCallback) callbackObject;
                    callback.scheduledTrigger();
                }

                // internal join processing, if applicable
                handle.internalDispatch();
            }
        } catch (RuntimeException ex) {
            services.getExceptionHandlingService().handleException(ex, handle, ExceptionHandlerExceptionType.PROCESS, null);
        } finally {
            if (handle.isHasTableAccess()) {
                services.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getStatementAgentInstanceLock().releaseWriteLock();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aTimeCP();
            }
        }
    }

    /**
     * Processing single schedule matche for a statement.
     *
     * @param handle   statement handle
     * @param services runtime services
     */
    public static void processStatementScheduleSingle(EPStatementHandleCallbackSchedule handle, EPServicesEvaluation services) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTimeCP(handle.getAgentInstanceHandle(), services.getSchedulingService().getTime());
        }

        StatementAgentInstanceLock statementLock = handle.getAgentInstanceHandle().getStatementAgentInstanceLock();
        statementLock.acquireWriteLock();
        try {
            if (!handle.getAgentInstanceHandle().isDestroyed()) {
                if (handle.getAgentInstanceHandle().isHasVariables()) {
                    services.getVariableManagementService().setLocalVersion();
                }

                handle.getScheduleCallback().scheduledTrigger();
                handle.getAgentInstanceHandle().internalDispatch();
            }
        } catch (RuntimeException ex) {
            services.getExceptionHandlingService().handleException(ex, handle.getAgentInstanceHandle(), ExceptionHandlerExceptionType.PROCESS, null);
        } finally {
            if (handle.getAgentInstanceHandle().isHasTableAccess()) {
                services.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getAgentInstanceHandle().getStatementAgentInstanceLock().releaseWriteLock();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aTimeCP();
            }
        }
    }

    public static ThreadLocal<EPEventServiceThreadLocalEntry> allocateThreadLocals(boolean isPrioritized, String runtimeURI, Configuration configuration, EventBeanService eventBeanService, ExceptionHandlingService exceptionHandlingService, SchedulingService schedulingService, TimeZone timeZone, TimeAbacus timeAbacus, VariableManagementService variableManagementService) {
        ExpressionResultCacheService expressionResultCacheService = new ExpressionResultCacheService(configuration.getRuntime().getExecution().getDeclaredExprValueCacheSize());
        return ThreadLocal.withInitial(() -> {
            ArrayBackedCollection<FilterHandle> filterHandles = new ArrayBackedCollection<>(100);
            ArrayBackedCollection<ScheduleHandle> scheduleHandles = new ArrayBackedCollection<>(100);

            Map<EPStatementAgentInstanceHandle, Object> matchesPerStmt;
            Map<EPStatementAgentInstanceHandle, Object> schedulesPerStmt;
            if (isPrioritized) {
                matchesPerStmt = new TreeMap<>(EPStatementAgentInstanceHandleComparator.INSTANCE);
                schedulesPerStmt = new TreeMap<>(EPStatementAgentInstanceHandleComparator.INSTANCE);
            } else {
                matchesPerStmt = new HashMap<>();
                schedulesPerStmt = new HashMap<>();
            }

            ExprEvaluatorContext runtimeFilterAndDispatchTimeContext = new EPEventServiceExprEvaluatorContext(runtimeURI, eventBeanService, exceptionHandlingService, expressionResultCacheService, schedulingService, timeZone, timeAbacus, variableManagementService);

            WorkQueue workQueue;
            boolean eventPrecedence = configuration.getRuntime().getExecution().isPrecedenceEnabled();
            boolean insertIntoLatching = configuration.getRuntime().getThreading().isInsertIntoDispatchPreserveOrder();
            if (!eventPrecedence) {
                if (insertIntoLatching) {
                    // the default work queue may or may not latch depending on statement stateless ascpect and lock-type configuration
                    workQueue = new WorkQueueNoPrecedenceMayLatch();
                } else {
                    workQueue = new WorkQueueNoPrecedenceNoLatch();
                }
            } else {
                if (!insertIntoLatching) {
                    workQueue = new WorkQueueWPrecedenceNoLatch();
                } else {
                    workQueue = new WorkQueueWPrecedenceMayLatch();
                }
            }

            return new EPEventServiceThreadLocalEntry(workQueue, filterHandles, scheduleHandles, matchesPerStmt, schedulesPerStmt, runtimeFilterAndDispatchTimeContext);
        });
    }
}
