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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportEngineFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecPatternMicrosecondResolution implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        Map<TimeUnit, EPServiceProvider> epServices = SupportEngineFactory.setupEnginesByTimeUnit();

        long time = DateTime.parseDefaultMSec("2002-05-30T09:00:00.000");
        long currentTime = System.currentTimeMillis();
        EPServiceProvider engineMillis = epServices.get(TimeUnit.MILLISECONDS);
        EPServiceProvider engineMicros = epServices.get(TimeUnit.MICROSECONDS);

        runAssertionPattern(engineMillis, 0, "timer:interval(1)", 1000);
        runAssertionPattern(engineMicros, 0, "timer:interval(1)", 1000000);

        runAssertionPattern(engineMillis, 0, "timer:interval(10 sec 5 msec)", 10005);
        runAssertionPattern(engineMicros, 0, "timer:interval(10 sec 5 msec 1 usec)", 10005001);

        runAssertionPattern(engineMillis, 0, "timer:interval(1 month 10 msec)", timePlusMonth(0, 1) + 10);
        runAssertionPattern(engineMicros, 0, "timer:interval(1 month 10 usec)", timePlusMonth(0, 1) * 1000 + 10);

        runAssertionPattern(engineMillis, currentTime, "timer:interval(1 month 50 msec)", timePlusMonth(currentTime, 1) + 50);
        runAssertionPattern(engineMicros, currentTime * 1000 + 33, "timer:interval(3 month 100 usec)", timePlusMonth(currentTime, 3) * 1000 + 33 + 100);

        runAssertionPattern(engineMillis, time, "timer:at(1, *, *, *, *, *)", time + 60000);
        runAssertionPattern(engineMicros, time * 1000 + 123, "timer:at(1, *, *, *, *, *)", time * 1000 + 60000000 + 123);

        // Schedule Date-only
        runAssertionPattern(engineMillis, time, "timer:schedule(iso:'2002-05-30T09:01:00')", time + 60000);
        runAssertionPattern(engineMicros, time * 1000 + 123, "timer:schedule(iso:'2002-05-30T09:01:00')", time * 1000 + 60000000);

        // Schedule Period-only
        runAssertionPattern(engineMillis, time, "every timer:schedule(period: 2 minute)", time + 120000);
        runAssertionPattern(engineMicros, time * 1000 + 123, "every timer:schedule(period: 2 minute)", time * 1000 + 123 + 120000000);

        // Schedule Date+period
        runAssertionPattern(engineMillis, time, "every timer:schedule(iso:'2002-05-30T09:00:00/PT1M')", time + 60000);
        runAssertionPattern(engineMicros, time * 1000 + 345, "every timer:schedule(iso:'2002-05-30T09:00:00/PT1M')", time * 1000 + 60000000);

        // Schedule recurring period
        runAssertionPattern(engineMillis, time, "every timer:schedule(iso:'R2/PT1M')", time + 60000, time + 120000);
        runAssertionPattern(engineMicros, time * 1000 + 345, "every timer:schedule(iso:'R2/PT1M')", time * 1000 + 345 + 60000000, time * 1000 + 345 + 120000000);

        // Schedule date+recurring period
        runAssertionPattern(engineMillis, time, "every timer:schedule(iso:'R2/2002-05-30T09:01:00/PT1M')", time + 60000, time + 120000);
        runAssertionPattern(engineMicros, time * 1000 + 345, "every timer:schedule(iso:'R2/2002-05-30T09:01:00/PT1M')", time * 1000 + 60000000, time * 1000 + 120000000);

        // Schedule with date computation
        runAssertionPattern(engineMillis, time, "timer:schedule(date: current_timestamp.withTime(9, 1, 0, 0))", time + 60000);
        runAssertionPattern(engineMicros, time * 1000 + 345, "timer:schedule(date: current_timestamp.withTime(9, 1, 0, 0))", time * 1000 + 345 + 60000000);
    }

    private void runAssertionPattern(EPServiceProvider epService, long startTime, String patternExpr, long... flipTimes) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("iso");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = isolated.getEPAdministrator().createEPL("select * from pattern[" + patternExpr + "]", "s0", null);
        stmt.addListener(listener);

        int count = 0;
        for (long flipTime : flipTimes) {
            isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime - 1));
            assertFalse("Failed for flip " + count, listener.getAndClearIsInvoked());

            isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime));
            assertTrue("Failed for flip " + count, listener.getAndClearIsInvoked());
            count++;
        }

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(Long.MAX_VALUE));
        assertFalse(listener.getAndClearIsInvoked());

        isolated.destroy();
    }

    private static long timePlusMonth(long timeInMillis, int monthToAdd) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.add(Calendar.MONTH, monthToAdd);
        return cal.getTimeInMillis();
    }
}
