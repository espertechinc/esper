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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

public class ExprEnumCountOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumCountOfEvents());
        execs.add(new ExprEnumCountOfScalar());
        return execs;
    }

    private static class ExprEnumCountOfEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"val0", "val1"};
            String eplFragment = "@name('s0') select " +
                "contained.countof(x=> x.p00 = 9) as val0, " +
                "contained.countof() as val1 " +
                " from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,9"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{2, 3});

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{null, null});

            env.sendEventBean(SupportBean_ST0_Container.make2Value(new String[0]));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{0, 0});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,9"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1, 1});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{0, 1});

            env.undeployAll();
        }
    }

    private static class ExprEnumCountOfScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"val0", "val1"};
            String eplFragment = "@name('s0') select " +
                "strvals.countof() as val0, " +
                "strvals.countof(x => x = 'E1') as val1 " +
                " from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 1});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E1,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, 2});

            env.undeployAll();
        }
    }
}
