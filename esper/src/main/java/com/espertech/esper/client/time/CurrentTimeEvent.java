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
package com.espertech.esper.client.time;

import java.util.Date;

/**
 * Event for externally controlling the time within an {@link com.espertech.esper.client.EPRuntime}
 * or {@link com.espertech.esper.client.EPRuntimeIsolated} instance.
 * External clocking must be enabled via {@link TimerControlEvent} before this class can be used
 * to externally feed time.
 */
public final class CurrentTimeEvent extends TimerEvent {
    private static final long serialVersionUID = -145001725701558269L;
    private long time;

    /**
     * Constructor.
     *
     * @param time is the time in milliseconds (or microseconds if so configured)
     */
    public CurrentTimeEvent(final long time) {
        this.time = time;
    }

    public String toString() {
        return (new Date(time)).toString();
    }

    /**
     * Returns the time in milliseconds (or microseconds if so configured).
     *
     * @return time
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the time in milliseconds (or microseconds if so configured).
     *
     * @param time to set
     */
    public void setTime(long time) {
        this.time = time;
    }
}
