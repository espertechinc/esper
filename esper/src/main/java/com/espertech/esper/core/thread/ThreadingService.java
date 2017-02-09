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

import com.espertech.esper.core.service.EPRuntimeImpl;
import com.espertech.esper.core.service.EPServicesContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Engine-level threading services.
 */
public interface ThreadingService {
    /**
     * Destroy thread pools.
     */
    public void destroy();

    /**
     * Initialize thread pools.
     *
     * @param services engine-level service context
     * @param runtime  runtime
     */
    public void initThreading(EPServicesContext services, EPRuntimeImpl runtime);

    /**
     * Returns true for timer execution threading enabled.
     *
     * @return indicator
     */
    public boolean isTimerThreading();

    /**
     * Submit timer execution work unit.
     *
     * @param timerUnit unit of work
     */
    public void submitTimerWork(TimerUnit timerUnit);

    /**
     * Returns true for inbound threading enabled.
     *
     * @return indicator
     */
    public boolean isInboundThreading();

    /**
     * Submit inbound work unit.
     *
     * @param unit unit of work
     */
    public void submitInbound(InboundUnitRunnable unit);

    /**
     * Returns true for route execution threading enabled.
     *
     * @return indicator
     */
    public boolean isRouteThreading();

    /**
     * Submit route work unit.
     *
     * @param unit unit of work
     */
    public void submitRoute(RouteUnitRunnable unit);

    /**
     * Returns true for outbound threading enabled.
     *
     * @return indicator
     */
    public boolean isOutboundThreading();

    /**
     * Submit outbound work unit.
     *
     * @param unit unit of work
     */
    public void submitOutbound(OutboundUnitRunnable unit);

    /**
     * Returns the outbound queue.
     *
     * @return queue
     */
    public BlockingQueue<Runnable> getOutboundQueue();

    /**
     * Returns the outbound thread pool
     *
     * @return thread pool
     */
    public ThreadPoolExecutor getOutboundThreadPool();

    /**
     * Returns the route queue.
     *
     * @return queue
     */
    public BlockingQueue<Runnable> getRouteQueue();

    /**
     * Returns the route thread pool
     *
     * @return thread pool
     */
    public ThreadPoolExecutor getRouteThreadPool();

    /**
     * Returns the timer queue.
     *
     * @return queue
     */
    public BlockingQueue<Runnable> getTimerQueue();

    /**
     * Returns the timer thread pool
     *
     * @return thread pool
     */
    public ThreadPoolExecutor getTimerThreadPool();

    /**
     * Returns the inbound queue.
     *
     * @return queue
     */
    public BlockingQueue<Runnable> getInboundQueue();

    /**
     * Returns the inbound thread pool
     *
     * @return thread pool
     */
    public ThreadPoolExecutor getInboundThreadPool();

    public Thread makeEventSourceThread(String engineURI, String sourceName, Runnable runnable);
}
