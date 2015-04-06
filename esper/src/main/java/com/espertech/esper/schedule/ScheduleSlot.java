/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.schedule;

import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;

/**
 * This class is a slot in a {@link ScheduleBucket} for sorting schedule service callbacks.
 */
public class ScheduleSlot implements Comparable<ScheduleSlot>, MetaDefItem, Serializable
{
    private final int bucketNum;
    private final int slotNum;
    private static final long serialVersionUID = 4560709630904887631L;

    /**
     * Ctor.
     * @param bucketNum is the number of the bucket the slot belongs to
     * @param slotNum is the slot number for ordering within the bucket
     */
    public ScheduleSlot(int bucketNum, int slotNum)
    {
        this.bucketNum = bucketNum;
        this.slotNum = slotNum;
    }

    /**
     * Returns the bucket number.
     * @return bucket number
     */
    public int getBucketNum()
    {
        return bucketNum;
    }

    /**
     * Returns the slot number.
     * @return slot number
     */
    public int getSlotNum()
    {
        return slotNum;
    }

    public int compareTo(ScheduleSlot scheduleCallbackSlot)
    {
        if (this.bucketNum > scheduleCallbackSlot.bucketNum)
        {
            return 1;
        }
        if (this.bucketNum < scheduleCallbackSlot.bucketNum)
        {
            return -1;
        }
        if (this.slotNum > scheduleCallbackSlot.slotNum)
        {
            return 1;
        }
        if (this.slotNum < scheduleCallbackSlot.slotNum)
        {
            return -1;
        }

        return 0;
    }

    public String toString()
    {
        return "bucket/slot=" + bucketNum + "/" + slotNum;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        ScheduleSlot that = (ScheduleSlot) o;

        if (bucketNum != that.bucketNum)
        {
            return false;
        }
        if (slotNum != that.slotNum)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = bucketNum;
        result = 31 * result + slotNum;
        return result;
    }
}
