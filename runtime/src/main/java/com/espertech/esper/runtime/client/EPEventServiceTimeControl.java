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
package com.espertech.esper.runtime.client;

/**
 * Service for advancing and controlling time.
 */
public interface EPEventServiceTimeControl {
    /**
     * Advance time by jumping to the given time in milliseconds (or nanoseconds if so configured).
     * <p>
     * For externally controlling the time within a runtime.
     * </p>
     * <p>
     * External clocking must be first be enabled by configuration {@link com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading#setInternalTimerEnabled(boolean)} passing false
     * or by calling {@link #clockExternal()}.
     * </p>
     * <p>
     * Time should never move backwards (unless for testing purposes where previous results can be thrown away)
     * </p>
     *
     * @param time time
     */
    void advanceTime(long time);

    /**
     * Advance time by continually-sliding to the given time in milliseconds (or nanoseconds if so configured) at the smallest resolution (non-hopping).
     * <p>
     * For externally controlling the time within a runtime.
     * </p>
     * <p>
     * External clocking must be first be enabled by configuration {@link com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading#setInternalTimerEnabled(boolean)} passing false
     * or by calling {@link #clockExternal()}.
     * </p>
     * <p>
     * Time should never move backwards (unless for testing purposes where previous results can be thrown away)
     * </p>
     *
     * @param time time
     */
    void advanceTimeSpan(long time);

    /**
     * Advance time by continually-sliding to the given time in milliseconds (or nanoseconds if so configured) at the provided resolution (hopping).
     * <p>
     * For externally controlling the time within a runtime.
     * </p>
     * <p>
     * External clocking must be first be enabled by configuration {@link com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading#setInternalTimerEnabled(boolean)} passing false
     * or by calling {@link #clockExternal()}.
     * </p>
     * <p>
     * Time should never move backwards (unless for testing purposes where previous results can be thrown away)
     * </p>
     *
     * @param time       time
     * @param resolution the resolution to use
     */
    void advanceTimeSpan(long time, long resolution);

    /**
     * Returns current engine time.
     * <p>
     * If time is provided externally via timer events, the function returns current time as externally provided.
     *
     * @return current engine time
     */
    long getCurrentTime();

    /**
     * Returns the time at which the next schedule execution is expected, returns null if no schedule execution is
     * outstanding.
     *
     * @return time of next schedule if any
     */
    Long getNextScheduledTime();

    /**
     * Switches on the internal timer which tracks system time. There is no effect if the runtime is already
     * on internal time.
     * <p>
     * Your application may not want to use {@link #advanceTime(long)}, {@link #advanceTimeSpan(long)} or {@link #advanceTimeSpan(long, long)}
     * after calling this method, since time advances according to JVM time.
     * </p>
     */
    void clockInternal();

    /**
     * Switches off the internal timer which tracks system time. There is no effect if the runtime is already
     * on external internal time.
     * <p>
     * Your application may want to use {@link #advanceTime(long)}, {@link #advanceTimeSpan(long)} or {@link #advanceTimeSpan(long, long)}
     * after calling this method to set or advance time.
     * </p>
     * <p>
     * Its generally preferable to turn off internal clocking (and thus turn on external clocking) by configuration {@link com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading#setInternalTimerEnabled(boolean)} passing false.
     * </p>
     */
    void clockExternal();

    /**
     * Returns true for external clocking, false for internal clocking.
     *
     * @return clocking indicator
     */
    boolean isExternalClockingEnabled();
}
