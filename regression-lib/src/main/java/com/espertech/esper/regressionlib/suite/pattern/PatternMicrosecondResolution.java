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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternMicrosecondResolution implements RegressionExecution {
    private final boolean micros;

    public PatternMicrosecondResolution(boolean isMicroseconds) {
        this.micros = isMicroseconds;
    }

    public void run(RegressionEnvironment env) {

        long time = DateTime.parseDefaultMSec("2002-05-30T09:00:00.000");
        long currentTime = System.currentTimeMillis();

        boolean millis = !micros;

        if (millis) {
            runAssertionPattern(env, 0, "timer:interval(1)", 1000);
        }
        if (micros) {
            runAssertionPattern(env, 0, "timer:interval(1)", 1000000);
        }

        if (millis) {
            runAssertionPattern(env, 0, "timer:interval(10 sec 5 msec)", 10005);
        }
        if (micros) {
            runAssertionPattern(env, 0, "timer:interval(10 sec 5 msec 1 usec)", 10005001);
        }

        if (millis) {
            runAssertionPattern(env, 0, "timer:interval(1 month 10 msec)", timePlusMonth(0, 1) + 10);
        }
        if (micros) {
            runAssertionPattern(env, 0, "timer:interval(1 month 10 usec)", timePlusMonth(0, 1) * 1000 + 10);
        }

        if (millis) {
            runAssertionPattern(env, currentTime, "timer:interval(1 month 50 msec)", timePlusMonth(currentTime, 1) + 50);
        }
        if (micros) {
            runAssertionPattern(env, currentTime * 1000 + 33, "timer:interval(3 month 100 usec)", timePlusMonth(currentTime, 3) * 1000 + 33 + 100);
        }

        if (millis) {
            runAssertionPattern(env, time, "timer:at(1, *, *, *, *, *)", time + 60000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 123, "timer:at(1, *, *, *, *, *)", time * 1000 + 60000000 + 123);
        }

        // Schedule Date-only
        if (millis) {
            runAssertionPattern(env, time, "timer:schedule(iso:'2002-05-30T09:01:00')", time + 60000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 123, "timer:schedule(iso:'2002-05-30T09:01:00')", time * 1000 + 60000000);
        }

        // Schedule Period-only
        if (millis) {
            runAssertionPattern(env, time, "every timer:schedule(period: 2 minute)", time + 120000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 123, "every timer:schedule(period: 2 minute)", time * 1000 + 123 + 120000000);
        }

        // Schedule Date+period
        if (millis) {
            runAssertionPattern(env, time, "every timer:schedule(iso:'2002-05-30T09:00:00/PT1M')", time + 60000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 345, "every timer:schedule(iso:'2002-05-30T09:00:00/PT1M')", time * 1000 + 60000000);
        }

        // Schedule recurring period
        if (millis) {
            runAssertionPattern(env, time, "every timer:schedule(iso:'R2/PT1M')", time + 60000, time + 120000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 345, "every timer:schedule(iso:'R2/PT1M')", time * 1000 + 345 + 60000000, time * 1000 + 345 + 120000000);
        }

        // Schedule date+recurring period
        if (millis) {
            runAssertionPattern(env, time, "every timer:schedule(iso:'R2/2002-05-30T09:01:00/PT1M')", time + 60000, time + 120000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 345, "every timer:schedule(iso:'R2/2002-05-30T09:01:00/PT1M')", time * 1000 + 60000000, time * 1000 + 120000000);
        }

        // Schedule with date computation
        if (millis) {
            runAssertionPattern(env, time, "timer:schedule(date: current_timestamp.withTime(9, 1, 0, 0))", time + 60000);
        }
        if (micros) {
            runAssertionPattern(env, time * 1000 + 345, "timer:schedule(date: current_timestamp.withTime(9, 1, 0, 0))", time * 1000 + 345 + 60000000);
        }
    }

    private static void runAssertionPattern(RegressionEnvironment env, long startTime, String patternExpr, long... flipTimes) {

        env.advanceTime(startTime);

        String epl = "@name('s0') select * from pattern[" + patternExpr + "]";
        env.compileDeploy(epl).addListener("s0");

        int count = 0;
        for (long flipTime : flipTimes) {
            env.advanceTime(flipTime - 1);
            assertFalse("Failed for flip " + count, env.listener("s0").getAndClearIsInvoked());

            env.advanceTime(flipTime);
            assertTrue("Failed for flip " + count, env.listener("s0").getAndClearIsInvoked());
            count++;
        }

        env.advanceTime(Long.MAX_VALUE);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static long timePlusMonth(long timeInMillis, int monthToAdd) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.add(Calendar.MONTH, monthToAdd);
        return cal.getTimeInMillis();
    }
}
