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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertNull;

public class ExprDTToDateCalMSec {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprToCalendarChain());
        executions.add(new ExprDTToDateCalMSecValue());
        return executions;
    }

    public static class ExprToCalendarChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            env.compileDeploy("@name('s0') select current_timestamp.toCalendar().add(Calendar.DAY_OF_MONTH,1) as c from SupportBean");
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("c"));

            env.undeployAll();
        }
    }

    public static class ExprDTToDateCalMSecValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("select current_timestamp.toCalendar().add(Calendar.DAY_OF_MONTH,1) from SupportBean");

            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7,val8,val9,val10,val11,val12,val13,val14,val15,val16,val17".split(",");
            String eplFragment = "@name('s0') select " +
                "current_timestamp.toDate() as val0," +
                "utildate.toDate() as val1," +
                "longdate.toDate() as val2," +
                "caldate.toDate() as val3," +
                "localdate.toDate() as val4," +
                "zoneddate.toDate() as val5," +
                "current_timestamp.toCalendar() as val6," +
                "utildate.toCalendar() as val7," +
                "longdate.toCalendar() as val8," +
                "caldate.toCalendar() as val9," +
                "localdate.toCalendar() as val10," +
                "zoneddate.toCalendar() as val11," +
                "current_timestamp.toMillisec() as val12," +
                "utildate.toMillisec() as val13," +
                "longdate.toMillisec() as val14," +
                "caldate.toMillisec() as val15," +
                "localdate.toMillisec() as val16," +
                "zoneddate.toMillisec() as val17" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Date.class, Date.class, Date.class, Date.class, Date.class, Date.class,
                Calendar.class, Calendar.class, Calendar.class, Calendar.class, Calendar.class, Calendar.class,
                Long.class, Long.class, Long.class, Long.class, Long.class, Long.class});

            env.sendEventBean(SupportDateTime.make(startTime));
            Object[] expectedUtil = SupportDateTime.getArrayCoerced(startTime, "util", "util", "util", "util", "util", "util");
            Object[] expectedCal = SupportDateTime.getArrayCoerced(startTime, "cal", "cal", "cal", "cal", "cal", "cal");
            Object[] expectedMsec = SupportDateTime.getArrayCoerced(startTime, "long", "long", "long", "long", "long", "long");
            Object[] expected = EPAssertionUtil.concatenateArray(expectedUtil, expectedCal, expectedMsec);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

            env.sendEventBean(SupportDateTime.make(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{
                SupportDateTime.getValueCoerced(startTime, "util"), null, null, null, null, null,
                SupportDateTime.getValueCoerced(startTime, "cal"), null, null, null, null, null,
                SupportDateTime.getValueCoerced(startTime, "long"), null, null, null, null, null});

            env.undeployAll();
        }
    }
}
