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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExprDTResolution {

    public static Collection<RegressionExecution> executions(boolean isMicrosecond) {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTResolutionEventTime(isMicrosecond));
        executions.add(new ExprDTLongProperty(isMicrosecond));
        return executions;
    }

    public static class ExprDTResolutionEventTime implements RegressionExecution {
        private final boolean isMicrosecond;

        public ExprDTResolutionEventTime(boolean isMicrosecond) {
            this.isMicrosecond = isMicrosecond;
        }

        public void run(RegressionEnvironment env) {
            long time = DateTime.parseDefaultMSec("2002-05-30T09:00:00.000");
            if (!isMicrosecond) {
                runAssertionEventTime(env, time, time);
            } else {
                runAssertionEventTime(env, time * 1000, time * 1000);
            }
        }
    }

    private static class ExprDTLongProperty implements RegressionExecution {
        private final boolean isMicrosecond;

        public ExprDTLongProperty(boolean isMicrosecond) {
            this.isMicrosecond = isMicrosecond;
        }

        public void run(RegressionEnvironment env) {
            long time = DateTime.parseDefaultMSec("2002-05-30T09:05:06.007");
            Calendar calTime = GregorianCalendar.getInstance();
            calTime.setTimeInMillis(time);

            Calendar calMod = GregorianCalendar.getInstance();
            calMod.setTimeInMillis(time);
            calMod.set(Calendar.HOUR_OF_DAY, 1);
            calMod.set(Calendar.MINUTE, 2);
            calMod.set(Calendar.SECOND, 3);
            calMod.set(Calendar.MILLISECOND, 4);

            String select =
                "longdate.withTime(1, 2, 3, 4) as c0," +
                    "longdate.set('hour', 1).set('minute', 2).set('second', 3).set('millisecond', 4).toCalendar() as c1," +
                    "longdate.get('month') as c2," +
                    "current_timestamp.get('month') as c3," +
                    "current_timestamp.getMinuteOfHour() as c4," +
                    "current_timestamp.toDate() as c5," +
                    "current_timestamp.toCalendar() as c6," +
                    "current_timestamp.minus(1) as c7";
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");

            if (!isMicrosecond) {
                runAssertionLongProperty(env, time, new SupportDateTime(time, null, null, null, null), select, fields,
                    new Object[]{calMod.getTimeInMillis(), calMod, 4, 4, 5, calTime.getTime(), calTime, time - 1});
            } else {
                runAssertionLongProperty(env, time * 1000, new SupportDateTime(time * 1000 + 123, null, null, null, null), select, fields,
                    new Object[]{calMod.getTimeInMillis() * 1000 + 123, calMod, 4, 4, 5, calTime.getTime(), calTime, time * 1000 - 1000});
            }
        }
    }

    private static void runAssertionLongProperty(RegressionEnvironment env, long startTime, SupportDateTime event, String select, String[] fields, Object[] expected) {
        env.advanceTime(startTime);

        String epl = "@name('s0') select " + select + " from SupportDateTime";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(event);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

        env.undeployAll();
    }

    private static void runAssertionEventTime(RegressionEnvironment env, long tsB, long flipTimeEndtsA) {

        env.advanceTime(0);
        String epl = "@name('s0') select * from MyEvent(id='A') as a unidirectional, MyEvent(id='B')#lastevent as b where a.withDate(2002, 4, 30).before(b)";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{"B", tsB, tsB}, "MyEvent");

        env.sendEventObjectArray(new Object[]{"A", flipTimeEndtsA - 1, flipTimeEndtsA - 1}, "MyEvent");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        env.sendEventObjectArray(new Object[]{"A", flipTimeEndtsA, flipTimeEndtsA}, "MyEvent");
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.undeployAll();
    }
}
