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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.type.EPTypePremade.*;

public class ExprDTSet {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTSetInput());
        executions.add(new ExprDTSetFields());
        return executions;
    }

    private static class ExprDTSetInput implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.set('month', 0) as val0," +
                "longdate.set('month', 0) as val1," +
                "caldate.set('month', 0) as val2," +
                "localdate.set('month', 1) as val3," +
                "zoneddate.set('month', 1) as val4" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            SupportEventPropUtil.assertTypes(env.statement("s0").getEventType(), fields, new EPTypeClass[]{DATE.getEPType(), LONGBOXED.getEPType(), CALENDAR.getEPType(), LOCALDATETIME.getEPType(), ZONEDDATETIME.getEPType()});

            String startTime = "2002-05-30T09:00:00.000";
            String expectedTime = "2002-1-30T09:00:00.000";
            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "util", "long", "cal", "ldt", "zdt"));

            env.undeployAll();
        }
    }

    private static class ExprDTSetFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.set('msec', 1) as val0," +
                "utildate.set('sec', 2) as val1," +
                "utildate.set('minutes', 3) as val2," +
                "utildate.set('hour', 13) as val3," +
                "utildate.set('day', 5) as val4," +
                "utildate.set('month', 6) as val5," +
                "utildate.set('year', 7) as val6," +
                "utildate.set('week', 8) as val7" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            SupportEventPropUtil.assertTypesAllSame(env.statement("s0").getEventType(), fields, DATE.getEPType());

            String[] expected = {
                "2002-05-30T09:00:00.001",
                "2002-05-30T09:00:02.000",
                "2002-05-30T09:03:00.000",
                "2002-05-30T13:00:00.000",
                "2002-05-5T09:00:00.000",
                "2002-07-30T09:00:00.000",
                "0007-05-30T09:00:00.000",
                "2002-02-21T09:00:00.000",
            };
            String startTime = "2002-05-30T09:00:00.000";
            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expected, "util"));

            env.undeployAll();
        }
    }
}
