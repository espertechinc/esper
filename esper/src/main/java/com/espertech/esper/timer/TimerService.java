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

/**
 * Service interface for repeated callbacks at regular intervals.
 */
public interface TimerService {
    /**
     * Set the callback method to invoke for clock ticks.
     *
     * @param timerCallback is the callback
     */
    public void setCallback(TimerCallback timerCallback);

    /**
     * Start clock expecting callbacks at regular intervals and a fixed rate.
     * Catch-up callbacks are possible should the callback fall behind.
     */
    public void startInternalClock();

    /**
     * Stop internal clock.
     *
     * @param warnIfNotStarted use true to indicate whether to warn if the clock is not started, use false to not warn
     *                         and expect the clock to be not started.
     */
    public void stopInternalClock(boolean warnIfNotStarted);

    /**
     * Enable statistics.
     */
    public void enableStats();

    /**
     * Disable statistics.
     */
    public void disableStats();

    /**
     * Return maximum drift.
     *
     * @return drift
     */
    public long getMaxDrift();

    /**
     * Return last drift.
     *
     * @return drift
     */
    public long getLastDrift();

    /**
     * Return total drift.
     *
     * @return drift
     */
    public long getTotalDrift();

    /**
     * Return invocation count.
     *
     * @return count
     */
    public long getInvocationCount();
}
