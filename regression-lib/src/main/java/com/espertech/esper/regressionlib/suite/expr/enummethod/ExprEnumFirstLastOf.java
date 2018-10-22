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
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ExprEnumFirstLastOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumFirstLastScalar());
        execs.add(new ExprEnumFirstLastProperty());
        execs.add(new ExprEnumFirstLastNoPred());
        execs.add(new ExprEnumFirstLastPredicate());
        return execs;
    }

    private static class ExprEnumFirstLastScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.firstOf() as val0, " +
                "strvals.lastOf() as val1, " +
                "strvals.firstOf(x => x like '%1%') as val2, " +
                "strvals.lastOf(x => x like '%1%') as val3 " +
                " from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class, String.class, String.class, String.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", "E1", "E1"});

            env.sendEventBean(SupportCollection.makeString("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", "E1", "E1"});

            env.sendEventBean(SupportCollection.makeString("E2,E3,E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E4", null, null});

            env.sendEventBean(SupportCollection.makeString(""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.sendEventBean(SupportCollection.makeString(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ExprEnumFirstLastProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "contained.firstOf().p00 as val0, " +
                "contained.lastOf().p00 as val1 " +
                " from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 3});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 1});

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static class ExprEnumFirstLastNoPred implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select " +
                "contained.firstOf() as val0, " +
                "contained.lastOf() as val1 " +
                " from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");


            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0,val1".split(","), new Class[]{SupportBean_ST0.class, SupportBean_ST0.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E3,9", "E2,9"));
            assertId(env.listener("s0"), "val0", "E1");
            assertId(env.listener("s0"), "val1", "E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E2,2"));
            assertId(env.listener("s0"), "val0", "E2");
            assertId(env.listener("s0"), "val1", "E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            assertNull(env.listener("s0").assertOneGetNew().get("val0"));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val1"));

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            assertNull(env.listener("s0").assertOneGetNew().get("val0"));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val1"));

            env.undeployAll();
        }
    }

    private static class ExprEnumFirstLastPredicate implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select contained.firstOf(x => p00 = 9) as val from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");


            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{SupportBean_ST0.class});

            SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,9");
            env.sendEventBean(bean);
            SupportBean_ST0 result = (SupportBean_ST0) env.listener("s0").assertOneGetNewAndReset().get("val");
            assertSame(result, bean.getContained().get(1));

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E2,1"));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.undeployAll();
        }
    }

    private static void assertId(SupportListener listener, String property, String id) {
        SupportBean_ST0 result = (SupportBean_ST0) listener.assertOneGetNew().get(property);
        assertEquals(id, result.getId());
    }
}
