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
package com.espertech.esper.runtime.internal.kernel.thread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.runtime.internal.kernel.service.EPEventServiceImpl;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Implementation for runtime-level threading.
 */
public class ThreadingServiceImpl implements ThreadingService {
    private static final Logger log = LoggerFactory.getLogger(ThreadingServiceImpl.class);

    private final ConfigurationRuntimeThreading config;
    private final boolean isTimerThreading;
    private final boolean isInboundThreading;
    private final boolean isRouteThreading;
    private final boolean isOutboundThreading;

    private BlockingQueue<Runnable> timerQueue;
    private BlockingQueue<Runnable> inboundQueue;
    private BlockingQueue<Runnable> routeQueue;
    private BlockingQueue<Runnable> outboundQueue;

    private ThreadPoolExecutor timerThreadPool;
    private ThreadPoolExecutor inboundThreadPool;
    private ThreadPoolExecutor routeThreadPool;
    private ThreadPoolExecutor outboundThreadPool;

    private EPServicesContext servicesContext;

    /**
     * Ctor.
     *
     * @param threadingConfig configuration
     */
    public ThreadingServiceImpl(ConfigurationRuntimeThreading threadingConfig) {
        this.config = threadingConfig;
        isTimerThreading = threadingConfig.isThreadPoolTimerExec();
        isInboundThreading = threadingConfig.isThreadPoolInbound();
        isRouteThreading = threadingConfig.isThreadPoolRouteExec();
        isOutboundThreading = threadingConfig.isThreadPoolOutbound();
    }

    public boolean isRouteThreading() {
        return isRouteThreading;
    }

    public boolean isInboundThreading() {
        return isInboundThreading;
    }

    public boolean isTimerThreading() {
        return isTimerThreading;
    }

    public boolean isOutboundThreading() {
        return isOutboundThreading;
    }

    public void initThreading(EPServicesContext services, EPEventServiceImpl runtime) {
        this.servicesContext = services;
        if (isInboundThreading) {
            inboundQueue = makeQueue(config.getThreadPoolInboundCapacity());
            inboundThreadPool = getThreadPool(services.getRuntimeURI(), "Inbound", inboundQueue, config.getThreadPoolInboundNumThreads());
        }

        if (isTimerThreading) {
            timerQueue = makeQueue(config.getThreadPoolTimerExecCapacity());
            timerThreadPool = getThreadPool(services.getRuntimeURI(), "TimerExec", timerQueue, config.getThreadPoolTimerExecNumThreads());
        }

        if (isRouteThreading) {
            routeQueue = makeQueue(config.getThreadPoolRouteExecCapacity());
            routeThreadPool = getThreadPool(services.getRuntimeURI(), "RouteExec", routeQueue, config.getThreadPoolRouteExecNumThreads());
        }

        if (isOutboundThreading) {
            outboundQueue = makeQueue(config.getThreadPoolOutboundCapacity());
            outboundThreadPool = getThreadPool(services.getRuntimeURI(), "Outbound", outboundQueue, config.getThreadPoolOutboundNumThreads());
        }
    }

    private BlockingQueue<Runnable> makeQueue(Integer threadPoolTimerExecCapacity) {
        if ((threadPoolTimerExecCapacity == null) ||
                (threadPoolTimerExecCapacity <= 0) ||
                (threadPoolTimerExecCapacity == Integer.MAX_VALUE)) {
            return new LinkedBlockingQueue<Runnable>();
        } else {
            return new ArrayBlockingQueue<Runnable>(threadPoolTimerExecCapacity);
        }
    }

    public void submitRoute(RouteUnitRunnable unit) {
        try {
            routeQueue.put(unit);
        } catch (InterruptedException e) {
            log.info("Submit interrupted:" + e);
        }
    }

    public void submitInbound(InboundUnitRunnable unit) {
        try {
            inboundQueue.put(unit);
        } catch (InterruptedException e) {
            log.info("Submit interrupted:" + e);
        }
    }

    public void submitOutbound(OutboundUnitRunnable unit) {
        try {
            outboundQueue.put(unit);
        } catch (InterruptedException e) {
            log.info("Submit interrupted:" + e);
        }
    }

    public void submitTimerWork(TimerUnit unit) {
        try {
            timerQueue.put(unit);
        } catch (InterruptedException e) {
            log.info("Submit interrupted:" + e);
        }
    }

    public BlockingQueue<Runnable> getOutboundQueue() {
        return outboundQueue;
    }

    public ThreadPoolExecutor getOutboundThreadPool() {
        return outboundThreadPool;
    }

    public BlockingQueue<Runnable> getRouteQueue() {
        return routeQueue;
    }

    public ThreadPoolExecutor getRouteThreadPool() {
        return routeThreadPool;
    }

    public BlockingQueue<Runnable> getTimerQueue() {
        return timerQueue;
    }

    public ThreadPoolExecutor getTimerThreadPool() {
        return timerThreadPool;
    }

    public BlockingQueue<Runnable> getInboundQueue() {
        return inboundQueue;
    }

    public ThreadPoolExecutor getInboundThreadPool() {
        return inboundThreadPool;
    }

    public synchronized void destroy() {
        if (timerThreadPool != null) {
            stopPool(timerThreadPool, timerQueue, "TimerExec");
        }
        if (routeThreadPool != null) {
            stopPool(routeThreadPool, routeQueue, "RouteExec");
        }
        if (outboundThreadPool != null) {
            stopPool(outboundThreadPool, outboundQueue, "Outbound");
        }
        if (inboundThreadPool != null) {
            stopPool(inboundThreadPool, inboundQueue, "Inbound");
        }

        timerThreadPool = null;
        routeThreadPool = null;
        outboundThreadPool = null;
        inboundThreadPool = null;
    }

    public Thread makeEventSourceThread(String runtimeURI, String sourceName, Runnable runnable) {
        if (runtimeURI == null) {
            runtimeURI = "default";
        }

        String threadGroupName = "com.espertech.esper." + runtimeURI + "-source-" + sourceName;
        ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
        return new Thread(threadGroup, runnable);
    }

    public void submitInbound(EventBean event, EPRuntimeEventProcessWrapped runtimeEventSender) {
        submitInbound(new InboundUnitSendWrapped(event, servicesContext, runtimeEventSender));
    }

    private void stopPool(ThreadPoolExecutor threadPool, BlockingQueue<Runnable> queue, String name) {
        if (log.isInfoEnabled()) {
            log.info("Shutting down pool " + name);
        }

        queue.clear();

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Interruped awaiting termination", e);
        }
    }

    private ThreadPoolExecutor getThreadPool(String runtimeURI, String name, BlockingQueue<Runnable> queue, int numThreads) {
        if (log.isInfoEnabled()) {
            log.info("Starting pool " + name + " with " + numThreads + " threads");
        }

        if (runtimeURI == null) {
            runtimeURI = "default";
        }

        String threadGroupName = "com.espertech.esper." + runtimeURI + "-" + name;
        ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 1, TimeUnit.SECONDS, queue, new EngineThreadFactory(runtimeURI, name, threadGroup, Thread.NORM_PRIORITY));
        pool.prestartAllCoreThreads();

        return pool;
    }
}
