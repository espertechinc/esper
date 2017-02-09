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
package com.espertech.esper.epl.metric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics executor relying on a cached threadpool.
 */
public class MetricsExecutorThreaded implements MetricsExecutor {
    private static final Logger log = LoggerFactory.getLogger(MetricsExecutorThreaded.class);
    private final ExecutorService threadPool;

    /**
     * Ctor.
     *
     * @param engineURI engine URI
     */
    public MetricsExecutorThreaded(final String engineURI) {
        ThreadFactory threadFactory = new ThreadFactory() {
            AtomicInteger count = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                String uri = engineURI;
                if (engineURI == null) {
                    uri = "default";
                }
                Thread t = new Thread(r);
                t.setName("com.espertech.esper.MetricReporting-" + uri + "-" + count.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        threadPool = Executors.newCachedThreadPool(threadFactory);
    }

    public void execute(final MetricExec execution, final MetricExecutionContext executionContext) {
        Runnable runnable = new Runnable() {
            public void run() {
                execution.execute(executionContext);
            }
        };
        threadPool.execute(runnable);
    }

    public void destroy() {
        threadPool.shutdownNow();

        try {
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }
}
