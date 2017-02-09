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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the internal clocking service interface.
 */
public final class TimerServiceImpl implements TimerService {
    private final String engineURI;
    private final long msecTimerResolution;
    private TimerCallback timerCallback;
    private ScheduledThreadPoolExecutor timer;
    private EPLTimerTask timerTask;
    private final static AtomicInteger NEXT_ID = new AtomicInteger(0);
    private final int id;

    /**
     * Constructor.
     *
     * @param msecTimerResolution is the millisecond resolution or interval the internal timer thread
     *                            processes schedules
     * @param engineURI           engine URI
     */
    public TimerServiceImpl(String engineURI, long msecTimerResolution) {
        this.engineURI = engineURI;
        this.msecTimerResolution = msecTimerResolution;
        id = NEXT_ID.getAndIncrement();
    }

    /**
     * Returns the timer resolution.
     *
     * @return the millisecond resolution or interval the internal timer thread
     * processes schedules
     */
    public long getMsecTimerResolution() {
        return msecTimerResolution;
    }

    public void setCallback(TimerCallback timerCallback) {
        this.timerCallback = timerCallback;
    }

    public final void startInternalClock() {
        if (timer != null) {
            log.warn(".startInternalClock Internal clock is already started, stop first before starting, operation not completed");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(".startInternalClock Starting internal clock daemon thread, resolution=" + msecTimerResolution);
        }

        if (timerCallback == null) {
            throw new IllegalStateException("Timer callback not set");
        }

        getScheduledThreadPoolExecutorDaemonThread();
        timerTask = new EPLTimerTask(timerCallback);

        // With no delay start every internal
        ScheduledFuture<?> future = timer.scheduleAtFixedRate(timerTask, 0, msecTimerResolution, TimeUnit.MILLISECONDS);
        timerTask.setFuture(future);
    }

    public final void stopInternalClock(boolean warnIfNotStarted) {
        if (timer == null) {
            if (warnIfNotStarted) {
                log.warn(".stopInternalClock Internal clock is already stopped, start first before stopping, operation not completed");
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(".stopInternalClock Stopping internal clock daemon thread");
        }

        timer.shutdown();

        try {
            // Sleep for 100 ms to await the internal timer
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.info("Timer start wait interval interruped");
        }

        timer = null;
    }


    public void enableStats() {
        if (timerTask != null) {
            timerTask.enableStats = true;
        }
    }

    public void disableStats() {
        if (timerTask != null) {
            timerTask.enableStats = false;
            // now it is safe to reset stats without any synchronization
            timerTask.resetStats();
        }
    }

    public long getMaxDrift() {
        return timerTask.maxDrift;
    }

    public long getLastDrift() {
        return timerTask.lastDrift;
    }

    public long getTotalDrift() {
        return timerTask.totalDrift;
    }

    public long getInvocationCount() {
        return timerTask.invocationCount;
    }

    private void getScheduledThreadPoolExecutorDaemonThread() {
        timer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            // set new thread as daemon thread and name appropriately
            public Thread newThread(Runnable r) {
                String uri = engineURI;
                if (engineURI == null) {
                    uri = "default";
                }
                Thread t = new Thread(r, "com.espertech.esper.Timer-" + uri + "-" + id);
                t.setDaemon(true);
                return t;
            }
        });
        timer.setMaximumPoolSize(timer.getCorePoolSize());
        timer.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        timer.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    private static final Logger log = LoggerFactory.getLogger(TimerServiceImpl.class);
}
