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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

public class ExprDTPlusMinus {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTPlusMinusSimple());
        executions.add(new ExprDTPlusMinusTimePeriod());
        return executions;
    }

    private static class ExprDTPlusMinusSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('var') create variable long varmsec", path);
            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7,val8,val9,val10,val11".split(",");
            String epl = "@name('s0') select " +
                "current_timestamp.plus(varmsec) as val0," +
                "utildate.plus(varmsec) as val1," +
                "longdate.plus(varmsec) as val2," +
                "caldate.plus(varmsec) as val3," +
                "localdate.plus(varmsec) as val4," +
                "zoneddate.plus(varmsec) as val5," +
                "current_timestamp.minus(varmsec) as val6," +
                "utildate.minus(varmsec) as val7," +
                "longdate.minus(varmsec) as val8," +
                "caldate.minus(varmsec) as val9," +
                "localdate.minus(varmsec) as val10," +
                "zoneddate.minus(varmsec) as val11" +
                " from SupportDateTime";
            env.compileDeploy(epl, path).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Long.class, Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class,
                Long.class, Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

            env.sendEventBean(SupportDateTime.make(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "long"), null, null, null, null, null,
                SupportDateTime.getValueCoerced(startTime, "long"), null, null, null, null, null});

            Object[] expectedPlus = SupportDateTime.getArrayCoerced(startTime, "long", "util", "long", "cal", "ldt", "zdt");
            Object[] expectedMinus = SupportDateTime.getArrayCoerced(startTime, "long", "util", "long", "cal", "ldt", "zdt");
            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, EPAssertionUtil.concatenateArray(expectedPlus, expectedMinus));

            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "varmsec", 1000);
            env.sendEventBean(SupportDateTime.make(startTime));
            //System.out.println("===> " + SupportDateTime.print(env.listener("s0").assertOneGetNew().get("val4")));
            expectedPlus = SupportDateTime.getArrayCoerced("2002-05-30T09:00:01.000", "long", "util", "long", "cal", "ldt", "zdt");
            expectedMinus = SupportDateTime.getArrayCoerced("2002-05-30T08:59:59.000", "long", "util", "long", "cal", "ldt", "zdt");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, EPAssertionUtil.concatenateArray(expectedPlus, expectedMinus));

            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "varmsec", 2 * 24 * 60 * 60 * 1000);
            env.sendEventBean(SupportDateTime.make(startTime));
            expectedMinus = SupportDateTime.getArrayCoerced("2002-05-28T09:00:00.000", "long", "util", "long", "cal", "ldt", "zdt");
            expectedPlus = SupportDateTime.getArrayCoerced("2002-06-1T09:00:00.000", "long", "util", "long", "cal", "ldt", "zdt");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, EPAssertionUtil.concatenateArray(expectedPlus, expectedMinus));

            env.undeployAll();
        }
    }

    private static class ExprDTPlusMinusTimePeriod implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7,val8,val9,val10,val11".split(",");
            String eplFragment = "@name('s0') select " +
                "current_timestamp.plus(1 hour 10 sec 20 msec) as val0," +
                "utildate.plus(1 hour 10 sec 20 msec) as val1," +
                "longdate.plus(1 hour 10 sec 20 msec) as val2," +
                "caldate.plus(1 hour 10 sec 20 msec) as val3," +
                "localdate.plus(1 hour 10 sec 20 msec) as val4," +
                "zoneddate.plus(1 hour 10 sec 20 msec) as val5," +
                "current_timestamp.minus(1 hour 10 sec 20 msec) as val6," +
                "utildate.minus(1 hour 10 sec 20 msec) as val7," +
                "longdate.minus(1 hour 10 sec 20 msec) as val8," +
                "caldate.minus(1 hour 10 sec 20 msec) as val9," +
                "localdate.minus(1 hour 10 sec 20 msec) as val10," +
                "zoneddate.minus(1 hour 10 sec 20 msec) as val11" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Long.class, Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class,
                Long.class, Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

            env.sendEventBean(SupportDateTime.make(startTime));
            Object[] expectedPlus = SupportDateTime.getArrayCoerced("2002-05-30T010:00:10.020", "long", "util", "long", "cal", "ldt", "zdt");
            Object[] expectedMinus = SupportDateTime.getArrayCoerced("2002-05-30T07:59:49.980", "long", "util", "long", "cal", "ldt", "zdt");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, EPAssertionUtil.concatenateArray(expectedPlus, expectedMinus));

            env.sendEventBean(SupportDateTime.make(null));
            expectedPlus = SupportDateTime.getArrayCoerced("2002-05-30T010:00:10.020", "long", "null", "null", "null", "null", "null");
            expectedMinus = SupportDateTime.getArrayCoerced("2002-05-30T07:59:49.980", "long", "null", "null", "null", "null", "null");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, EPAssertionUtil.concatenateArray(expectedPlus, expectedMinus));

            env.undeployAll();
        }
    }
}