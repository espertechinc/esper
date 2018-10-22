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

public class PatternObserverTimerScheduleTimeZoneEST implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        sendCurrentTime(env, "2012-10-01T08:59:00.000GMT-04:00");

        String epl = "@name('s0') select * from pattern[timer:schedule(date: current_timestamp.withTime(9, 0, 0, 0))]";
        env.compileDeploy(epl).addListener("s0");

        sendCurrentTime(env, "2012-10-01T08:59:59.999GMT-4:00");
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        sendCurrentTime(env, "2012-10-01T09:00:00.000GMT-4:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        sendCurrentTime(env, "2012-10-03T09:00:00.000GMT-4:00");
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.undeployAll();
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSecWZone(time));
    }
}


