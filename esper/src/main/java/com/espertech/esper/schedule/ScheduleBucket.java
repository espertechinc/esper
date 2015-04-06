/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.schedule;

/**
 * This class acts as a buckets for sorting schedule service callbacks that are scheduled to occur at the same
 * time. Each buckets constists of {@link ScheduleSlot} slots that callbacks are
 * assigned to.
 * <p>
 * At the time of timer evaluation, callbacks that become triggerable are ordered using the bucket
 * as the first-level order, and slot as the second-level order.
 * <p>
 * Each statement at statement creation time allocates a buckets, and each timer within the
 * statement allocates a slot. Thus statements that depend on other statements (such as for insert-into),
 * and timers within their statement (such as time window or output rate limit timers) behave
 * deterministically.
 */
public class ScheduleBucket
{
    private final int bucketNum;
    private int lastSlot;

    /**
     * Ctor.
     * @param bucketNum is a simple integer number for this bucket by which buckets can be sorted
     */
    public ScheduleBucket(int bucketNum)
    {
        this.bucketNum = bucketNum;
        lastSlot = 0;
    }

    /**
     * Restart bucket slot numbering wuch as when a statement is restarted and new slots are allocated.
     */
    public void restart()
    {
        lastSlot = 0;
    }

    /**
     * Returns a new slot in the bucket.
     * @return slot
     */
    public ScheduleSlot allocateSlot()
    {
        return new ScheduleSlot(bucketNum, lastSlot++);
    }

    /**
     * Returns a new slot in the bucket, given a slot number
     * @return slot
     */
    public ScheduleSlot allocateSlot(int slotNumber)
    {
        return new ScheduleSlot(bucketNum, slotNumber);
    }
}
