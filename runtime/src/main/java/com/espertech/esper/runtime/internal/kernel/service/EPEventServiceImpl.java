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

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntime;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerExceptionType;
import com.espertech.esper.common.internal.collection.ArrayBackedCollection;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.EventSenderObjectArray;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.avro.EventSenderAvro;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.EventSenderBean;
import com.espertech.esper.common.internal.event.core.NaturalEventBean;
import com.espertech.esper.common.internal.event.json.compiletime.EventSenderJsonImpl;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.EventSenderMap;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import com.espertech.esper.common.internal.event.xml.EventSenderXMLDOM;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchSpin;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchWait;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import com.espertech.esper.common.internal.util.MetricUtil;
import com.espertech.esper.common.internal.util.ThreadLogUtil;
import com.espertech.esper.runtime.client.UnmatchedListener;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.kernel.thread.*;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.runtime.internal.metrics.jmx.JmxGetter;
import com.espertech.esper.runtime.internal.schedulesvcimpl.ScheduleVisit;
import com.espertech.esper.runtime.internal.schedulesvcimpl.ScheduleVisitor;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;
import com.espertech.esper.runtime.internal.statementlifesvc.StatementLifecycleService;
import com.espertech.esper.runtime.internal.timer.TimerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.espertech.esper.runtime.internal.kernel.service.EPEventServiceHelper.*;

/**
 * Implements runtime interface. Also accepts timer callbacks for synchronizing time events with regular events
 * sent in.
 */
public class EPEventServiceImpl implements EPEventServiceSPI, InternalEventRouteDest, TimerCallback, EPRuntimeEventProcessWrapped, EPEventServiceQueueProcessor {
    protected static final Logger log = LoggerFactory.getLogger(EPEventServiceImpl.class);
    public static final int MAX_FILTER_FAULT_COUNT = 10;

    protected EPServicesContext services;
    private boolean inboundThreading;
    private boolean routeThreading;
    private boolean timerThreading;
    private boolean isUsingExternalClocking;
    protected boolean isPrioritized;
    protected volatile UnmatchedListener unmatchedListener;
    private AtomicLong routedInternal;
    private AtomicLong routedExternal;
    private InternalEventRouter internalEventRouter;
    protected ThreadLocal<EPEventServiceThreadLocalEntry> threadLocals;

    /**
     * Constructor.
     *
     * @param services - references to services
     */
    public EPEventServiceImpl(EPServicesContext services) {
        this.services = services;
        this.inboundThreading = services.getThreadingService().isInboundThreading();
        this.routeThreading = services.getThreadingService().isRouteThreading();
        this.timerThreading = services.getThreadingService().isTimerThreading();

        ConfigurationRuntime runtimeConfig = services.getRuntimeSettingsService().getConfigurationRuntime();
        isUsingExternalClocking = !runtimeConfig.getThreading().isInternalTimerEnabled();
        isPrioritized = runtimeConfig.getExecution().isPrioritized();
        routedInternal = new AtomicLong();
        routedExternal = new AtomicLong();

        initThreadLocals();

        services.getThreadingService().initThreading(getRuntimeURI(), services);
    }

    public EPServicesContext getServices() {
        return services;
    }

    /**
     * Sets the route for events to use
     *
     * @param internalEventRouter router
     */
    public void setInternalEventRouter(InternalEventRouter internalEventRouter) {
        this.internalEventRouter = internalEventRouter;
    }

    @JmxGetter(name = "NumInsertIntoEvents", description = "Number of inserted-into events")
    public long getRoutedInternal() {
        return routedInternal.get();
    }

    @JmxGetter(name = "NumRoutedEvents", description = "Number of routed events")
    public long getRoutedExternal() {
        return routedExternal.get();
    }

    public void timerCallback() {
        long msec = services.getTimeSourceService().getTimeMillis();

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled() && (ExecutionPathDebugLog.isTimerDebugEnabled))) {
            log.debug(".timerCallback Evaluating scheduled callbacks, time is " + msec);
        }

        advanceTime(msec);
    }

    public void sendEventAvro(Object avroGenericDataDotRecord, String avroEventTypeName) {
        if (avroGenericDataDotRecord == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendMap Processing event " + avroGenericDataDotRecord.toString());
        }

        if (inboundThreading) {
            services.getThreadingService().submitInbound(new InboundUnitSendAvro(avroGenericDataDotRecord, avroEventTypeName, this, services));
        } else {
            EventBean eventBean = wrapEventAvro(avroGenericDataDotRecord, avroEventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void sendEventJson(String json, String jsonEventTypeName) {
        if (json == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendEventJson Processing event " + json);
        }

        if (inboundThreading) {
            services.getThreadingService().submitInbound(new InboundUnitSendJson(json, jsonEventTypeName, this, services));
        } else {
            EventBean eventBean = wrapEventJson(json, jsonEventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void sendEventBean(Object theEvent, String eventTypeName) {
        if (theEvent == null) {
            log.error(".sendEvent Null object supplied");
            return;
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendEvent Processing event " + theEvent);
        }

        if (inboundThreading) {
            services.getThreadingService().submitInbound(new InboundUnitSendEvent(theEvent, eventTypeName, this, services));
        } else {
            EventBean eventBean = services.getEventTypeResolvingBeanFactory().adapterForBean(theEvent, eventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void advanceTime(long time) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qStimulantTime(services.getSchedulingService().getTime(), time, time, false, null, services.getRuntimeURI());
        }

        services.getSchedulingService().setTime(time);

        services.getMetricReportingService().processTimeEvent(time);

        processSchedule(time);

        // Let listeners know of results
        dispatch();

        // Work off the event queue if any events accumulated in there via a route()
        processThreadWorkQueue();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aStimulantTime();
        }
    }

    public void advanceTimeSpan(long targetTime) {
        advanceTimeSpanInternal(targetTime, null);
    }

    public void advanceTimeSpan(long targetTime, long resolution) {
        advanceTimeSpanInternal(targetTime, resolution);
    }

    public Long getNextScheduledTime() {
        return services.getSchedulingService().getNearestTimeHandle();
    }

    private void advanceTimeSpanInternal(long targetTime, Long optionalResolution) {
        long currentTime = services.getSchedulingService().getTime();

        while (currentTime < targetTime) {

            if ((optionalResolution != null) && (optionalResolution > 0)) {
                currentTime += optionalResolution;
            } else {
                Long nearest = services.getSchedulingService().getNearestTimeHandle();
                if (nearest == null) {
                    currentTime = targetTime;
                } else {
                    currentTime = nearest;
                }
            }
            if (currentTime > targetTime) {
                currentTime = targetTime;
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qStimulantTime(services.getSchedulingService().getTime(), currentTime, targetTime, true, optionalResolution, services.getRuntimeURI());
            }

            services.getSchedulingService().setTime(currentTime);

            processSchedule(currentTime);

            // Let listeners know of results
            dispatch();

            // Work off the event queue if any events accumulated in there via a route()
            processThreadWorkQueue();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aStimulantTime();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aStimulantTime();
        }
    }

    public void sendEventXMLDOM(org.w3c.dom.Node node, String eventTypeName) {
        if (node == null) {
            log.error(".sendEvent Null object supplied");
            return;
        }

        // Process event
        if (inboundThreading) {
            services.getThreadingService().submitInbound(new InboundUnitSendDOM(node, eventTypeName, this, services));
        } else {
            EventBean eventBean = wrapEventBeanXMLDOM(node, eventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void sendEventObjectArray(Object[] propertyValues, String eventTypeName) throws EPException {
        if (propertyValues == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendEventObjectArray Processing event " + Arrays.toString(propertyValues));
        }

        if (inboundThreading) {
            services.getThreadingService().submitInbound(new InboundUnitSendObjectArray(propertyValues, eventTypeName, this, services));
        } else {
            EventBean eventBean = wrapEventObjectArray(propertyValues, eventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void sendEventMap(Map<String, Object> map, String mapEventTypeName) throws EPException {
        if (map == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendMap Processing event " + map);
        }

        if (inboundThreading) {
            services.getThreadingService().submitInbound(new InboundUnitSendMap(map, mapEventTypeName, this, services));
        } else {
            EventBean eventBean = wrapEventMap(map, mapEventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void routeEventBean(EventBean theEvent) {
        threadLocals.get().getWorkQueue().add(theEvent);
    }

    // Internal route of events via insert-into, holds a statement lock
    public void route(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront, int precedence) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRouteBetweenStmt(theEvent, epStatementHandle, addToFront);
        }

        if (theEvent instanceof NaturalEventBean) {
            theEvent = ((NaturalEventBean) theEvent).getOptionalSynthetic();
        }
        routedInternal.incrementAndGet();

        WorkQueue threadWorkQueue = threadLocals.get().getWorkQueue();
        threadWorkQueue.add(theEvent, epStatementHandle, addToFront, precedence);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRouteBetweenStmt();
        }
    }

    public void processWrappedEvent(EventBean eventBean) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qStimulantEvent(eventBean, services.getRuntimeURI());
        }

        EPEventServiceThreadLocalEntry tlEntry = threadLocals.get();
        if (internalEventRouter.isHasPreprocessing()) {
            eventBean = internalEventRouter.preprocess(eventBean, tlEntry.getExprEvaluatorContext(), InstrumentationHelper.get());
            if (eventBean == null) {
                return;
            }
        }

        // Acquire main processing lock which locks out statement management
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getRuntimeURI(), true);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            tlEntry.getMatchesArrayThreadLocal().clear();
            throw new EPException(ex);
        } finally {
            services.getEventProcessingRWLock().releaseReadLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEvent();
            }
        }

        // Dispatch results to listeners
        // Done outside of the read-lock to prevent lockups when listeners create statements
        dispatch();

        // Work off the event queue if any events accumulated in there via a route() or insert-into
        processThreadWorkQueue();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aStimulantEvent();
        }
    }

    /**
     * Works off the thread's work queue.
     */
    public void processThreadWorkQueue() {
        WorkQueue queues = threadLocals.get().getWorkQueue();

        if (queues.isFrontEmpty()) {
            boolean haveDispatched = services.getNamedWindowDispatchService().dispatch();
            if (haveDispatched) {
                // Dispatch results to listeners
                dispatch();

                if (!queues.isFrontEmpty()) {
                    processThreadWorkQueueFront(queues);
                }
            }
        } else {
            processThreadWorkQueueFront(queues);
        }

        while (queues.processBack(this)) {
            boolean haveDispatched = services.getNamedWindowDispatchService().dispatch();
            if (haveDispatched) {
                dispatch();
            }

            if (!queues.isFrontEmpty()) {
                processThreadWorkQueueFront(queues);
            }
        }
    }

    private void processThreadWorkQueueFront(WorkQueue queues) {
        while (queues.processFront(this)) {
            boolean haveDispatched = services.getNamedWindowDispatchService().dispatch();
            if (haveDispatched) {
                dispatch();
            }
        }
    }

    public void processThreadWorkQueueLatchedWait(InsertIntoLatchWait insertIntoLatch) {
        // wait for the latch to complete
        EventBean eventBean = insertIntoLatch.await();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getRuntimeURI(), false);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            threadLocals.get().getMatchesArrayThreadLocal().clear();
            throw ex;
        } finally {
            insertIntoLatch.done();
            services.getEventProcessingRWLock().releaseReadLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEvent();
            }
        }

        dispatch();
    }

    public void processThreadWorkQueueLatchedSpin(InsertIntoLatchSpin insertIntoLatch) {
        // wait for the latch to complete
        EventBean eventBean = insertIntoLatch.await();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getRuntimeURI(), false);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            threadLocals.get().getMatchesArrayThreadLocal().clear();
            throw ex;
        } finally {
            insertIntoLatch.done();
            services.getEventProcessingRWLock().releaseReadLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEvent();
            }
        }

        dispatch();
    }

    public void processThreadWorkQueueUnlatched(Object item) {
        EventBean eventBean;
        if (item instanceof EventBean) {
            eventBean = (EventBean) item;
        } else {
            throw new IllegalStateException("Unexpected item type " + item + " in queue");
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getRuntimeURI(), false);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            threadLocals.get().getMatchesArrayThreadLocal().clear();
            throw ex;
        } finally {
            services.getEventProcessingRWLock().releaseReadLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEvent();
            }
        }

        dispatch();
    }

    protected void processMatches(EventBean theEvent) {
        // get matching filters
        EPEventServiceThreadLocalEntry tlEntry = threadLocals.get();
        ArrayBackedCollection<FilterHandle> matches = tlEntry.getMatchesArrayThreadLocal();
        ExprEvaluatorContext ctx = tlEntry.getExprEvaluatorContext();
        long version = services.getFilterService().evaluate(theEvent, matches, ctx);

        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.trace("Found matches for underlying ", matches.size(), theEvent.getUnderlying());
        }

        if (matches.size() == 0) {
            if (unmatchedListener != null) {
                services.getEventProcessingRWLock().releaseReadLock();  // Allow listener to create new statements
                try {
                    unmatchedListener.update(theEvent);
                } catch (Throwable t) {
                    log.error("Exception thrown by unmatched listener: " + t.getMessage(), t);
                } finally {
                    // acquire read lock for release by caller
                    services.getEventProcessingRWLock().acquireReadLock();
                }
            }
            return;
        }

        Map<EPStatementAgentInstanceHandle, Object> stmtCallbacks = tlEntry.getMatchesPerStmtThreadLocal();
        Object[] matchArray = matches.getArray();
        int entryCount = matches.size();

        for (int i = 0; i < entryCount; i++) {
            EPStatementHandleCallbackFilter handleCallback = (EPStatementHandleCallbackFilter) matchArray[i];
            EPStatementAgentInstanceHandle handle = handleCallback.getAgentInstanceHandle();

            // Self-joins require that the internal dispatch happens after all streams are evaluated.
            // Priority or preemptive settings also require special ordering.
            if (handle.isCanSelfJoin() || isPrioritized) {
                Object callbacks = stmtCallbacks.get(handle);
                if (callbacks == null) {
                    stmtCallbacks.put(handle, handleCallback.getFilterCallback());
                } else if (callbacks instanceof ArrayDeque) {
                    ArrayDeque<FilterHandleCallback> q = (ArrayDeque<FilterHandleCallback>) callbacks;
                    q.add(handleCallback.getFilterCallback());
                } else {
                    ArrayDeque<FilterHandleCallback> q = new ArrayDeque<>(4);
                    q.add((FilterHandleCallback) callbacks);
                    q.add(handleCallback.getFilterCallback());
                    stmtCallbacks.put(handle, q);
                }
                continue;
            }

            if (handle.getStatementHandle().getMetricsHandle().isEnabled()) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementFilterSingle(handle, handleCallback, theEvent, version, 0);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                services.getMetricReportingService().accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
            } else {
                if (routeThreading) {
                    services.getThreadingService().submitRoute(new RouteUnitSingle(this, handleCallback, theEvent, version));
                } else {
                    processStatementFilterSingle(handle, handleCallback, theEvent, version, 0);
                }
            }
        }
        matches.clear();
        if (stmtCallbacks.isEmpty()) {
            return;
        }

        for (Map.Entry<EPStatementAgentInstanceHandle, Object> entry : stmtCallbacks.entrySet()) {
            EPStatementAgentInstanceHandle handle = entry.getKey();
            Object callbackList = entry.getValue();

            if (handle.getStatementHandle().getMetricsHandle().isEnabled()) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementFilterMultiple(handle, callbackList, theEvent, version, 0);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                int size = 1;
                if (callbackList instanceof Collection) {
                    size = ((Collection) callbackList).size();
                }
                services.getMetricReportingService().accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, size);
            } else {
                if (routeThreading) {
                    services.getThreadingService().submitRoute(new RouteUnitMultiple(this, callbackList, theEvent, handle, version));
                } else {
                    processStatementFilterMultiple(handle, callbackList, theEvent, version, 0);
                }
            }

            if (isPrioritized && handle.isPreemptive()) {
                break;
            }
        }
        stmtCallbacks.clear();
    }

    /**
     * Processing multiple filter matches for a statement.
     *
     * @param handle           statement handle
     * @param callbackList     object containing callbacks
     * @param theEvent         to process
     * @param version          filter version
     * @param filterFaultCount filter fault count
     */
    public void processStatementFilterMultiple(EPStatementAgentInstanceHandle handle, Object callbackList, EventBean theEvent, long version, int filterFaultCount) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEventCP(theEvent, handle, services.getSchedulingService().getTime());
        }
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try {
            if (handle.isHasVariables()) {
                services.getVariableManagementService().setLocalVersion();
            }
            if (!handle.isCurrentFilter(version)) {
                boolean handled = false;
                if (handle.getFilterFaultHandler() != null) {
                    handled = handle.getFilterFaultHandler().handleFilterFault(theEvent, version);
                }
                if (!handled && filterFaultCount < MAX_FILTER_FAULT_COUNT) {
                    handleFilterFault(handle, theEvent, filterFaultCount);
                }
            } else {
                if (callbackList instanceof Collection) {
                    Collection<FilterHandleCallback> callbacks = (Collection<FilterHandleCallback>) callbackList;
                    handle.getMultiMatchHandler().handle(callbacks, theEvent);
                } else {
                    FilterHandleCallback single = (FilterHandleCallback) callbackList;
                    single.matchFound(theEvent, null);
                }

                // internal join processing, if applicable
                handle.internalDispatch();
            }
        } catch (RuntimeException ex) {
            services.getExceptionHandlingService().handleException(ex, handle, ExceptionHandlerExceptionType.PROCESS, theEvent);
        } finally {
            if (handle.isHasTableAccess()) {
                services.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEventCP();
            }
        }
    }

    /**
     * Process a single match.
     *
     * @param handle           statement
     * @param handleCallback   callback
     * @param theEvent         event to indicate
     * @param version          filter version
     * @param filterFaultCount filter fault count
     */
    public void processStatementFilterSingle(EPStatementAgentInstanceHandle handle, EPStatementHandleCallbackFilter handleCallback, EventBean theEvent, long version, int filterFaultCount) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEventCP(theEvent, handle, services.getSchedulingService().getTime());
        }
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try {
            if (handle.isHasVariables()) {
                services.getVariableManagementService().setLocalVersion();
            }
            if (!handle.isCurrentFilter(version)) {
                boolean handled = false;
                if (handle.getFilterFaultHandler() != null) {
                    handled = handle.getFilterFaultHandler().handleFilterFault(theEvent, version);
                }
                if (!handled && filterFaultCount < MAX_FILTER_FAULT_COUNT) {
                    handleFilterFault(handle, theEvent, filterFaultCount);
                }
            } else {
                handleCallback.getFilterCallback().matchFound(theEvent, null);
            }

            // internal join processing, if applicable
            handle.internalDispatch();
        } catch (RuntimeException ex) {
            services.getExceptionHandlingService().handleException(ex, handle, ExceptionHandlerExceptionType.PROCESS, theEvent);
        } finally {
            if (handle.isHasTableAccess()) {
                services.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handleCallback.getAgentInstanceHandle().getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEventCP();
            }
        }
    }

    protected void handleFilterFault(EPStatementAgentInstanceHandle faultingHandle, EventBean theEvent, int filterFaultCount) {
        ArrayDeque<FilterHandle> callbacksForStatement = new ArrayDeque<>();
        long version = services.getFilterService().evaluate(theEvent, callbacksForStatement, faultingHandle.getStatementId(), threadLocals.get().getExprEvaluatorContext());

        if (callbacksForStatement.size() == 1) {
            EPStatementHandleCallbackFilter handleCallback = (EPStatementHandleCallbackFilter) callbacksForStatement.getFirst();
            processStatementFilterSingle(handleCallback.getAgentInstanceHandle(), handleCallback, theEvent, version, filterFaultCount + 1);
            return;
        }
        if (callbacksForStatement.isEmpty()) {
            return;
        }

        Map<EPStatementAgentInstanceHandle, Object> stmtCallbacks;
        if (isPrioritized) {
            stmtCallbacks = new TreeMap<EPStatementAgentInstanceHandle, Object>(EPStatementAgentInstanceHandleComparator.INSTANCE);
        } else {
            stmtCallbacks = new HashMap<EPStatementAgentInstanceHandle, Object>();
        }

        for (FilterHandle filterHandle : callbacksForStatement) {
            EPStatementHandleCallbackFilter handleCallback = (EPStatementHandleCallbackFilter) filterHandle;
            EPStatementAgentInstanceHandle handle = handleCallback.getAgentInstanceHandle();

            if (handle.isCanSelfJoin() || isPrioritized) {
                Object callbacks = stmtCallbacks.get(handle);
                if (callbacks == null) {
                    stmtCallbacks.put(handle, handleCallback.getFilterCallback());
                } else if (callbacks instanceof ArrayDeque) {
                    ArrayDeque<FilterHandleCallback> q = (ArrayDeque<FilterHandleCallback>) callbacks;
                    q.add(handleCallback.getFilterCallback());
                } else {
                    ArrayDeque<FilterHandleCallback> q = new ArrayDeque<>(4);
                    q.add((FilterHandleCallback) callbacks);
                    q.add(handleCallback.getFilterCallback());
                    stmtCallbacks.put(handle, q);
                }
                continue;
            }

            processStatementFilterSingle(handle, handleCallback, theEvent, version, filterFaultCount + 1);
        }

        if (stmtCallbacks.isEmpty()) {
            return;
        }

        for (Map.Entry<EPStatementAgentInstanceHandle, Object> entry : stmtCallbacks.entrySet()) {
            EPStatementAgentInstanceHandle handle = entry.getKey();
            Object callbackList = entry.getValue();

            processStatementFilterMultiple(handle, callbackList, theEvent, version, filterFaultCount + 1);

            if (isPrioritized && handle.isPreemptive()) {
                break;
            }
        }
    }

    /**
     * Dispatch events.
     */
    public void dispatch() {
        try {
            services.getDispatchService().dispatch();
        } catch (RuntimeException ex) {
            throw new EPException(ex);
        }
    }

    public boolean isExternalClockingEnabled() {
        return isUsingExternalClocking;
    }

    /**
     * Destroy for destroying an runtime instance: sets references to null and clears thread-locals
     */
    public void destroy() {
        services = null;
        removeFromThreadLocals();
    }

    public void initialize() {
        initThreadLocals();
    }

    public void clearCaches() {
        initThreadLocals();
    }

    public void setUnmatchedListener(UnmatchedListener listener) {
        this.unmatchedListener = listener;
    }

    public long getCurrentTime() {
        return services.getSchedulingService().getTime();
    }

    public String getRuntimeURI() {
        return services.getRuntimeURI();
    }

    private void removeFromThreadLocals() {
        if (threadLocals != null) {
            threadLocals.remove();
        }
    }

    private void initThreadLocals() {
        removeFromThreadLocals();
        threadLocals = allocateThreadLocals(isPrioritized, services.getRuntimeURI(), services.getConfigSnapshot(), services.getEventBeanService(), services.getExceptionHandlingService(), services.getSchedulingService(), services.getClasspathImportServiceRuntime().getTimeZone(), services.getClasspathImportServiceRuntime().getTimeAbacus(), services.getVariableManagementService());
    }

    private void processSchedule(long time) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTime(time, services.getRuntimeURI());
        }

        ArrayBackedCollection<ScheduleHandle> handles = threadLocals.get().getScheduleArrayThreadLocal();

        // Evaluation of schedules is protected by an optional scheduling service lock and then the runtimelock
        // We want to stay in this order for allowing the runtimelock as a second-order lock to the
        // services own lock, if it has one.
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            services.getSchedulingService().evaluate(handles);
        } finally {
            services.getEventProcessingRWLock().releaseReadLock();
        }

        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processScheduleHandles(handles);
        } catch (RuntimeException ex) {
            handles.clear();
            throw ex;
        } finally {
            services.getEventProcessingRWLock().releaseReadLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aTime();
            }
        }
    }

    public void processScheduleHandles(ArrayBackedCollection<ScheduleHandle> handles) {
        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.trace("Found schedules for", handles.size());
        }

        if (handles.size() == 0) {
            return;
        }

        // handle 1 result separately for performance reasons
        if (handles.size() == 1) {
            Object[] handleArray = handles.getArray();
            EPStatementHandleCallbackSchedule handle = (EPStatementHandleCallbackSchedule) handleArray[0];

            if (handle.getAgentInstanceHandle().getStatementHandle().getMetricsHandle().isEnabled()) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementScheduleSingle(handle, services);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                services.getMetricReportingService().accountTime(handle.getAgentInstanceHandle().getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
            } else {
                if (timerThreading) {
                    services.getThreadingService().submitTimerWork(new TimerUnitSingle(services, this, handle));
                } else {
                    processStatementScheduleSingle(handle, services);
                }
            }

            handles.clear();
            return;
        }

        Object[] matchArray = handles.getArray();
        int entryCount = handles.size();

        // sort multiple matches for the event into statements
        Map<EPStatementAgentInstanceHandle, Object> stmtCallbacks = threadLocals.get().getSchedulePerStmtThreadLocal();
        stmtCallbacks.clear();
        for (int i = 0; i < entryCount; i++) {
            EPStatementHandleCallbackSchedule handleCallback = (EPStatementHandleCallbackSchedule) matchArray[i];
            EPStatementAgentInstanceHandle handle = handleCallback.getAgentInstanceHandle();
            ScheduleHandleCallback callback = handleCallback.getScheduleCallback();

            Object entry = stmtCallbacks.get(handle);

            // This statement has not been encountered before
            if (entry == null) {
                stmtCallbacks.put(handle, callback);
                continue;
            }

            // This statement has been encountered once before
            if (entry instanceof ScheduleHandleCallback) {
                ScheduleHandleCallback existingCallback = (ScheduleHandleCallback) entry;
                ArrayDeque<ScheduleHandleCallback> entries = new ArrayDeque<ScheduleHandleCallback>();
                entries.add(existingCallback);
                entries.add(callback);
                stmtCallbacks.put(handle, entries);
                continue;
            }

            // This statement has been encountered more then once before
            ArrayDeque<ScheduleHandleCallback> entries = (ArrayDeque<ScheduleHandleCallback>) entry;
            entries.add(callback);
        }
        handles.clear();

        for (Map.Entry<EPStatementAgentInstanceHandle, Object> entry : stmtCallbacks.entrySet()) {
            EPStatementAgentInstanceHandle handle = entry.getKey();
            Object callbackObject = entry.getValue();

            if (handle.getStatementHandle().getMetricsHandle().isEnabled()) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementScheduleMultiple(handle, callbackObject, services);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                int numInput = (callbackObject instanceof Collection) ? ((Collection) callbackObject).size() : 1;
                services.getMetricReportingService().accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, numInput);
            } else {
                if (timerThreading) {
                    services.getThreadingService().submitTimerWork(new TimerUnitMultiple(services, this, handle, callbackObject));
                } else {
                    processStatementScheduleMultiple(handle, callbackObject, services);
                }
            }

            if (isPrioritized && handle.isPreemptive()) {
                break;
            }
        }
    }

    private EventBean wrapEventMap(Map<String, Object> map, String eventTypeName) {
        return services.getEventTypeResolvingBeanFactory().adapterForMap(map, eventTypeName);
    }

    private EventBean wrapEventObjectArray(Object[] objectArray, String eventTypeName) {
        return services.getEventTypeResolvingBeanFactory().adapterForObjectArray(objectArray, eventTypeName);
    }

    private EventBean wrapEventBeanXMLDOM(org.w3c.dom.Node node, String eventTypeName) {
        return services.getEventTypeResolvingBeanFactory().adapterForXMLDOM(node, eventTypeName);
    }

    private EventBean wrapEventAvro(Object avroGenericDataDotRecord, String eventTypeName) {
        return services.getEventTypeResolvingBeanFactory().adapterForAvro(avroGenericDataDotRecord, eventTypeName);
    }

    private EventBean wrapEventJson(String json, String eventTypeName) {
        return services.getEventTypeResolvingBeanFactory().adapterForJson(json, eventTypeName);
    }

    public void routeEventMap(Map<String, Object> map, String eventTypeName) {
        if (map == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }
        EventBean theEvent = services.getEventTypeResolvingBeanFactory().adapterForMap(map, eventTypeName);
        routeEventInternal(theEvent);
    }

    public void routeEventBean(Object event, String eventTypeName) {
        if (event == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }
        EventBean theEvent = services.getEventTypeResolvingBeanFactory().adapterForBean(event, eventTypeName);
        routeEventInternal(theEvent);
    }

    public void routeEventObjectArray(Object[] event, String eventTypeName) {
        if (event == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }
        EventBean theEvent = services.getEventTypeResolvingBeanFactory().adapterForObjectArray(event, eventTypeName);
        routeEventInternal(theEvent);
    }

    public void routeEventXMLDOM(Node event, String eventTypeName) {
        if (event == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }
        EventBean theEvent = services.getEventTypeResolvingBeanFactory().adapterForXMLDOM(event, eventTypeName);
        routeEventInternal(theEvent);
    }

    public void routeEventAvro(Object avroGenericDataDotRecord, String eventTypeName) {
        if (avroGenericDataDotRecord == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }
        EventBean theEvent = services.getEventTypeResolvingBeanFactory().adapterForAvro(avroGenericDataDotRecord, eventTypeName);
        routeEventInternal(theEvent);
    }

    public void routeEventJson(String json, String eventTypeName) {
        if (json == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }
        EventBean theEvent = services.getEventTypeResolvingBeanFactory().adapterForJson(json, eventTypeName);
        routeEventInternal(theEvent);
    }

    public String getURI() {
        return getRuntimeURI();
    }

    public EventSender getEventSender(String eventTypeName) throws EventTypeException {
        EventType eventType = services.getEventTypeRepositoryBus().getTypeByName(eventTypeName);
        if (eventType == null) {
            throw new EventTypeException("Event type named '" + eventTypeName + "' could not be found");
        }

        // handle built-in types
        ThreadingService threadingService = services.getThreadingService();
        if (eventType instanceof BeanEventType) {
            return new EventSenderBean(this, (BeanEventType) eventType, services.getEventBeanTypedEventFactory(), threadingService);
        }
        if (eventType instanceof MapEventType) {
            return new EventSenderMap(this, (MapEventType) eventType, services.getEventBeanTypedEventFactory(), threadingService);
        }
        if (eventType instanceof ObjectArrayEventType) {
            return new EventSenderObjectArray(this, (ObjectArrayEventType) eventType, services.getEventBeanTypedEventFactory(), threadingService);
        }
        if (eventType instanceof BaseXMLEventType) {
            return new EventSenderXMLDOM(this, (BaseXMLEventType) eventType, services.getEventBeanTypedEventFactory(), threadingService);
        }
        if (eventType instanceof AvroSchemaEventType) {
            return new EventSenderAvro(this, eventType, services.getEventBeanTypedEventFactory(), threadingService);
        }
        if (eventType instanceof JsonEventType) {
            return new EventSenderJsonImpl(this, (JsonEventType) eventType, services.getEventBeanTypedEventFactory(), threadingService);
        }

        throw new EventTypeException("An event sender for event type named '" + eventTypeName + "' could not be created as the type is not known");
    }

    public Map<DeploymentIdNamePair, Long> getStatementNearestSchedules() {
        return getStatementNearestSchedulesInternal(services.getSchedulingService(), services.getStatementLifecycleService());
    }

    public void clockInternal() {
        // Start internal clock which supplies CurrentTimeEvent events every 100ms
        // This may be done without delay thus the write lock indeed must be reentrant.
        if (services.getConfigSnapshot().getCommon().getTimeSource().getTimeUnit() != TimeUnit.MILLISECONDS) {
            throw new EPException("Internal timer requires millisecond time resolution");
        }
        services.getTimerService().startInternalClock();
        isUsingExternalClocking = false;
    }

    public void clockExternal() {
        // Stop internal clock, for unit testing and for external clocking
        services.getTimerService().stopInternalClock(true);
        isUsingExternalClocking = true;
    }

    public long getNumEventsEvaluated() {
        return services.getFilterService().getNumEventsEvaluated();
    }

    public void resetStats() {
        services.getFilterService().resetStats();
        routedInternal.set(0);
        routedExternal.set(0);
    }

    private static Map<DeploymentIdNamePair, Long> getStatementNearestSchedulesInternal(SchedulingServiceSPI schedulingService, StatementLifecycleService statementLifecycleSvc) {
        final Map<Integer, Long> schedulePerStatementId = new HashMap<>();
        schedulingService.visitSchedules(new ScheduleVisitor() {
            public void visit(ScheduleVisit visit) {
                if (schedulePerStatementId.containsKey(visit.getStatementId())) {
                    return;
                }
                schedulePerStatementId.put(visit.getStatementId(), visit.getTimestamp());
            }
        });

        Map<DeploymentIdNamePair, Long> result = new HashMap<>();
        for (Map.Entry<Integer, Long> schedule : schedulePerStatementId.entrySet()) {
            EPStatementSPI spi = statementLifecycleSvc.getStatementById(schedule.getKey());
            if (spi != null) {
                result.put(new DeploymentIdNamePair(spi.getDeploymentId(), spi.getName()), schedule.getValue());
            }
        }
        return result;
    }

    private void routeEventInternal(EventBean theEvent) {
        EPEventServiceThreadLocalEntry tlEntry = threadLocals.get();
        if (internalEventRouter.isHasPreprocessing()) {
            ExprEvaluatorContext exprEvaluatorContext = tlEntry.getExprEvaluatorContext();
            theEvent = internalEventRouter.preprocess(theEvent, exprEvaluatorContext, InstrumentationHelper.get());
            if (theEvent == null) {
                return;
            }
        }
        tlEntry.getWorkQueue().add(theEvent);
    }
}
