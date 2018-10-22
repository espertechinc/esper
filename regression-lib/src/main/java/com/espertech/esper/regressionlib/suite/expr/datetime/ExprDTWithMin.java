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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

public class ExprDTWithMin {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTWithMinInput());
        executions.add(new ExprDTWithMinFields());
        return executions;
    }

    private static class ExprDTWithMinInput implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.withMin('month') as val0," +
                "longdate.withMin('month') as val1," +
                "caldate.withMin('month') as val2," +
                "localdate.withMin('month') as val3," +
                "zoneddate.withMin('month') as val4" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

            String startTime = "2002-05-30T09:00:00.000";
            String expectedTime = "2002-01-30T09:00:00.000";
            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "util", "long", "cal", "ldt", "zdt"));

            env.undeployAll();
        }
    }

    private static class ExprDTWithMinFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.withMin('msec') as val0," +
                "utildate.withMin('sec') as val1," +
                "utildate.withMin('minutes') as val2," +
                "utildate.withMin('hour') as val3," +
                "utildate.withMin('day') as val4," +
                "utildate.withMin('month') as val5," +
                "utildate.withMin('year') as val6," +
                "utildate.withMin('week') as val7" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Date.class, Date.class, Date.class, Date.class, Date.class, Date.class, Date.class, Date.class});

            String[] expected = {
                "2002-05-30T09:01:02.000",
                "2002-05-30T09:01:00.003",
                "2002-05-30T09:00:02.003",
                "2002-05-30T00:01:02.003",
                "2002-05-01T09:01:02.003",
                "2002-01-30T09:01:02.003",
                "0001-05-30T09:01:02.003",
                "2002-01-03T09:01:02.003",
            };
            String startTime = "2002-05-30T09:01:02.003";
            env.sendEventBean(SupportDateTime.make(startTime));
            //System.out.println("===> " + SupportDateTime.print(env.listener("s0").assertOneGetNew().get("val7")));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expected, "util"));

            env.undeployAll();
        }
    }
}
