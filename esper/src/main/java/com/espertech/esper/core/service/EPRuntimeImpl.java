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
package com.espertech.esper.core.service;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionDescriptor;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.context.ContextPartitionVariableState;
import com.espertech.esper.client.dataflow.EPDataFlowRuntime;
import com.espertech.esper.client.hook.ExceptionHandlerExceptionType;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.client.time.TimerEvent;
import com.espertech.esper.client.util.EventRenderer;
import com.espertech.esper.collection.ArrayBackedCollection;
import com.espertech.esper.collection.DualWorkQueue;
import com.espertech.esper.collection.ThreadWorkQueue;
import com.espertech.esper.core.context.mgr.ContextManager;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandleComparator;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.core.start.*;
import com.espertech.esper.core.thread.*;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.specmapper.StatementSpecMapper;
import com.espertech.esper.epl.specmapper.StatementSpecUnMapResult;
import com.espertech.esper.epl.metric.MetricReportingPath;
import com.espertech.esper.epl.script.AgentInstanceScriptContext;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.StatementSpecRawAnalyzer;
import com.espertech.esper.epl.table.mgmt.TableExprEvaluatorContext;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.event.util.EventRendererImpl;
import com.espertech.esper.filter.FilterHandle;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.metrics.jmx.JmxGetter;
import com.espertech.esper.schedule.*;
import com.espertech.esper.timer.TimerCallback;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.MetricUtil;
import com.espertech.esper.util.ThreadLogUtil;
import com.espertech.esper.util.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements runtime interface. Also accepts timer callbacks for synchronizing time events with regular events
 * sent in.
 */
public class EPRuntimeImpl implements EPRuntimeSPI, EPRuntimeEventSender, TimerCallback, InternalEventRouteDest {
    protected static final Logger log = LoggerFactory.getLogger(EPRuntimeImpl.class);
    private static final int MAX_FILTER_FAULT_COUNT = 10;

    protected EPServicesContext services;
    protected boolean isLatchStatementInsertStream;
    protected boolean isUsingExternalClocking;
    protected boolean isPrioritized;
    protected volatile UnmatchedListener unmatchedListener;
    protected AtomicLong routedInternal;
    protected AtomicLong routedExternal;
    protected EventRenderer eventRenderer;
    protected InternalEventRouter internalEventRouter;
    protected ExprEvaluatorContext engineFilterAndDispatchTimeContext;
    protected ThreadWorkQueue threadWorkQueue;
    protected ThreadLocal<ArrayBackedCollection<FilterHandle>> matchesArrayThreadLocal;
    protected ThreadLocal<ArrayBackedCollection<ScheduleHandle>> scheduleArrayThreadLocal;
    protected ThreadLocal<Map<EPStatementAgentInstanceHandle, Object>> matchesPerStmtThreadLocal;
    protected ThreadLocal<Map<EPStatementAgentInstanceHandle, Object>> schedulePerStmtThreadLocal;

    /**
     * Constructor.
     *
     * @param services - references to services
     */
    public EPRuntimeImpl(final EPServicesContext services) {
        this.services = services;
        this.threadWorkQueue = new ThreadWorkQueue();
        isLatchStatementInsertStream = this.services.getEngineSettingsService().getEngineSettings().getThreading().isInsertIntoDispatchPreserveOrder();
        isUsingExternalClocking = !this.services.getEngineSettingsService().getEngineSettings().getThreading().isInternalTimerEnabled();
        isPrioritized = services.getEngineSettingsService().getEngineSettings().getExecution().isPrioritized();
        routedInternal = new AtomicLong();
        routedExternal = new AtomicLong();
        engineFilterAndDispatchTimeContext = new ExprEvaluatorContext() {
            private ExpressionResultCacheService expressionResultCacheService = services.getExpressionResultCacheSharable();

            public TimeProvider getTimeProvider() {
                return services.getSchedulingService();
            }

            public ExpressionResultCacheService getExpressionResultCacheService() {
                return expressionResultCacheService;
            }

            public int getAgentInstanceId() {
                return -1;
            }

            public EventBean getContextProperties() {
                return null;
            }

            public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
                return null;
            }

            public String getStatementName() {
                return null;
            }

            public String getEngineURI() {
                return null;
            }

            public int getStatementId() {
                return -1;
            }

            public StatementAgentInstanceLock getAgentInstanceLock() {
                return null;
            }

            public StatementType getStatementType() {
                return null;
            }

            public TableExprEvaluatorContext getTableExprEvaluatorContext() {
                throw new UnsupportedOperationException("Table-access evaluation is not supported in this expression");
            }

            public Object getStatementUserObject() {
                return null;
            }
        };

        initThreadLocals();

        services.getThreadingService().initThreading(services, this);
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
        long msec = services.getTimeSource().getTimeMillis();

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled() && (ExecutionPathDebugLog.isTimerDebugEnabled))) {
            log.debug(".timerCallback Evaluating scheduled callbacks, time is " + msec);
        }

        CurrentTimeEvent currentTimeEvent = new CurrentTimeEvent(msec);
        sendEvent(currentTimeEvent);
    }

    public void sendEventAvro(Object avroGenericDataDotRecord, String avroEventTypeName) {
        if (avroGenericDataDotRecord == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendMap Processing event " + avroGenericDataDotRecord.toString());
        }

        if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isInboundThreading())) {
            services.getThreadingService().submitInbound(new InboundUnitSendAvro(avroGenericDataDotRecord, avroEventTypeName, services, this));
        } else {
            // Process event
            EventBean eventBean = wrapEventAvro(avroGenericDataDotRecord, avroEventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void sendEvent(Object theEvent) throws EPException {
        if (theEvent == null) {
            log.error(".sendEvent Null object supplied");
            return;
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            if ((!(theEvent instanceof CurrentTimeEvent)) || (ExecutionPathDebugLog.isTimerDebugEnabled)) {
                log.debug(".sendEvent Processing event " + theEvent);
            }
        }

        // Process event
        if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isInboundThreading())) {
            services.getThreadingService().submitInbound(new InboundUnitSendEvent(theEvent, this));
        } else {
            processEvent(theEvent);
        }
    }

    public void sendEvent(org.w3c.dom.Node document) throws EPException {
        if (document == null) {
            log.error(".sendEvent Null object supplied");
            return;
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendEvent Processing DOM node event " + document);
        }

        // Process event
        if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isInboundThreading())) {
            services.getThreadingService().submitInbound(new InboundUnitSendDOM(document, services, this));
        } else {
            // Get it wrapped up, process event
            EventBean eventBean = wrapEvent(document);
            processEvent(eventBean);
        }
    }

    public EventBean wrapEvent(Node node) {
        return services.getEventAdapterService().adapterForDOM(node);
    }

    public void route(org.w3c.dom.Node document) throws EPException {
        if (document == null) {
            log.error(".sendEvent Null object supplied");
            return;
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendEvent Processing DOM node event " + document);
        }

        // Get it wrapped up, process event
        EventBean eventBean = services.getEventAdapterService().adapterForDOM(document);
        threadWorkQueue.addBack(eventBean);
    }

    public void routeAvro(Object avroGenericDataDotRecord, String avroEventTypeName) throws EPException {
        if (avroGenericDataDotRecord == null) {
            log.error(".sendEvent Null object supplied");
            return;
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendEvent Processing Avro event " + avroGenericDataDotRecord);
        }

        // Get it wrapped up, process event
        EventBean eventBean = services.getEventAdapterService().adapterForAvro(avroGenericDataDotRecord, avroEventTypeName);
        threadWorkQueue.addBack(eventBean);
    }

    public void sendEvent(Map map, String mapEventTypeName) throws EPException {
        if (map == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendMap Processing event " + map);
        }

        if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isInboundThreading())) {
            services.getThreadingService().submitInbound(new InboundUnitSendMap(map, mapEventTypeName, services, this));
        } else {
            // Process event
            EventBean eventBean = wrapEvent(map, mapEventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public void sendEvent(Object[] propertyValues, String objectArrayEventTypeName) throws EPException {
        if (propertyValues == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendMap Processing event " + Arrays.toString(propertyValues));
        }

        if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isInboundThreading())) {
            services.getThreadingService().submitInbound(new InboundUnitSendObjectArray(propertyValues, objectArrayEventTypeName, services, this));
        } else {
            // Process event
            EventBean eventBean = wrapEvent(propertyValues, objectArrayEventTypeName);
            processWrappedEvent(eventBean);
        }
    }

    public EventBean wrapEvent(Map map, String eventTypeName) {
        return services.getEventAdapterService().adapterForMap(map, eventTypeName);
    }

    public EventBean wrapEvent(Object[] objectArray, String eventTypeName) {
        return services.getEventAdapterService().adapterForObjectArray(objectArray, eventTypeName);
    }

    public EventBean wrapEventAvro(Object avroGenericDataDotRecord, String eventTypeName) {
        return services.getEventAdapterService().adapterForAvro(avroGenericDataDotRecord, eventTypeName);
    }

    public void route(Map map, String eventTypeName) throws EPException {
        if (map == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".route Processing event " + map);
        }

        // Process event
        EventBean theEvent = services.getEventAdapterService().adapterForMap(map, eventTypeName);
        if (internalEventRouter.isHasPreprocessing()) {
            theEvent = internalEventRouter.preprocess(theEvent, engineFilterAndDispatchTimeContext);
            if (theEvent == null) {
                return;
            }
        }
        threadWorkQueue.addBack(theEvent);
    }

    public void route(Object[] objectArray, String eventTypeName) throws EPException {
        if (objectArray == null) {
            throw new IllegalArgumentException("Invalid null event object");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".route Processing event " + Arrays.toString(objectArray));
        }

        // Process event
        EventBean theEvent = services.getEventAdapterService().adapterForObjectArray(objectArray, eventTypeName);
        if (internalEventRouter.isHasPreprocessing()) {
            theEvent = internalEventRouter.preprocess(theEvent, engineFilterAndDispatchTimeContext);
            if (theEvent == null) {
                return;
            }
        }
        threadWorkQueue.addBack(theEvent);
    }

    public long getNumEventsEvaluated() {
        return services.getFilterService().getNumEventsEvaluated();
    }

    public void resetStats() {
        services.getFilterService().resetStats();
        routedInternal.set(0);
        routedExternal.set(0);
    }

    public void routeEventBean(EventBean theEvent) {
        threadWorkQueue.addBack(theEvent);
    }

    public void route(Object theEvent) {
        routedExternal.incrementAndGet();

        if (internalEventRouter.isHasPreprocessing()) {
            EventBean eventBean = services.getEventAdapterService().adapterForBean(theEvent);
            theEvent = internalEventRouter.preprocess(eventBean, engineFilterAndDispatchTimeContext);
            if (theEvent == null) {
                return;
            }
        }

        threadWorkQueue.addBack(theEvent);
    }

    // Internal route of events via insert-into, holds a statement lock
    public void route(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRouteBetweenStmt(theEvent, epStatementHandle, addToFront);
        }
        routedInternal.incrementAndGet();

        if (isLatchStatementInsertStream) {
            if (addToFront) {
                Object latch = epStatementHandle.getInsertIntoFrontLatchFactory().newLatch(theEvent);
                threadWorkQueue.addFront(latch);
            } else {
                Object latch = epStatementHandle.getInsertIntoBackLatchFactory().newLatch(theEvent);
                threadWorkQueue.addBack(latch);
            }
        } else {
            if (addToFront) {
                threadWorkQueue.addFront(theEvent);
            } else {
                threadWorkQueue.addBack(theEvent);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRouteBetweenStmt();
        }
    }

    /**
     * Process an unwrapped event.
     *
     * @param theEvent to process.
     */
    public void processEvent(Object theEvent) {
        if (theEvent instanceof TimerEvent) {
            processTimeEvent((TimerEvent) theEvent);
            return;
        }

        EventBean eventBean;

        if (theEvent instanceof EventBean) {
            eventBean = (EventBean) theEvent;
        } else {
            eventBean = wrapEvent(theEvent);
        }

        processWrappedEvent(eventBean);
    }

    public EventBean wrapEvent(Object theEvent) {
        return services.getEventAdapterService().adapterForBean(theEvent);
    }

    public void processWrappedEvent(EventBean eventBean) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qStimulantEvent(eventBean, services.getEngineURI());
        }

        if (internalEventRouter.isHasPreprocessing()) {
            eventBean = internalEventRouter.preprocess(eventBean, engineFilterAndDispatchTimeContext);
            if (eventBean == null) {
                return;
            }
        }

        // Acquire main processing lock which locks out statement management
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getEngineURI(), true);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            matchesArrayThreadLocal.get().clear();
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

    private void processTimeEvent(TimerEvent theEvent) {
        if (theEvent instanceof TimerControlEvent) {
            TimerControlEvent timerControlEvent = (TimerControlEvent) theEvent;
            if (timerControlEvent.getClockType() == TimerControlEvent.ClockType.CLOCK_INTERNAL) {
                // Start internal clock which supplies CurrentTimeEvent events every 100ms
                // This may be done without delay thus the write lock indeed must be reentrant.
                if (services.getConfigSnapshot().getEngineDefaults().getTimeSource().getTimeUnit() != TimeUnit.MILLISECONDS) {
                    throw new EPException("Internal timer requires millisecond time resolution");
                }
                services.getTimerService().startInternalClock();
                isUsingExternalClocking = false;
            } else {
                // Stop internal clock, for unit testing and for external clocking
                services.getTimerService().stopInternalClock(true);
                isUsingExternalClocking = true;
            }

            return;
        }

        if (theEvent instanceof CurrentTimeEvent) {

            CurrentTimeEvent current = (CurrentTimeEvent) theEvent;
            long currentTime = current.getTime();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qStimulantTime(currentTime, services.getEngineURI());
            }

            // Evaluation of all time events is protected from statement management
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) && (ExecutionPathDebugLog.isTimerDebugEnabled)) {
                log.debug(".processTimeEvent Setting time and evaluating schedules for time " + currentTime);
            }

            if (isUsingExternalClocking && (currentTime == services.getSchedulingService().getTime())) {
                if (log.isWarnEnabled()) {
                    log.warn("Duplicate time event received for currentTime " + currentTime);
                }
            }
            services.getSchedulingService().setTime(currentTime);

            if (MetricReportingPath.isMetricsEnabled) {
                services.getMetricsReportingService().processTimeEvent(currentTime);
            }

            processSchedule(currentTime);

            // Let listeners know of results
            dispatch();

            // Work off the event queue if any events accumulated in there via a route()
            processThreadWorkQueue();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aStimulantTime();
            }

            return;
        }

        // handle time span
        CurrentTimeSpanEvent span = (CurrentTimeSpanEvent) theEvent;
        long targetTime = span.getTargetTime();
        long currentTime = services.getSchedulingService().getTime();
        Long optionalResolution = span.getOptionalResolution();

        if (isUsingExternalClocking && (targetTime < currentTime)) {
            if (log.isWarnEnabled()) {
                log.warn("Past or current time event received for currentTime " + targetTime);
            }
        }

        // Evaluation of all time events is protected from statement management
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) && (ExecutionPathDebugLog.isTimerDebugEnabled)) {
            log.debug(".processTimeEvent Setting time span and evaluating schedules for time " + targetTime + " optional resolution " + span.getOptionalResolution());
        }

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
                InstrumentationHelper.get().qStimulantTime(currentTime, services.getEngineURI());
            }

            // Evaluation of all time events is protected from statement management
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) && (ExecutionPathDebugLog.isTimerDebugEnabled)) {
                log.debug(".processTimeEvent Setting time and evaluating schedules for time " + currentTime);
            }

            services.getSchedulingService().setTime(currentTime);

            if (MetricReportingPath.isMetricsEnabled) {
                services.getMetricsReportingService().processTimeEvent(currentTime);
            }

            processSchedule(currentTime);

            // Let listeners know of results
            dispatch();

            // Work off the event queue if any events accumulated in there via a route()
            processThreadWorkQueue();

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aStimulantTime();
            }
        }
    }

    private void processSchedule(long time) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTime(time, services.getEngineURI());
        }
        ArrayBackedCollection<ScheduleHandle> handles = scheduleArrayThreadLocal.get();

        // Evaluation of schedules is protected by an optional scheduling service lock and then the engine lock
        // We want to stay in this order for allowing the engine lock as a second-order lock to the
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
            EPStatementHandleCallback handle = (EPStatementHandleCallback) handleArray[0];

            if ((MetricReportingPath.isMetricsEnabled) && (handle.getAgentInstanceHandle().getStatementHandle().getMetricsHandle().isEnabled())) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementScheduleSingle(handle, services);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                services.getMetricsReportingService().accountTime(handle.getAgentInstanceHandle().getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
            } else {
                if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isTimerThreading())) {
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
        Map<EPStatementAgentInstanceHandle, Object> stmtCallbacks = schedulePerStmtThreadLocal.get();
        stmtCallbacks.clear();
        for (int i = 0; i < entryCount; i++) {
            EPStatementHandleCallback handleCallback = (EPStatementHandleCallback) matchArray[i];
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

            if ((MetricReportingPath.isMetricsEnabled) && (handle.getStatementHandle().getMetricsHandle().isEnabled())) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementScheduleMultiple(handle, callbackObject, services);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                int numInput = (callbackObject instanceof Collection) ? ((Collection) callbackObject).size() : 1;
                services.getMetricsReportingService().accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, numInput);
            } else {
                if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isTimerThreading())) {
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

    /**
     * Works off the thread's work queue.
     */
    public void processThreadWorkQueue() {
        DualWorkQueue queues = threadWorkQueue.getThreadQueue();

        if (queues.getFrontQueue().isEmpty()) {
            boolean haveDispatched = services.getNamedWindowDispatchService().dispatch();
            if (haveDispatched) {
                // Dispatch results to listeners
                dispatch();

                if (!queues.getFrontQueue().isEmpty()) {
                    processThreadWorkQueueFront(queues);
                }
            }
        } else {
            processThreadWorkQueueFront(queues);
        }

        Object item;
        while ((item = queues.getBackQueue().poll()) != null) {
            if (item instanceof InsertIntoLatchSpin) {
                processThreadWorkQueueLatchedSpin((InsertIntoLatchSpin) item);
            } else if (item instanceof InsertIntoLatchWait) {
                processThreadWorkQueueLatchedWait((InsertIntoLatchWait) item);
            } else {
                processThreadWorkQueueUnlatched(item);
            }

            boolean haveDispatched = services.getNamedWindowDispatchService().dispatch();
            if (haveDispatched) {
                dispatch();
            }

            if (!queues.getFrontQueue().isEmpty()) {
                processThreadWorkQueueFront(queues);
            }
        }
    }

    private void processThreadWorkQueueFront(DualWorkQueue queues) {
        Object item;
        while ((item = queues.getFrontQueue().poll()) != null) {
            if (item instanceof InsertIntoLatchSpin) {
                processThreadWorkQueueLatchedSpin((InsertIntoLatchSpin) item);
            } else if (item instanceof InsertIntoLatchWait) {
                processThreadWorkQueueLatchedWait((InsertIntoLatchWait) item);
            } else {
                processThreadWorkQueueUnlatched(item);
            }

            boolean haveDispatched = services.getNamedWindowDispatchService().dispatch();
            if (haveDispatched) {
                dispatch();
            }
        }
    }

    private void processThreadWorkQueueLatchedWait(InsertIntoLatchWait insertIntoLatch) {
        // wait for the latch to complete
        EventBean eventBean = insertIntoLatch.await();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getEngineURI(), false);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            matchesArrayThreadLocal.get().clear();
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

    private void processThreadWorkQueueLatchedSpin(InsertIntoLatchSpin insertIntoLatch) {
        // wait for the latch to complete
        EventBean eventBean = insertIntoLatch.await();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getEngineURI(), false);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            matchesArrayThreadLocal.get().clear();
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

    private void processThreadWorkQueueUnlatched(Object item) {
        EventBean eventBean;
        if (item instanceof EventBean) {
            eventBean = (EventBean) item;
        } else {
            eventBean = services.getEventAdapterService().adapterForBean(item);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEvent(eventBean, services.getEngineURI(), false);
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            processMatches(eventBean);
        } catch (RuntimeException ex) {
            matchesArrayThreadLocal.get().clear();
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
        ArrayBackedCollection<FilterHandle> matches = matchesArrayThreadLocal.get();
        long version = services.getFilterService().evaluate(theEvent, matches);

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

        Map<EPStatementAgentInstanceHandle, Object> stmtCallbacks = matchesPerStmtThreadLocal.get();
        Object[] matchArray = matches.getArray();
        int entryCount = matches.size();

        for (int i = 0; i < entryCount; i++) {
            EPStatementHandleCallback handleCallback = (EPStatementHandleCallback) matchArray[i];
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
                    ArrayDeque<FilterHandleCallback> q = new ArrayDeque<FilterHandleCallback>(4);
                    q.add((FilterHandleCallback) callbacks);
                    q.add(handleCallback.getFilterCallback());
                    stmtCallbacks.put(handle, q);
                }
                continue;
            }

            if ((MetricReportingPath.isMetricsEnabled) && (handle.getStatementHandle().getMetricsHandle().isEnabled())) {
                long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                long wallTimeBefore = MetricUtil.getWall();

                processStatementFilterSingle(handle, handleCallback, theEvent, version, 0);

                long wallTimeAfter = MetricUtil.getWall();
                long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                long deltaWall = wallTimeAfter - wallTimeBefore;
                services.getMetricsReportingService().accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
            } else {
                if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isRouteThreading())) {
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

            if ((MetricReportingPath.isMetricsEnabled) && (handle.getStatementHandle().getMetricsHandle().isEnabled())) {
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
                services.getMetricsReportingService().accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, size);
            } else {
                if ((ThreadingOption.isThreadingEnabled) && (services.getThreadingService().isRouteThreading())) {
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
     * Processing multiple schedule matches for a statement.
     *
     * @param handle         statement handle
     * @param callbackObject object containing matches
     * @param services       engine services
     */
    public static void processStatementScheduleMultiple(EPStatementAgentInstanceHandle handle, Object callbackObject, EPServicesContext services) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTimeCP(handle, services.getSchedulingService().getTime());
        }
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try {
            if (!handle.isDestroyed()) {
                if (handle.isHasVariables()) {
                    services.getVariableService().setLocalVersion();
                }

                if (callbackObject instanceof ArrayDeque) {
                    ArrayDeque<ScheduleHandleCallback> callbackList = (ArrayDeque<ScheduleHandleCallback>) callbackObject;
                    for (ScheduleHandleCallback callback : callbackList) {
                        callback.scheduledTrigger(services.getEngineLevelExtensionServicesContext());
                    }
                } else {
                    ScheduleHandleCallback callback = (ScheduleHandleCallback) callbackObject;
                    callback.scheduledTrigger(services.getEngineLevelExtensionServicesContext());
                }

                // internal join processing, if applicable
                handle.internalDispatch();
            }
        } catch (RuntimeException ex) {
            services.getExceptionHandlingService().handleException(ex, handle, ExceptionHandlerExceptionType.PROCESS, null);
        } finally {
            if (handle.isHasTableAccess()) {
                services.getTableService().getTableExprEvaluatorContext().releaseAcquiredLocks();
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
     * @param services engine services
     */
    public static void processStatementScheduleSingle(EPStatementHandleCallback handle, EPServicesContext services) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTimeCP(handle.getAgentInstanceHandle(), services.getSchedulingService().getTime());
        }
        StatementAgentInstanceLock statementLock = handle.getAgentInstanceHandle().getStatementAgentInstanceLock();
        statementLock.acquireWriteLock();
        try {
            if (!handle.getAgentInstanceHandle().isDestroyed()) {
                if (handle.getAgentInstanceHandle().isHasVariables()) {
                    services.getVariableService().setLocalVersion();
                }

                handle.getScheduleCallback().scheduledTrigger(services.getEngineLevelExtensionServicesContext());
                handle.getAgentInstanceHandle().internalDispatch();
            }
        } catch (RuntimeException ex) {
            services.getExceptionHandlingService().handleException(ex, handle.getAgentInstanceHandle(), ExceptionHandlerExceptionType.PROCESS, null);
        } finally {
            if (handle.getAgentInstanceHandle().isHasTableAccess()) {
                services.getTableService().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getAgentInstanceHandle().getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aTimeCP();
            }
        }
    }

    /**
     * Processing multiple filter matches for a statement.
     *  @param handle       statement handle
     * @param callbackList object containing callbacks
     * @param theEvent     to process
     * @param version      filter version
     * @param filterFaultCount filter fault count
     */
    public void processStatementFilterMultiple(EPStatementAgentInstanceHandle handle, Object callbackList, EventBean theEvent, long version, int filterFaultCount) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEventCP(theEvent, handle, services.getSchedulingService().getTime());
        }
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try {
            if (handle.isHasVariables()) {
                services.getVariableService().setLocalVersion();
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
                services.getTableService().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEventCP();
            }
        }
    }

    /**
     * Process a single match.
     *  @param handle         statement
     * @param handleCallback callback
     * @param theEvent       event to indicate
     * @param version        filter version
     * @param filterFaultCount filter fault count
     */
    public void processStatementFilterSingle(EPStatementAgentInstanceHandle handle, EPStatementHandleCallback handleCallback, EventBean theEvent, long version, int filterFaultCount) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qEventCP(theEvent, handle, services.getSchedulingService().getTime());
        }
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try {
            if (handle.isHasVariables()) {
                services.getVariableService().setLocalVersion();
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
                services.getTableService().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handleCallback.getAgentInstanceHandle().getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aEventCP();
            }
        }
    }

    protected void handleFilterFault(EPStatementAgentInstanceHandle faultingHandle, EventBean theEvent, int filterFaultCount) {
        ArrayDeque<FilterHandle> callbacksForStatement = new ArrayDeque<FilterHandle>();
        long version = services.getFilterService().evaluate(theEvent, callbacksForStatement, faultingHandle.getStatementId());

        if (callbacksForStatement.size() == 1) {
            EPStatementHandleCallback handleCallback = (EPStatementHandleCallback) callbacksForStatement.getFirst();
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
            EPStatementHandleCallback handleCallback = (EPStatementHandleCallback) filterHandle;
            EPStatementAgentInstanceHandle handle = handleCallback.getAgentInstanceHandle();

            if (handle.isCanSelfJoin() || isPrioritized) {
                Object callbacks = stmtCallbacks.get(handle);
                if (callbacks == null) {
                    stmtCallbacks.put(handle, handleCallback.getFilterCallback());
                } else if (callbacks instanceof ArrayDeque) {
                    ArrayDeque<FilterHandleCallback> q = (ArrayDeque<FilterHandleCallback>) callbacks;
                    q.add(handleCallback.getFilterCallback());
                } else {
                    ArrayDeque<FilterHandleCallback> q = new ArrayDeque<FilterHandleCallback>(4);
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
     * Destroy for destroying an engine instance: sets references to null and clears thread-locals
     */
    public void destroy() {
        services = null;

        removeFromThreadLocals();
        matchesArrayThreadLocal = null;
        matchesPerStmtThreadLocal = null;
        scheduleArrayThreadLocal = null;
        schedulePerStmtThreadLocal = null;
    }

    public void initialize() {
        initThreadLocals();
        threadWorkQueue = new ThreadWorkQueue();
    }

    public void clearCaches() {
        initThreadLocals();
    }

    public void setUnmatchedListener(UnmatchedListener listener) {
        this.unmatchedListener = listener;
    }

    public void setVariableValue(String variableName, Object variableValue) throws EPException {
        VariableMetaData metaData = services.getVariableService().getVariableMetaData(variableName);
        checkVariable(variableName, metaData, true, false);

        services.getVariableService().getReadWriteLock().writeLock().lock();
        try {
            services.getVariableService().checkAndWrite(variableName, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, variableValue);
            services.getVariableService().commit();
        } finally {
            services.getVariableService().getReadWriteLock().writeLock().unlock();
        }
    }

    public void setVariableValue(Map<String, Object> variableValues) throws EPException {
        setVariableValueInternal(variableValues, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, false);
    }

    public void setVariableValue(Map<String, Object> variableValues, int agentInstanceId) throws VariableValueException, VariableNotFoundException {
        setVariableValueInternal(variableValues, agentInstanceId, true);
    }

    public Object getVariableValue(String variableName) throws EPException {
        services.getVariableService().setLocalVersion();
        VariableMetaData metaData = services.getVariableService().getVariableMetaData(variableName);
        if (metaData == null) {
            throw new VariableNotFoundException("Variable by name '" + variableName + "' has not been declared");
        }
        if (metaData.getContextPartitionName() != null) {
            throw new VariableNotFoundException("Variable by name '" + variableName + "' has been declared for context '" + metaData.getContextPartitionName() + "' and cannot be read without context partition selector");
        }
        VariableReader reader = services.getVariableService().getReader(variableName, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
        Object value = reader.getValue();
        if (value == null || reader.getVariableMetaData().getEventType() == null) {
            return value;
        }
        return ((EventBean) value).getUnderlying();
    }

    public Map<String, List<ContextPartitionVariableState>> getVariableValue(Set<String> variableNames, ContextPartitionSelector contextPartitionSelector) throws VariableNotFoundException {
        services.getVariableService().setLocalVersion();
        String contextPartitionName = null;
        for (String variableName : variableNames) {
            VariableMetaData metaData = services.getVariableService().getVariableMetaData(variableName);
            if (metaData == null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' has not been declared");
            }
            if (metaData.getContextPartitionName() == null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' is a global variable and not context-partitioned");
            }
            if (contextPartitionName == null) {
                contextPartitionName = metaData.getContextPartitionName();
            } else {
                if (!contextPartitionName.equals(metaData.getContextPartitionName())) {
                    throw new VariableNotFoundException("Variable by name '" + variableName + "' is a declared for context '" + metaData.getContextPartitionName() + "' however the expected context is '" + contextPartitionName + "'");
                }
            }
        }
        ContextManager contextManager = services.getContextManagementService().getContextManager(contextPartitionName);
        if (contextManager == null) {
            throw new VariableNotFoundException("Context by name '" + contextPartitionName + "' cannot be found");
        }
        Map<Integer, ContextPartitionDescriptor> contextPartitions = contextManager.extractPaths(contextPartitionSelector).getContextPartitionInformation();
        if (contextPartitions.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<ContextPartitionVariableState>> statesMap = new HashMap<String, List<ContextPartitionVariableState>>();
        for (String variableName : variableNames) {
            List<ContextPartitionVariableState> states = new ArrayList<ContextPartitionVariableState>();
            statesMap.put(variableName, states);
            for (Map.Entry<Integer, ContextPartitionDescriptor> entry : contextPartitions.entrySet()) {
                VariableReader reader = services.getVariableService().getReader(variableName, entry.getKey());
                Object value = reader.getValue();
                if (value != null && reader.getVariableMetaData().getEventType() != null) {
                    value = ((EventBean) value).getUnderlying();
                }
                states.add(new ContextPartitionVariableState(entry.getKey(), entry.getValue().getIdentifier(), value));
            }
        }
        return statesMap;
    }

    public Map<String, Object> getVariableValue(Set<String> variableNames) throws EPException {
        services.getVariableService().setLocalVersion();
        Map<String, Object> values = new HashMap<String, Object>();
        for (String variableName : variableNames) {
            VariableMetaData metaData = services.getVariableService().getVariableMetaData(variableName);
            checkVariable(variableName, metaData, false, false);
            VariableReader reader = services.getVariableService().getReader(variableName, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
            if (reader == null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' has not been declared");
            }

            Object value = reader.getValue();
            if (value != null && reader.getVariableMetaData().getEventType() != null) {
                value = ((EventBean) value).getUnderlying();
            }
            values.put(variableName, value);
        }
        return values;
    }

    public Map<String, Object> getVariableValueAll() throws EPException {
        services.getVariableService().setLocalVersion();
        Map<String, VariableReader> variables = services.getVariableService().getVariableReadersNonCP();
        Map<String, Object> values = new HashMap<String, Object>();
        for (Map.Entry<String, VariableReader> entry : variables.entrySet()) {
            Object value = entry.getValue().getValue();
            values.put(entry.getValue().getVariableMetaData().getVariableName(), value);
        }
        return values;
    }

    public Map<String, Class> getVariableTypeAll() {
        Map<String, VariableReader> variables = services.getVariableService().getVariableReadersNonCP();
        Map<String, Class> values = new HashMap<String, Class>();
        for (Map.Entry<String, VariableReader> entry : variables.entrySet()) {
            Class type = entry.getValue().getVariableMetaData().getType();
            values.put(entry.getValue().getVariableMetaData().getVariableName(), type);
        }
        return values;
    }

    public Class getVariableType(String variableName) {
        VariableMetaData metaData = services.getVariableService().getVariableMetaData(variableName);
        if (metaData == null) {
            return null;
        }
        return metaData.getType();
    }

    public EPOnDemandQueryResult executeQuery(String epl, ContextPartitionSelector[] contextPartitionSelectors) {
        if (contextPartitionSelectors == null) {
            throw new IllegalArgumentException("No context partition selectors provided");
        }
        return executeQueryInternal(epl, null, null, contextPartitionSelectors);
    }

    public EPOnDemandQueryResult executeQuery(String epl) {
        return executeQueryInternal(epl, null, null, null);
    }

    public EPOnDemandQueryResult executeQuery(EPStatementObjectModel model) {
        return executeQueryInternal(null, model, null, null);
    }

    public EPOnDemandQueryResult executeQuery(EPStatementObjectModel model, ContextPartitionSelector[] contextPartitionSelectors) {
        if (contextPartitionSelectors == null) {
            throw new IllegalArgumentException("No context partition selectors provided");
        }
        return executeQueryInternal(null, model, null, contextPartitionSelectors);
    }

    public EPOnDemandQueryResult executeQuery(EPOnDemandPreparedQueryParameterized parameterizedQuery) {
        return executeQueryInternal(null, null, parameterizedQuery, null);
    }

    public EPOnDemandQueryResult executeQuery(EPOnDemandPreparedQueryParameterized parameterizedQuery, ContextPartitionSelector[] contextPartitionSelectors) {
        return executeQueryInternal(null, null, parameterizedQuery, contextPartitionSelectors);
    }

    private EPOnDemandQueryResult executeQueryInternal(String epl, EPStatementObjectModel model, EPOnDemandPreparedQueryParameterized parameterizedQuery, ContextPartitionSelector[] contextPartitionSelectors) {
        try {
            EPPreparedExecuteMethod executeMethod = getExecuteMethod(epl, model, parameterizedQuery);
            EPPreparedQueryResult result = executeMethod.execute(contextPartitionSelectors);
            return new EPQueryResultImpl(result);
        } catch (EPStatementException ex) {
            throw ex;
        } catch (Throwable t) {
            String message = "Error executing statement: " + t.getMessage();
            log.info(message, t);
            throw new EPStatementException(message, t, epl);
        }
    }

    public EPOnDemandPreparedQuery prepareQuery(String epl) {
        return prepareQueryInternal(epl, null);
    }

    public EPOnDemandPreparedQuery prepareQuery(EPStatementObjectModel model) {
        return prepareQueryInternal(null, model);
    }

    public EPOnDemandPreparedQueryParameterized prepareQueryWithParameters(String epl) {
        // compile to specification
        String stmtName = UuidGenerator.generate();
        StatementSpecRaw statementSpec = EPAdministratorHelper.compileEPL(epl, epl, true, stmtName, services, SelectClauseStreamSelectorEnum.ISTREAM_ONLY);

        // map to object model thus finding all substitution parameters and their indexes
        StatementSpecUnMapResult unmapped = StatementSpecMapper.unmap(statementSpec);

        // the prepared statement is the object model plus a list of substitution parameters
        // map to specification will refuse any substitution parameters that are unfilled
        return new EPPreparedStatementImpl(unmapped.getObjectModel(), unmapped.getSubstitutionParams(), epl);
    }

    private EPOnDemandPreparedQuery prepareQueryInternal(String epl, EPStatementObjectModel model) {
        try {
            EPPreparedExecuteMethod startMethod = getExecuteMethod(epl, model, null);
            return new EPPreparedQueryImpl(startMethod, epl);
        } catch (EPStatementException ex) {
            throw ex;
        } catch (Throwable t) {
            String message = "Error executing statement: " + t.getMessage();
            log.debug(message, t);
            throw new EPStatementException(message, epl);
        }
    }

    private EPPreparedExecuteMethod getExecuteMethod(String epl, EPStatementObjectModel model, EPOnDemandPreparedQueryParameterized parameterizedQuery) {
        String stmtName = UuidGenerator.generate();
        int stmtId = -1;

        try {
            StatementSpecRaw spec;
            if (epl != null) {
                spec = EPAdministratorHelper.compileEPL(epl, epl, true, stmtName, services, SelectClauseStreamSelectorEnum.ISTREAM_ONLY);
            } else if (model != null) {
                spec = StatementSpecMapper.map(model, services.getEngineImportService(), services.getVariableService(), services.getConfigSnapshot(), services.getEngineURI(), services.getPatternNodeFactory(), services.getNamedWindowMgmtService(), services.getContextManagementService(), services.getExprDeclaredService(), services.getTableService());
                epl = model.toEPL();
            } else {
                EPPreparedStatementImpl prepared = (EPPreparedStatementImpl) parameterizedQuery;
                spec = StatementSpecMapper.map(prepared.getModel(), services.getEngineImportService(), services.getVariableService(), services.getConfigSnapshot(), services.getEngineURI(), services.getPatternNodeFactory(), services.getNamedWindowMgmtService(), services.getContextManagementService(), services.getExprDeclaredService(), services.getTableService());
                epl = prepared.getOptionalEPL();
                if (epl == null) {
                    epl = prepared.getModel().toEPL();
                }
            }
            Annotation[] annotations = AnnotationUtil.compileAnnotations(spec.getAnnotations(), services.getEngineImportService(), epl);
            boolean writesToTables = StatementLifecycleSvcUtil.isWritesToTables(spec, services.getTableService());
            StatementContext statementContext = services.getStatementContextFactory().makeContext(stmtId, stmtName, epl, StatementType.SELECT, services, null, true, annotations, null, true, spec, Collections.<ExprSubselectNode>emptyList(), writesToTables, null);

            // walk subselects, alias expressions, declared expressions, dot-expressions
            ExprNodeSubselectDeclaredDotVisitor visitor;
            try {
                visitor = StatementSpecRawAnalyzer.walkSubselectAndDeclaredDotExpr(spec);
            } catch (ExprValidationException ex) {
                throw new EPStatementException(ex.getMessage(), epl);
            }

            StatementSpecCompiled compiledSpec = StatementLifecycleSvcImpl.compile(spec, epl, statementContext, false, true, annotations, visitor.getSubselects(), Collections.<ExprDeclaredNode>emptyList(), spec.getTableExpressions(), services);
            if (compiledSpec.getInsertIntoDesc() != null) {
                return new EPPreparedExecuteIUDInsertInto(compiledSpec, services, statementContext);
            } else if (compiledSpec.getFireAndForgetSpec() == null) {   // null indicates a select-statement, same as continuous query
                if (compiledSpec.getUpdateSpec() != null) {
                    throw new EPStatementException("Provided EPL expression is a continuous query expression (not an on-demand query), please use the administrator createEPL API instead", epl);
                }
                return new EPPreparedExecuteMethodQuery(compiledSpec, services, statementContext);
            } else if (compiledSpec.getFireAndForgetSpec() instanceof FireAndForgetSpecDelete) {
                return new EPPreparedExecuteIUDSingleStreamDelete(compiledSpec, services, statementContext);
            } else if (compiledSpec.getFireAndForgetSpec() instanceof FireAndForgetSpecUpdate) {
                return new EPPreparedExecuteIUDSingleStreamUpdate(compiledSpec, services, statementContext);
            } else {
                throw new IllegalStateException("Unrecognized FAF code " + compiledSpec.getFireAndForgetSpec());
            }
        } catch (EPStatementException ex) {
            throw ex;
        } catch (Throwable t) {
            String message = "Error executing statement: " + t.getMessage();
            log.debug(message, t);
            throw new EPStatementException(message, t, epl);
        }
    }

    public EventSender getEventSender(String eventTypeName) {
        return services.getEventAdapterService().getStaticTypeEventSender(this, eventTypeName, services.getThreadingService());
    }

    public EventSender getEventSender(URI[] uri) throws EventTypeException {
        return services.getEventAdapterService().getDynamicTypeEventSender(this, uri, services.getThreadingService());
    }

    public EventRenderer getEventRenderer() {
        if (eventRenderer == null) {
            eventRenderer = new EventRendererImpl();
        }
        return eventRenderer;
    }

    public long getCurrentTime() {
        return services.getSchedulingService().getTime();
    }

    public Long getNextScheduledTime() {
        return services.getSchedulingService().getNearestTimeHandle();
    }

    public Map<String, Long> getStatementNearestSchedules() {
        return getStatementNearestSchedulesInternal(services.getSchedulingService(), services.getStatementLifecycleSvc());
    }

    protected static Map<String, Long> getStatementNearestSchedulesInternal(SchedulingServiceSPI schedulingService, StatementLifecycleSvc statementLifecycleSvc) {
        final Map<Integer, Long> schedulePerStatementId = new HashMap<Integer, Long>();
        schedulingService.visitSchedules(new ScheduleVisitor() {
            public void visit(ScheduleVisit visit) {
                if (schedulePerStatementId.containsKey(visit.getStatementId())) {
                    return;
                }
                schedulePerStatementId.put(visit.getStatementId(), visit.getTimestamp());
            }
        });

        Map<String, Long> result = new HashMap<String, Long>();
        for (Map.Entry<Integer, Long> schedule : schedulePerStatementId.entrySet()) {
            String stmtName = statementLifecycleSvc.getStatementNameById(schedule.getKey());
            if (stmtName != null) {
                result.put(stmtName, schedule.getValue());
            }
        }
        return result;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return services.getExceptionHandlingService();
    }

    public String getEngineURI() {
        return services.getEngineURI();
    }

    public EPDataFlowRuntime getDataFlowRuntime() {
        return services.getDataFlowService();
    }

    private void removeFromThreadLocals() {
        if (matchesArrayThreadLocal != null) {
            matchesArrayThreadLocal.remove();
        }
        if (matchesPerStmtThreadLocal != null) {
            matchesPerStmtThreadLocal.remove();
        }
        if (scheduleArrayThreadLocal != null) {
            scheduleArrayThreadLocal.remove();
        }
        if (schedulePerStmtThreadLocal != null) {
            schedulePerStmtThreadLocal.remove();
        }
    }

    private void initThreadLocals() {
        removeFromThreadLocals();

        matchesArrayThreadLocal = new ThreadLocal<ArrayBackedCollection<FilterHandle>>() {
            protected synchronized ArrayBackedCollection<FilterHandle> initialValue() {
                return new ArrayBackedCollection<FilterHandle>(100);
            }
        };

        scheduleArrayThreadLocal = new ThreadLocal<ArrayBackedCollection<ScheduleHandle>>() {
            protected synchronized ArrayBackedCollection<ScheduleHandle> initialValue() {
                return new ArrayBackedCollection<ScheduleHandle>(100);
            }
        };

        matchesPerStmtThreadLocal =
            new ThreadLocal<Map<EPStatementAgentInstanceHandle, Object>>() {
                protected synchronized Map<EPStatementAgentInstanceHandle, Object> initialValue() {
                    if (isPrioritized) {
                        return new TreeMap<EPStatementAgentInstanceHandle, Object>(EPStatementAgentInstanceHandleComparator.INSTANCE);
                    } else {
                        return new HashMap<EPStatementAgentInstanceHandle, Object>();
                    }
                }
            };

        schedulePerStmtThreadLocal = new ThreadLocal<Map<EPStatementAgentInstanceHandle, Object>>() {
            protected synchronized Map<EPStatementAgentInstanceHandle, Object> initialValue() {
                if (isPrioritized) {
                    return new TreeMap<EPStatementAgentInstanceHandle, Object>(EPStatementAgentInstanceHandleComparator.INSTANCE);
                } else {
                    return new HashMap<EPStatementAgentInstanceHandle, Object>();
                }
            }
        };
    }

    private void checkVariable(String variableName, VariableMetaData metaData, boolean settable, boolean requireContextPartitioned) {
        if (metaData == null) {
            throw new VariableNotFoundException("Variable by name '" + variableName + "' has not been declared");
        }
        if (!requireContextPartitioned) {
            if (metaData.getContextPartitionName() != null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' has been declared for context '" + metaData.getContextPartitionName() + "' and cannot be set without context partition selectors");
            }
        } else {
            if (metaData.getContextPartitionName() == null) {
                throw new VariableNotFoundException("Variable by name '" + variableName + "' is a global variable and not context-partitioned");
            }
        }
        if (settable && metaData.isConstant()) {
            throw new VariableConstantValueException("Variable by name '" + variableName + "' is declared as constant and may not be assigned a new value");
        }
    }

    private void setVariableValueInternal(Map<String, Object> variableValues, int agentInstanceId, boolean requireContextPartitioned) throws EPException {
        // verify
        for (Map.Entry<String, Object> entry : variableValues.entrySet()) {
            String variableName = entry.getKey();
            VariableMetaData metaData = services.getVariableService().getVariableMetaData(variableName);
            checkVariable(variableName, metaData, true, requireContextPartitioned);
        }

        // set values
        services.getVariableService().getReadWriteLock().writeLock().lock();
        try {
            for (Map.Entry<String, Object> entry : variableValues.entrySet()) {
                String variableName = entry.getKey();
                try {
                    services.getVariableService().checkAndWrite(variableName, agentInstanceId, entry.getValue());
                } catch (RuntimeException ex) {
                    services.getVariableService().rollback();
                    throw ex;
                }
            }
            services.getVariableService().commit();
        } finally {
            services.getVariableService().getReadWriteLock().writeLock().unlock();
        }
    }
}
