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

package com.espertech.esper.metrics.codahale_metrics.metrics.reporting;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.MetricsRegistry;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An abstract base class for all reporter implementations which periodically poll registered
 * metrics (e.g., to send the data to another service).
 */
public abstract class AbstractPollingReporter extends AbstractReporter implements Runnable {
    private final ScheduledExecutorService executor;

    /**
     * Creates a new {@link AbstractPollingReporter} instance.
     *
     * @param registry the {@link MetricsRegistry} containing the metrics this reporter will
     *                 report
     * @param name     the reporter's name
     * @see AbstractReporter#AbstractReporter(MetricsRegistry)
     */
    protected AbstractPollingReporter(MetricsRegistry registry, String name) {
        super(registry);
        this.executor = registry.newScheduledThreadPool(1, name);
    }

    /**
     * Starts the reporter polling at the given period.
     *
     * @param period the amount of time between polls
     * @param unit   the unit for {@code period}
     */
    public void start(long period, TimeUnit unit) {
        executor.scheduleWithFixedDelay(this, period, period, unit);
    }

    /**
     * Shuts down the reporter polling, waiting the specific amount of time for any current polls to
     * complete.
     *
     * @param timeout the maximum time to wait
     * @param unit    the unit for {@code timeout}
     * @throws InterruptedException if interrupted while waiting
     */
    public void shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(timeout, unit);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        super.shutdown();
    }

    /**
     * The method called when a a poll is scheduled to occur.
     */
    @Override
    public abstract void run();
}
