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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternMicrosecondResolutionCrontab implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runSequenceMicro(env, "2013-08-23T08:05:00.000u000",
            "select * from pattern [ every timer:at(*, *, *, *, *, *, *, *, 200) ]", new String[]{
                "2013-08-23T08:05:00.000u200",
                "2013-08-23T08:05:00.001u200",
                "2013-08-23T08:05:00.002u200",
                "2013-08-23T08:05:00.003u200"
            });

        runSequenceMicro(env, "2013-08-23T08:05:00.000u000",
            "select * from pattern [ every timer:at(*, *, *, *, *, *, *, *, [200,201,202,300,500]) ]",
            new String[]{
                "2013-08-23T08:05:00.000u200",
                "2013-08-23T08:05:00.000u201",
                "2013-08-23T08:05:00.000u202",
                "2013-08-23T08:05:00.000u300",
                "2013-08-23T08:05:00.000u500",
                "2013-08-23T08:05:00.001u200",
                "2013-08-23T08:05:00.001u201",
            });

        runSequenceMicro(env, "2013-08-23T08:05:00.000u373",
            "select * from pattern [ every timer:at(*, *, *, *, *, *, *, * / 5, 0) ]",
            new String[]{
                "2013-08-23T08:05:00.005u000",
                "2013-08-23T08:05:00.010u000",
                "2013-08-23T08:05:00.015u000",
                "2013-08-23T08:05:00.020u000"
            });

        runSequenceMicro(env, "2013-08-23T08:05:00.000u373",
            "select * from pattern [ every timer:at(*, *, *, *, *, * / 5, *, 0, 373) ]",
            new String[]{
                "2013-08-23T08:05:05.000u373",
                "2013-08-23T08:05:10.000u373",
                "2013-08-23T08:05:15.000u373",
                "2013-08-23T08:05:20.000u373"
            });

        runSequenceMicro(env, "2013-08-23T08:05:00.000u000",
            "select * from pattern [ every timer:at(10, 9, *, *, *, 2, *, 373, 243) ]",
            new String[]{
                "2013-08-23T09:10:02.373u243",
                "2013-08-24T09:10:02.373u243",
                "2013-08-25T09:10:02.373u243"
            });
    }

    private static void runSequenceMicro(RegressionEnvironment env, String startTime, String epl, String[] times) {
        // Comment-me-in: System.out.println("Start from " + startTime);
        env.advanceTime(parseWithMicro(startTime));

        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        runSequenceMilliseconds(env, times);

        env.undeployAll();
    }

    private static long parseWithMicro(String startTime) {
        String[] parts = startTime.split("u");
        long millis = DateTime.parseDefaultMSec(parts[0]);
        int micro = Integer.parseInt(parts[1]);
        return 1000 * millis + micro;
    }

    private static String printMicro(long time) {
        return DateTime.print(time / 1000) + " u" + time % 1000;
    }

    private static void runSequenceMilliseconds(RegressionEnvironment env, String[] times) {
        for (String next : times) {
            // send right-before time
            long nextLong = parseWithMicro(next);
            env.advanceTime(nextLong - 1);
            // Comment-me-in: System.out.println("Advance to " + printMicro(nextLong));
            assertFalse("unexpected callback at " + next, env.listener("s0").isInvoked());

            // send right-after time
            env.advanceTime(nextLong);
            // Comment-me-in: System.out.println("Advance to " + printMicro(nextLong));
            assertTrue("missing callback at " + next, env.listener("s0").getAndClearIsInvoked());
        }
    }
}
