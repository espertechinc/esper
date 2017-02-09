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
package com.espertech.esper.metrics.codahale_metrics.metrics.stats;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.exp;

/**
 * An exponentially-weighted moving average.
 *
 * @see <a href="http://www.teamquest.com/pdfs/whitepaper/ldavg1.pdf">UNIX Load Average Part 1: How
 * It Works</a>
 * @see <a href="http://www.teamquest.com/pdfs/whitepaper/ldavg2.pdf">UNIX Load Average Part 2: Not
 * Your Average Average</a>
 */
public class EWMA {
    private static final int INTERVAL = 5;
    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final int ONE_MINUTE = 1;
    private static final int FIVE_MINUTES = 5;
    private static final int FIFTEEN_MINUTES = 15;
    private static final double M1_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / ONE_MINUTE);
    private static final double M5_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / FIVE_MINUTES);
    private static final double M15_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / FIFTEEN_MINUTES);

    private volatile boolean initialized = false;
    private volatile double rate = 0.0;

    private final AtomicLong uncounted = new AtomicLong();
    private final double alpha, interval;

    /**
     * Creates a new EWMA which is equivalent to the UNIX one minute load average and which expects
     * to be ticked every 5 seconds.
     *
     * @return a one-minute EWMA
     */
    public static EWMA oneMinuteEWMA() {
        return new EWMA(M1_ALPHA, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX five minute load average and which expects
     * to be ticked every 5 seconds.
     *
     * @return a five-minute EWMA
     */
    public static EWMA fiveMinuteEWMA() {
        return new EWMA(M5_ALPHA, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX fifteen minute load average and which
     * expects to be ticked every 5 seconds.
     *
     * @return a fifteen-minute EWMA
     */
    public static EWMA fifteenMinuteEWMA() {
        return new EWMA(M15_ALPHA, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Create a new EWMA with a specific smoothing constant.
     *
     * @param alpha        the smoothing constant
     * @param interval     the expected tick interval
     * @param intervalUnit the time unit of the tick interval
     */
    public EWMA(double alpha, long interval, TimeUnit intervalUnit) {
        this.interval = intervalUnit.toNanos(interval);
        this.alpha = alpha;
    }

    /**
     * Update the moving average with a new value.
     *
     * @param n the new value
     */
    public void update(long n) {
        uncounted.addAndGet(n);
    }

    /**
     * Mark the passage of time and decay the current rate accordingly.
     */
    public void tick() {
        final long count = uncounted.getAndSet(0);
        final double instantRate = count / interval;
        if (initialized) {
            rate += alpha * (instantRate - rate);
        } else {
            rate = instantRate;
            initialized = true;
        }
    }

    /**
     * Returns the rate in the given units of time.
     *
     * @param rateUnit the unit of time
     * @return the rate
     */
    public double rate(TimeUnit rateUnit) {
        return rate * (double) rateUnit.toNanos(1);
    }
}
