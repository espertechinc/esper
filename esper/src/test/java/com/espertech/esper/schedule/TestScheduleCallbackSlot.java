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

import junit.framework.TestCase;

public class TestScheduleCallbackSlot extends TestCase {
    public void testCompare() {
        long slots[] = new long[10];
        slots[0] = ScheduleBucket.toLong(1, 1);
        slots[1] = ScheduleBucket.toLong(1, 2);
        slots[2] = ScheduleBucket.toLong(2, 1);
        slots[3] = ScheduleBucket.toLong(2, 2);

        assertEquals(-1, compare(slots[0], slots[1]));
        assertEquals(1, compare(slots[1], slots[0]));
        assertEquals(0, compare(slots[0], slots[0]));

        assertEquals(-1, compare(slots[0], slots[2]));
        assertEquals(-1, compare(slots[1], slots[2]));
        assertEquals(1, compare(slots[2], slots[0]));
        assertEquals(1, compare(slots[2], slots[1]));
    }

    private int compare(long first, long second) {
        return ((Long) first).compareTo(second);
    }
}
