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
package com.espertech.esper.core.thread;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.core.service.EPRuntimeImpl;
import com.espertech.esper.core.service.EPServicesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Implementation for engine-level threading.
 */
public class ThreadingServiceImpl implements ThreadingService {
    private static final Logger log = LoggerFactory.getLogger(ThreadingServiceImpl.class);

    private final ConfigurationEngineDefaults.Threading config;
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

    /**
     * Ctor.
     *
     * @param threadingConfig configuration
     */
    public ThreadingServiceImpl(ConfigurationEngineDefaults.Threading threadingConfig) {
        this.config = threadingConfig;
        if (ThreadingOption.isThreadingEnabled()) {
            isTimerThreading = threadingConfig.isThreadPoolTimerExec();
            isInboundThreading = threadingConfig.isThreadPoolInbound();
            isRouteThreading = threadingConfig.isThreadPoolRouteExec();
            isOutboundThreading = threadingConfig.isThreadPoolOutbound();
        } else {
            isTimerThreading = false;
            isInboundThreading = false;
            isRouteThreading = false;
            isOutboundThreading = false;
        }
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

    public void initThreading(EPServicesContext services, EPRuntimeImpl runtime) {
        if (isInboundThreading) {
            inboundQueue = makeQueue(config.getThreadPoolInboundCapacity());
            inboundThreadPool = getThreadPool(services.getEngineURI(), "Inbound", inboundQueue, config.getThreadPoolInboundNumThreads());
        }

        if (isTimerThreading) {
            timerQueue = makeQueue(config.getThreadPoolTimerExecCapacity());
            timerThreadPool = getThreadPool(services.getEngineURI(), "TimerExec", timerQueue, config.getThreadPoolTimerExecNumThreads());
        }

        if (isRouteThreading) {
            routeQueue = makeQueue(config.getThreadPoolRouteExecCapacity());
            routeThreadPool = getThreadPool(services.getEngineURI(), "RouteExec", routeQueue, config.getThreadPoolRouteExecNumThreads());
        }

        if (isOutboundThreading) {
            outboundQueue = makeQueue(config.getThreadPoolOutboundCapacity());
            outboundThreadPool = getThreadPool(services.getEngineURI(), "Outbound", outboundQueue, config.getThreadPoolOutboundNumThreads());
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

    private ThreadPoolExecutor getThreadPool(String engineURI, String name, BlockingQueue<Runnable> queue, int numThreads) {
        if (log.isInfoEnabled()) {
            log.info("Starting pool " + name + " with " + numThreads + " threads");
        }

        if (engineURI == null) {
            engineURI = "default";
        }

        String threadGroupName = "com.espertech.esper." + engineURI + "-" + name;
        ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 1, TimeUnit.SECONDS, queue, new EngineThreadFactory(engineURI, name, threadGroup, Thread.NORM_PRIORITY));
        pool.prestartAllCoreThreads();

        return pool;
    }

    public Thread makeEventSourceThread(String engineURI, String sourceName, Runnable runnable) {
        if (engineURI == null) {
            engineURI = "default";
        }

        String threadGroupName = "com.espertech.esper." + engineURI + "-source-" + sourceName;
        ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
        return new Thread(threadGroup, runnable);
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
}
