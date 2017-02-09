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

import com.espertech.esper.epl.expression.time.TimeAbacusMilliseconds;
import com.espertech.esper.supportunit.schedule.SupportScheduleCallback;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import com.espertech.esper.type.ScheduleUnit;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TimeZone;

public class TestSchedulingServiceImpl extends TestCase {
    private SchedulingServiceImpl service;
    private SchedulingMgmtServiceImpl mgmtService;

    private long slots[][];
    private SupportScheduleCallback callbacks[];

    public void setUp() {
        service = new SchedulingServiceImpl(new TimeSourceServiceImpl());
        mgmtService = new SchedulingMgmtServiceImpl();

        // 2-by-2 table of buckets and slots
        ScheduleBucket[] buckets = new ScheduleBucket[3];
        slots = new long[buckets.length][2];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = mgmtService.allocateBucket();
            slots[i] = new long[2];
            for (int j = 0; j < slots[i].length; j++) {
                slots[i][j] = buckets[i].allocateSlot();
            }
        }

        callbacks = new SupportScheduleCallback[5];
        for (int i = 0; i < callbacks.length; i++) {
            callbacks[i] = new SupportScheduleCallback();
        }
    }

    public void testAddTwice() {
        service.add(100, callbacks[0], slots[0][0]);
        assertTrue(service.isScheduled(callbacks[0]));
        service.add(100, callbacks[0], slots[0][0]);

        service.add(ScheduleComputeHelper.computeNextOccurance(new ScheduleSpec(), service.getTime(), TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE), callbacks[1], slots[0][0]);
        service.add(ScheduleComputeHelper.computeNextOccurance(new ScheduleSpec(), service.getTime(), TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE), callbacks[1], slots[0][0]);
    }

    public void testTrigger() {
        long startTime = 0;

        service.setTime(0);

        // Add callbacks
        service.add(20, callbacks[3], slots[1][1]);
        service.add(20, callbacks[2], slots[1][0]);
        service.add(20, callbacks[1], slots[0][1]);
        service.add(21, callbacks[0], slots[0][0]);
        assertTrue(service.isScheduled(callbacks[3]));
        assertTrue(service.isScheduled(callbacks[0]));

        // Evaluate before the within time, expect not results
        startTime += 19;
        service.setTime(startTime);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});
        assertTrue(service.isScheduled(callbacks[3]));

        // Evaluate exactly on the within time, expect a result
        startTime += 1;
        service.setTime(startTime);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 1, 2, 3, 0});
        assertFalse(service.isScheduled(callbacks[3]));

        // Evaluate after already evaluated once, no result
        startTime += 1;
        service.setTime(startTime);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{4, 0, 0, 0, 0});
        assertFalse(service.isScheduled(callbacks[3]));

        startTime += 1;
        service.setTime(startTime);
        evaluateSchedule();
        assertEquals(0, callbacks[3].clearAndGetOrderTriggered());

        // Adding the same callback more than once should cause an exception
        service.add(20, callbacks[0], slots[0][0]);
        service.add(28, callbacks[0], slots[0][0]);
        service.remove(callbacks[0], slots[0][0]);

        service.add(20, callbacks[2], slots[1][0]);
        service.add(25, callbacks[1], slots[0][1]);
        service.remove(callbacks[1], slots[0][1]);
        service.add(21, callbacks[0], slots[0][0]);
        service.add(21, callbacks[3], slots[1][1]);
        service.add(20, callbacks[1], slots[0][1]);
        SupportScheduleCallback.setCallbackOrderNum(0);

        startTime += 20;
        service.setTime(startTime);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 1, 2, 0, 0});

        startTime += 1;
        service.setTime(startTime);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{3, 0, 0, 4, 0});

        service.setTime(startTime + Integer.MAX_VALUE);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});
    }

    public void testWaitAndSpecTogether() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2004, 11, 9, 15, 27, 10);
        calendar.set(Calendar.MILLISECOND, 500);
        long startTime = calendar.getTimeInMillis();

        service.setTime(startTime);

        // Add a specification
        ScheduleSpec spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MONTHS, 12);
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 9);
        spec.addValue(ScheduleUnit.HOURS, 15);
        spec.addValue(ScheduleUnit.MINUTES, 27);
        spec.addValue(ScheduleUnit.SECONDS, 20);

        service.add(ScheduleComputeHelper.computeDeltaNextOccurance(spec, service.getTime(), TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE), callbacks[3], slots[1][1]);

        spec.addValue(ScheduleUnit.SECONDS, 15);
        service.add(ScheduleComputeHelper.computeDeltaNextOccurance(spec, service.getTime(), TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE), callbacks[4], slots[2][0]);

        // Add some more callbacks
        service.add(5000, callbacks[0], slots[0][0]);
        service.add(10000, callbacks[1], slots[0][1]);
        service.add(15000, callbacks[2], slots[1][0]);

        // Now send a times reflecting various seconds later and check who got a callback
        service.setTime(startTime + 1000);
        SupportScheduleCallback.setCallbackOrderNum(0);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});

        service.setTime(startTime + 2000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});

        service.setTime(startTime + 4000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});

        service.setTime(startTime + 5000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{1, 0, 0, 0, 2});

        service.setTime(startTime + 9000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});

        service.setTime(startTime + 10000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 3, 0, 4, 0});

        service.setTime(startTime + 11000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});

        service.setTime(startTime + 15000);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 5, 0, 0});

        service.setTime(startTime + Integer.MAX_VALUE);
        evaluateSchedule();
        checkCallbacks(callbacks, new Integer[]{0, 0, 0, 0, 0});
    }

    public void testIncorrectRemove() {
        SchedulingServiceImpl evaluator = new SchedulingServiceImpl(new TimeSourceServiceImpl());
        SupportScheduleCallback callback = new SupportScheduleCallback();
        evaluator.remove(callback, 0);
    }

    private void checkCallbacks(SupportScheduleCallback callbacks[], Integer[] results) {
        assertTrue(callbacks.length == results.length);

        for (int i = 0; i < callbacks.length; i++) {
            assertEquals((int) results[i], (int) callbacks[i].clearAndGetOrderTriggered());
        }
    }

    private void evaluateSchedule() {
        Collection<ScheduleHandle> handles = new LinkedList<ScheduleHandle>();
        service.evaluate(handles);

        for (ScheduleHandle handle : handles) {
            ScheduleHandleCallback cb = (ScheduleHandleCallback) handle;
            cb.scheduledTrigger(null);
        }
    }
}
