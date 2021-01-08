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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.type.EPTypePremade.*;

public class ExprDTWithMax {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTWithMaxInput());
        executions.add(new ExprDTWithMaxFields());
        return executions;
    }

    private static class ExprDTWithMaxInput implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.withMax('month') as val0," +
                "longdate.withMax('month') as val1," +
                "caldate.withMax('month') as val2," +
                "localdate.withMax('month') as val3," +
                "zoneddate.withMax('month') as val4" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            SupportEventPropUtil.assertTypes(env.statement("s0").getEventType(), fields, new EPTypeClass[]{DATE.getEPType(), LONGBOXED.getEPType(), CALENDAR.getEPType(), LOCALDATETIME.getEPType(), ZONEDDATETIME.getEPType()});

            String startTime = "2002-05-30T09:00:00.000";
            String expectedTime = "2002-12-30T09:00:00.000";
            env.sendEventBean(SupportDateTime.make(startTime));
            env.assertPropsNew("s0", fields, SupportDateTime.getArrayCoerced(expectedTime, "util", "long", "cal", "ldt", "zdt"));

            env.undeployAll();
        }
    }

    private static class ExprDTWithMaxFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.withMax('msec') as val0," +
                "utildate.withMax('sec') as val1," +
                "utildate.withMax('minutes') as val2," +
                "utildate.withMax('hour') as val3," +
                "utildate.withMax('day') as val4," +
                "utildate.withMax('month') as val5," +
                "utildate.withMax('year') as val6," +
                "utildate.withMax('week') as val7" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            SupportEventPropUtil.assertTypesAllSame(env.statement("s0").getEventType(), fields, DATE.getEPType());

            String[] expected = {
                "2002-5-30T09:00:00.999",
                "2002-5-30T09:00:59.000",
                "2002-5-30T09:59:00.000",
                "2002-5-30T23:00:00.000",
                "2002-5-31T09:00:00.000",
                "2002-12-30T09:00:00.000",
                "292278994-5-30T09:00:00.000",
                "2002-12-26T09:00:00.000"
            };
            String startTime = "2002-05-30T09:00:00.000";
            env.sendEventBean(SupportDateTime.make(startTime));
            env.assertPropsNew("s0", fields, SupportDateTime.getArrayCoerced(expected, "util"));

            env.undeployAll();
        }
    }
}
