/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.client.time;

import java.util.Date;

/**
 * Event for externally controlling the time within an {@link com.espertech.esper.client.EPRuntime}
 * or {@link com.espertech.esper.client.EPRuntimeIsolated} instance.
 * External clocking must be enabled via {@link TimerControlEvent} before this class can be used
 * to externally feed time.
 */
public final class CurrentTimeEvent extends TimerEvent
{
    private static final long serialVersionUID = -145001725701558269L;
    private long timeInMillis;

    /**
     * Constructor.
     * @param timeInMillis is the time in milliseconds
     */
    public CurrentTimeEvent(final long timeInMillis)
    {
        this.timeInMillis = timeInMillis;
    }

    public String toString()
    {
        return (new Date(timeInMillis)).toString();
    }

    /**
     * Returns the time in milliseconds.
     * @return time in milliseconds
     */
    public long getTimeInMillis()
    {
        return timeInMillis;
    }

    /**
     * Sets the time in milliseconds.
     * @param timeInMillis to set
     */
    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }
}
