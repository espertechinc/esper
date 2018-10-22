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
import java.util.Calendar;
import java.util.Date;

public class ExprDTNested implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String[] fields = "val0,val1,val2,val3,val4".split(",");
        String eplFragment = "@name('s0') select " +
            "utildate.set('hour', 1).set('minute', 2).set('second', 3) as val0," +
            "longdate.set('hour', 1).set('minute', 2).set('second', 3) as val1," +
            "caldate.set('hour', 1).set('minute', 2).set('second', 3) as val2," +
            "localdate.set('hour', 1).set('minute', 2).set('second', 3) as val3," +
            "zoneddate.set('hour', 1).set('minute', 2).set('second', 3) as val4" +
            " from SupportDateTime";
        env.compileDeploy(eplFragment).addListener("s0");
        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

        String startTime = "2002-05-30T09:00:00.000";
        String expectedTime = "2002-05-30T01:02:03.000";
        env.sendEventBean(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "util", "long", "cal", "ldt", "zdt"));

        env.undeployAll();

        eplFragment = "@name('s0') select " +
            "utildate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val0," +
            "longdate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val1," +
            "caldate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val2," +
            "localdate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val3," +
            "zoneddate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val4" +
            " from SupportDateTime";
        env.compileDeployAddListenerMile(eplFragment, "s0", 1);
        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Calendar.class, Calendar.class, Calendar.class, Calendar.class, Calendar.class});

        env.sendEventBean(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "cal", "cal", "cal", "cal", "cal"));

        env.undeployAll();
    }
}
