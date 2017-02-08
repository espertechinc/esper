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
/***************************************************************************************
 * Attribution Notice
 *
 * This file is imported from Metrics (https://github.com/codahale/metrics subproject metrics-core).
 * Metrics is Copyright (c) 2010-2012 Coda Hale, Yammer.com
 * Metrics is Published under Apache Software License 2.0, see LICENSE in root folder.
 *
 * Thank you for the Metrics developers efforts in making their library available under an Apache license.
 * EsperTech incorporates Metrics version 0.2.2 in source code form since Metrics depends on SLF4J
 * and this dependency is not possible to introduce for Esper.
 * *************************************************************************************
 */
package com.espertech.esper.metrics.codahale_metrics.metrics.core;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A manager class for a set of named thread pools.
 */
class ThreadPools {
    /**
     * A simple named thread factory.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        /**
         * Creates a new {@link ThreadPools.NamedThreadFactory} with the
         * given name.
         *
         * @param name the name of the threads, to be used in the pattern {@code
         *             metrics-$NAME$-thread-$NUMBER$}
         */
        NamedThreadFactory(String name) {
            final SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = "metrics-" + name + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private final ConcurrentMap<String, ScheduledExecutorService> threadPools =
            new ConcurrentHashMap<String, ScheduledExecutorService>(100);

    /**
     * Creates a new scheduled thread pool of a given size with the given name, or returns an
     * existing thread pool if one was already created with the same name.
     *
     * @param poolSize the number of threads to create
     * @param name     the name of the pool
     * @return a new {@link java.util.concurrent.ScheduledExecutorService}
     */
    ScheduledExecutorService newScheduledThreadPool(int poolSize, String name) {
        final ScheduledExecutorService existing = threadPools.get(name);
        if (isValidExecutor(existing)) {
            return existing;
        } else {
            // We lock here because executors are expensive to create. So
            // instead of just doing the usual putIfAbsent dance, we lock the
            // damn thing, check to see if anyone else put a thread pool in
            // there while we weren't watching.
            synchronized (this) {
                final ScheduledExecutorService lastChance = threadPools.get(name);
                if (isValidExecutor(lastChance)) {
                    return lastChance;
                } else {
                    final ScheduledExecutorService service =
                            Executors.newScheduledThreadPool(poolSize, new NamedThreadFactory(name));
                    threadPools.put(name, service);
                    return service;
                }
            }
        }
    }

    private static boolean isValidExecutor(ExecutorService executor) {
        return executor != null && !executor.isShutdown() && !executor.isTerminated();
    }

    /**
     * Shuts down all thread pools created by this class in an orderly fashion.
     */
    void shutdown() {
        synchronized (this) {
            for (ExecutorService executor : threadPools.values()) {
                executor.shutdown();
            }
            threadPools.clear();
        }
    }
}
