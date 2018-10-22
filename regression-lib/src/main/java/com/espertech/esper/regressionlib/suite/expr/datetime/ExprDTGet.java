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
import com.espertech.esper.regressionlib.support.bean.SupportEventWithJustGet;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndA;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

public class ExprDTGet {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTGetFields());
        executions.add(new ExprDTGetInput());
        return executions;
    }

    private static class ExprDTGetInput implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4".split(",");
            String epl = "@name('s0') select " +
                "utildate.get('month') as val0," +
                "longdate.get('month') as val1," +
                "caldate.get('month') as val2, " +
                "localdate.get('month') as val3, " +
                "zoneddate.get('month') as val4 " +
                " from SupportDateTime";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class, Integer.class, Integer.class, Integer.class});

            String startTime = "2002-05-30T09:00:00.000";
            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, 4, 4, 5, 5});

            env.undeployAll();

            // try event as input
            epl = "@name('s0') select abc.get('month') as val0 from SupportTimeStartEndA as abc";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            env.sendEventBean(SupportTimeStartEndA.make("A0", startTime, 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{4});

            env.undeployAll();

            // test "get" method on object is preferred
            epl = "@name('s0') select e.get() as c0, e.get('abc') as c1 from SupportEventWithJustGet as e";
            env.compileDeployAddListenerMile(epl, "s0", 1);
            env.sendEventBean(new SupportEventWithJustGet());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{1, 2});

            env.undeployAll();
        }
    }

    private static class ExprDTGetFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7".split(",");
            String eplFragment = "@name('s0') select " +
                "utildate.get('msec') as val0," +
                "utildate.get('sec') as val1," +
                "utildate.get('minutes') as val2," +
                "utildate.get('hour') as val3," +
                "utildate.get('day') as val4," +
                "utildate.get('month') as val5," +
                "utildate.get('year') as val6," +
                "utildate.get('week') as val7" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class});

            String startTime = "2002-05-30T09:01:02.003";
            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 2, 1, 9, 30, 4, 2002, 22});

            env.undeployAll();
        }
    }

}
