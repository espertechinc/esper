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
package com.espertech.esper.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Timer task to simply invoke the callback when triggered.
 */
final class EPLTimerTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(EPLTimerTask.class);
    private final TimerCallback callback;
    private ScheduledFuture<?> future;
    private boolean isCancelled;

    protected boolean enableStats;
    protected long lastDrift;
    protected long maxDrift;
    protected long totalDrift;
    protected long invocationCount;

    public EPLTimerTask(TimerCallback callback) {
        this.callback = callback;
        enableStats = false;
        lastDrift = 0;
        maxDrift = 0;
        totalDrift = 0;
        invocationCount = 0;
    }

    public final void run() {
        if (!isCancelled) {
            if (enableStats) {
                // If we are called early, then delay will be positive. If we are called late, then the delay will be negative.
                // NOTE: don't allow enableStats to be set until future has been set
                if (future != null) {
                    lastDrift = Math.abs(future.getDelay(TimeUnit.MILLISECONDS));
                }
                totalDrift += lastDrift;
                invocationCount++;
                if (lastDrift > maxDrift) maxDrift = lastDrift;
            }

            try {
                callback.timerCallback();
            } catch (Throwable t) {
                log.error("Timer thread caught unhandled exception: " + t.getMessage(), t);
            }
        }
    }

    protected void resetStats() {
        invocationCount = 0;
        lastDrift = 0;
        totalDrift = 0;
        maxDrift = 0;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }
}
