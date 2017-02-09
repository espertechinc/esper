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
package com.espertech.esper.schedule;

/**
 * Entry for a schedule item.
 */
public class ScheduleSetEntry {
    private Long time;
    private long scheduleSlot;
    private ScheduleHandle handle;

    /**
     * Ctor.
     *
     * @param time         of schedule
     * @param scheduleSlot scheduleSlot
     * @param handle       handle to use
     */
    public ScheduleSetEntry(Long time, long scheduleSlot, ScheduleHandle handle) {
        this.time = time;
        this.scheduleSlot = scheduleSlot;
        this.handle = handle;
    }

    /**
     * Sets time.
     *
     * @param time value
     */
    public void setTime(Long time) {
        this.time = time;
    }

    /**
     * Returns time.
     *
     * @return time
     */
    public Long getTime() {
        return time;
    }

    /**
     * Returns schedule scheduleSlot.
     *
     * @return scheduleSlot
     */
    public long getScheduleSlot() {
        return scheduleSlot;
    }

    /**
     * Returns the schedule handle.
     *
     * @return handle
     */
    public ScheduleHandle getHandle() {
        return handle;
    }
}