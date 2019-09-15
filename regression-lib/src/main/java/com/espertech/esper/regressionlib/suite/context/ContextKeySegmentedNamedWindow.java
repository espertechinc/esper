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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportGroupSubgroupEvent;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertTrue;

public class ContextKeySegmentedNamedWindow {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextKeyedNamedWindowBasic());
        execs.add(new ContextKeyedNamedWindowNonPattern());
        execs.add(new ContextKeyedNamedWindowPattern());
        execs.add(new ContextKeyedNamedWindowFAF());
        execs.add(new ContextKeyedSubqueryNamedWindowIndexUnShared());
        execs.add(new ContextKeyedSubqueryNamedWindowIndexShared());
        return execs;
    }

    private static class ContextKeyedNamedWindowFAF implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context SegmentedByString partition by theString from SupportBean", path);
            env.compileDeploy("context SegmentedByString create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("context SegmentedByString insert into MyWindow select * from SupportBean", path);
            EPCompiled compiled = env.compileFAF("select * from MyWindow", path);

            env.sendEventBean(new SupportBean("G1", 0));

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRow(env.runtime().getFireAndForgetService().executeQuery(compiled).getArray(), "theString".split(","), new Object[][]{{"G1"}});

            env.sendEventBean(new SupportBean("G2", 0));

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.runtime().getFireAndForgetService().executeQuery(compiled).getArray(), "theString".split(","), new Object[][]{{"G1"}, {"G2"}});

            env.undeployAll();
        }
    }

    private static class ContextKeyedNamedWindowBasic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // ESPER-663
            String epl =
                "@Audit @Name('CTX') create context Ctx partition by grp, subGrp from SupportGroupSubgroupEvent;\n" +
                    "@Audit @Name('Window') context Ctx create window EventData#unique(type) as SupportGroupSubgroupEvent;" +
                    "@Audit @Name('Insert') context Ctx insert into EventData select * from SupportGroupSubgroupEvent;" +
                    "@Audit @Name('Test') context Ctx select irstream * from EventData;";
            env.compileDeploy(epl);
            env.addListener("Test");
            env.sendEventBean(new SupportGroupSubgroupEvent("G1", "SG1", 1, 10.45));
            assertTrue(env.listener("Test").isInvoked());
            env.undeployAll();
        }
    }

    private static class ContextKeyedNamedWindowNonPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionNamedWindow(env, "MyWindow as a");
        }
    }

    private static class ContextKeyedNamedWindowPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionNamedWindow(env, "pattern [every a=MyWindow]");
        }
    }

    private static class ContextKeyedSubqueryNamedWindowIndexShared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);
            env.compileDeploy("@Hint('enable_window_subquery_indexshare') create window MyWindowTwo#keepall as SupportBean_S0", path);
            env.compileDeploy("insert into MyWindowTwo select * from SupportBean_S0", path);

            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from MyWindowTwo as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb", path);
            env.addListener("s0");

            tryAssertionSubqueryNW(env);

            env.undeployAll();
        }
    }

    private static class ContextKeyedSubqueryNamedWindowIndexUnShared implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@Name('context') create context SegmentedByString partition by theString from SupportBean;\n" +
                "create window MyWindowThree#keepall as SupportBean_S0;\n" +
                "insert into MyWindowThree select * from SupportBean_S0;\n" +
                "@Name('s0') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from MyWindowThree as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb;\n";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionSubqueryNW(env);

            env.undeployAll();
        }
    }

    private static void tryAssertionSubqueryNW(RegressionEnvironment env) {
        String[] fields = new String[]{"theString", "intPrimitive", "val0"};

        env.sendEventBean(new SupportBean_S0(10, "s1"));
        env.sendEventBean(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "s1"});

        env.milestone(0);

        env.sendEventBean(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 10, "s1"});

        env.sendEventBean(new SupportBean("G3", 20));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G3", 20, null});

        env.milestone(1);

        env.sendEventBean(new SupportBean_S0(20, "s2"));
        env.sendEventBean(new SupportBean("G3", 20));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G3", 20, "s2"});

        env.sendEventBean(new SupportBean("G1", 20));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 20, "s2"});
    }

    private static void runAssertionNamedWindow(RegressionEnvironment env, String fromClause) {
        RegressionPath path = new RegressionPath();
        String epl = "create context Ctx partition by theString from SupportBean;\n" +
            "@name('window') context Ctx create window MyWindow#keepall as SupportBean;" +
            "@name('insert') context Ctx insert into MyWindow select * from SupportBean;" +
            "@name('s0') context Ctx select irstream context.key1 as c0, a.intPrimitive as c1 from " + fromClause;
        env.compileDeploy(epl, path).addListener("s0");
        String[] fields = "c0,c1".split(",");

        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        env.sendEventBean(new SupportBean("E1", 3));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

        env.sendEventBean(new SupportBean("E2", 4));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 4});

        tryInvalidCreateWindow(env, path);
        tryInvalidCreateWindow(env, path); // making sure all is cleaned up

        env.undeployAll();
    }

    private static void tryInvalidCreateWindow(RegressionEnvironment env, RegressionPath path) {
        tryInvalidCompile(env, path, "context Ctx create window MyInvalidWindow#unique(p00) as SupportBean_S0",
            "Segmented context 'Ctx' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [context Ctx create window MyInvalidWindow#unique(p00) as SupportBean_S0]");
    }
}
