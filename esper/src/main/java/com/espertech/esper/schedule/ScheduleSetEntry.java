/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.schedule;

/**
 * Entry for a schedule item.
 */
public class ScheduleSetEntry
{
    private Long time;
    private ScheduleSlot slot;
    private ScheduleHandle handle;

    /**
     * Ctor.
     * @param time of schedule
     * @param slot slot
     * @param handle handle to use
     */
    public ScheduleSetEntry(Long time, ScheduleSlot slot, ScheduleHandle handle)
    {
        this.time = time;
        this.slot = slot;
        this.handle = handle;
    }

    /**
     * Sets time.
     * @param time value
     */
    public void setTime(Long time)
    {
        this.time = time;
    }

    /**
     * Returns time.
     * @return time
     */
    public Long getTime()
    {
        return time;
    }

    /**
     * Returns schedule slot.
     * @return slot
     */
    public ScheduleSlot getSlot()
    {
        return slot;
    }

    /**
     * Returns the schedule handle.
     * @return handle
     */
    public ScheduleHandle getHandle()
    {
        return handle;
    }
}