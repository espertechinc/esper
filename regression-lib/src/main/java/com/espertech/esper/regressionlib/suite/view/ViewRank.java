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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.util.SupportBeanAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

public class ViewRank {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewRankedSceneOne());
        execs.add(new ViewRankedPrev());
        execs.add(new ViewRankPrevAndGroupWin());
        execs.add(new ViewRankMultiexpression());
        execs.add(new ViewRankRemoveStream());
        execs.add(new ViewRankRanked());
        execs.add(new ViewRankInvalid());
        return execs;
    }

    public static class ViewRankedPrev implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select prevwindow(ev) as win, prev(0, ev) as prev0, prev(1, ev) as prev1, prev(2, ev) as prev2, prev(3, ev) as prev3, prev(4, ev) as prev4 " +
                "from SupportBean#rank(theString, 3, intPrimitive) as ev";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(makeEvent("E1", 100, 0L));
            assertWindowAggAndPrev(env, new Object[][]{{"E1", 100, 0L}});

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 99, 0L));
            assertWindowAggAndPrev(env, new Object[][]{{"E2", 99, 0L}, {"E1", 100, 0L}});

            env.sendEventBean(makeEvent("E1", 98, 1L));
            assertWindowAggAndPrev(env, new Object[][]{{"E1", 98, 1L}, {"E2", 99, 0L}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E3", 98, 0L));
            assertWindowAggAndPrev(env, new Object[][]{{"E1", 98, 1L}, {"E3", 98, 0L}, {"E2", 99, 0L}});

            env.milestone(2);

            env.sendEventBean(makeEvent("E2", 97, 1L));
            assertWindowAggAndPrev(env, new Object[][]{{"E2", 97, 1L}, {"E1", 98, 1L}, {"E3", 98, 0L}});

            env.undeployAll();
        }
    }

    private static class ViewRankPrevAndGroupWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select prevwindow(ev) as win, prev(0, ev) as prev0, prev(1, ev) as prev1, prev(2, ev) as prev2, prev(3, ev) as prev3, prev(4, ev) as prev4 " +
                "from SupportBean#rank(theString, 3, intPrimitive) as ev";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(makeEvent("E1", 100, 0L));
            assertWindowAggAndPrev(env, new Object[][]{{"E1", 100, 0L}});

            env.sendEventBean(makeEvent("E2", 99, 0L));
            assertWindowAggAndPrev(env, new Object[][]{{"E2", 99, 0L}, {"E1", 100, 0L}});

            env.sendEventBean(makeEvent("E1", 98, 1L));
            assertWindowAggAndPrev(env, new Object[][]{{"E1", 98, 1L}, {"E2", 99, 0L}});

            env.sendEventBean(makeEvent("E3", 98, 0L));
            assertWindowAggAndPrev(env, new Object[][]{{"E1", 98, 1L}, {"E3", 98, 0L}, {"E2", 99, 0L}});

            env.sendEventBean(makeEvent("E2", 97, 1L));
            assertWindowAggAndPrev(env, new Object[][]{{"E2", 97, 1L}, {"E1", 98, 1L}, {"E3", 98, 0L}});
            env.undeployAll();

            epl = "@name('s0') select irstream * from SupportBean#groupwin(theString)#rank(intPrimitive, 2, doublePrimitive) as ev";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            String[] fields = "theString,intPrimitive,longPrimitive,doublePrimitive".split(",");
            env.sendEventBean(makeEvent("E1", 100, 0L, 1d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 100, 0L, 1d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 0L, 1d}});

            env.sendEventBean(makeEvent("E2", 100, 0L, 2d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, 0L, 2d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 0L, 1d}, {"E2", 100, 0L, 2d}});

            env.sendEventBean(makeEvent("E1", 200, 0L, 0.5d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 200, 0L, 0.5d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 200, 0L, 0.5d}, {"E1", 100, 0L, 1d}, {"E2", 100, 0L, 2d}});

            env.sendEventBean(makeEvent("E2", 200, 0L, 2.5d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 200, 0L, 2.5d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 200, 0L, 0.5d}, {"E1", 100, 0L, 1d}, {"E2", 100, 0L, 2d}, {"E2", 200, 0L, 2.5d}});

            env.sendEventBean(makeEvent("E1", 300, 0L, 0.1d));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E1", 300, 0L, 0.1d}, new Object[]{"E1", 100, 0L, 1d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 300, 0L, 0.1d}, {"E1", 200, 0L, 0.5d}, {"E2", 100, 0L, 2d}, {"E2", 200, 0L, 2.5d}});

            env.undeployAll();
        }
    }

    private static class ViewRankMultiexpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive,longPrimitive,doublePrimitive".split(",");
            String epl = "@name('s0') select irstream * from SupportBean#rank(theString, intPrimitive, 3, longPrimitive, doublePrimitive)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(makeEvent("E1", 100, 1L, 10d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 100, 1L, 10d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 1L, 10d}});

            env.sendEventBean(makeEvent("E1", 200, 1L, 9d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 200, 1L, 9d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 200, 1L, 9d}, {"E1", 100, 1L, 10d}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E1", 150, 1L, 11d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 150, 1L, 11d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 200, 1L, 9d}, {"E1", 100, 1L, 10d}, {"E1", 150, 1L, 11d}});

            env.sendEventBean(makeEvent("E1", 100, 1L, 8d));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E1", 100, 1L, 8d}, new Object[]{"E1", 100, 1L, 10d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E1", 200, 1L, 9d}, {"E1", 150, 1L, 11d}});

            env.milestone(2);

            env.sendEventBean(makeEvent("E2", 300, 2L, 7d));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E2", 300, 2L, 7d}, new Object[]{"E2", 300, 2L, 7d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E1", 200, 1L, 9d}, {"E1", 150, 1L, 11d}});

            env.sendEventBean(makeEvent("E3", 300, 1L, 8.5d));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E3", 300, 1L, 8.5d}, new Object[]{"E1", 150, 1L, 11d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E3", 300, 1L, 8.5d}, {"E1", 200, 1L, 9d}});

            env.milestone(3);

            env.sendEventBean(makeEvent("E4", 400, 1L, 9d));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E4", 400, 1L, 9d}, new Object[]{"E1", 200, 1L, 9d});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E3", 300, 1L, 8.5d}, {"E4", 400, 1L, 9d}});

            env.undeployAll();
        }
    }

    private static class ViewRankRemoveStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive,longPrimitive".split(",");
            String epl = "@name('create') create window MyWindow#rank(theString, 3, intPrimitive asc) as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "@name('s0') select irstream * from MyWindow;\n" +
                "on SupportBean_A delete from MyWindow mw where theString = id;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(makeEvent("E1", 10, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E1", 10, 0L}});

            env.sendEventBean(makeEvent("E2", 50, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 50, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E1", 10, 0L}, {"E2", 50, 0L}});

            env.sendEventBean(makeEvent("E3", 5, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 5, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E3", 5, 0L}, {"E1", 10, 0L}, {"E2", 50, 0L}});

            env.sendEventBean(makeEvent("E4", 5, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E4", 5, 0L}, new Object[]{"E2", 50, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E3", 5, 0L}, {"E4", 5, 0L}, {"E1", 10, 0L}});

            env.sendEventBean(new SupportBean_A("E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E3", 5, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E4", 5, 0L}, {"E1", 10, 0L}});

            env.sendEventBean(new SupportBean_A("E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E4", 5, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E1", 10, 0L}});

            env.sendEventBean(new SupportBean_A("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E1", 10, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[0][]);

            env.sendEventBean(makeEvent("E3", 100, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 100, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E3", 100, 0L}});

            env.sendEventBean(makeEvent("E3", 101, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E3", 101, 1L}, new Object[]{"E3", 100, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[][]{{"E3", 101, 1L}});

            env.sendEventBean(new SupportBean_A("E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E3", 101, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("create").iterator(), fields, new Object[0][]);

            env.undeployAll();
        }
    }

    public static class ViewRankedSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive,longPrimitive".split(",");
            String epl = "@Name('s0') select irstream * from SupportBean.ext:rank(theString, 3, intPrimitive)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            sendSupportBean(env, "A", 10, 100L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 10, 100L});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 10, 100L}});
            sendSupportBean(env, "B", 20, 101L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 20, 101L});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 10, 100L}, {"B", 20, 101L}});
            sendSupportBean(env, "A", 8, 102L);  // replace A
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"A", 8, 102L}, new Object[]{"A", 10, 100L});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 8, 102L}, {"B", 20, 101L}});
            sendSupportBean(env, "C", 15, 103L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C", 15, 103L});

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 8, 102L}, {"C", 15, 103L}, {"B", 20, 101L}});
            sendSupportBean(env, "D", 21, 104L);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"D", 21, 104L}, new Object[]{"D", 21, 104L});

            env.milestone(5);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 8, 102L}, {"C", 15, 103L}, {"B", 20, 101L}});
            sendSupportBean(env, "A", 16, 105L);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"A", 16, 105L}, new Object[]{"A", 8, 102L});

            env.milestone(6);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"C", 15, 103L}, {"A", 16, 105L}, {"B", 20, 101L}});
            sendSupportBean(env, "C", 16, 106L);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"C", 16, 106L}, new Object[]{"C", 15, 103L});

            env.milestone(7);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 16, 105L}, {"C", 16, 106L}, {"B", 20, 101L}});
            sendSupportBean(env, "C", 16, 107L);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"C", 16, 107L}, new Object[]{"C", 16, 106L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 16, 105L}, {"C", 16, 107L}, {"B", 20, 101L}});
            sendSupportBean(env, "E", 1, 108L);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E", 1, 108L}, new Object[]{"B", 20, 101L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E", 1, 108L}, {"A", 16, 105L}, {"C", 16, 107L}});

            env.undeployAll();
        }
    }

    private static class ViewRankRanked implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive,longPrimitive".split(",");
            String epl = "@name('s0') select irstream * from SupportBean#rank(theString, 4, intPrimitive desc)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("E1", 10, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 10, 0L}});

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 30, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 30, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 30, 0L}, {"E1", 10, 0L}});

            env.sendEventBean(makeEvent("E1", 50, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E1", 50, 0L}, new Object[]{"E1", 10, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 50, 0L}, {"E2", 30, 0L}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E3", 40, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 40, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 50, 0L}, {"E3", 40, 0L}, {"E2", 30, 0L}});

            env.sendEventBean(makeEvent("E2", 45, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E2", 45, 0L}, new Object[]{"E2", 30, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 50, 0L}, {"E2", 45, 0L}, {"E3", 40, 0L}});

            env.milestone(2);

            env.sendEventBean(makeEvent("E1", 43, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E1", 43, 0L}, new Object[]{"E1", 50, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E3", 40, 0L}});

            env.sendEventBean(makeEvent("E3", 50, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E3", 50, 0L}, new Object[]{"E3", 40, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3", 50, 0L}, {"E2", 45, 0L}, {"E1", 43, 0L}});

            env.milestone(3);

            env.sendEventBean(makeEvent("E3", 10, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E3", 10, 0L}, new Object[]{"E3", 50, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E3", 10, 0L}});

            env.sendEventBean(makeEvent("E4", 43, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", 43, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E4", 43, 0L}, {"E3", 10, 0L}});

            env.milestone(4);

            // in-place replacement
            env.sendEventBean(makeEvent("E4", 43, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E4", 43, 1L}, new Object[]{"E4", 43, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E4", 43, 1L}, {"E3", 10, 0L}});

            env.sendEventBean(makeEvent("E2", 45, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E2", 45, 1L}, new Object[]{"E2", 45, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E1", 43, 0L}, {"E4", 43, 1L}, {"E3", 10, 0L}});

            env.milestone(5);

            env.sendEventBean(makeEvent("E1", 43, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E1", 43, 1L}, new Object[]{"E1", 43, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E3", 10, 0L}});

            // out-of-space: pushing out the back end
            env.sendEventBean(makeEvent("E5", 10, 2L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E5", 10, 2L}, new Object[]{"E3", 10, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E5", 10, 2L}});

            env.milestone(6);

            env.sendEventBean(makeEvent("E5", 11, 3L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E5", 11, 3L}, new Object[]{"E5", 10, 2L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E5", 11, 3L}});

            env.sendEventBean(makeEvent("E6", 43, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E6", 43, 0L}, new Object[]{"E5", 11, 3L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E6", 43, 0L}});

            env.milestone(7);

            env.sendEventBean(makeEvent("E7", 50, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E7", 50, 0L}, new Object[]{"E4", 43, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7", 50, 0L}, {"E2", 45, 1L}, {"E1", 43, 1L}, {"E6", 43, 0L}});

            env.sendEventBean(makeEvent("E8", 45, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E8", 45, 0L}, new Object[]{"E1", 43, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7", 50, 0L}, {"E2", 45, 1L}, {"E8", 45, 0L}, {"E6", 43, 0L}});

            env.milestone(8);

            env.sendEventBean(makeEvent("E8", 46, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E8", 46, 1L}, new Object[]{"E8", 45, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7", 50, 0L}, {"E8", 46, 1L}, {"E2", 45, 1L}, {"E6", 43, 0L}});

            env.undeployAll();
        }
    }

    private static class ViewRankInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#rank(1, intPrimitive desc)",
                "Failed to validate data window declaration: rank view requires a list of expressions providing unique keys, a numeric size parameter and a list of expressions providing sort keys [select * from SupportBean#rank(1, intPrimitive desc)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#rank(1, intPrimitive, theString desc)",
                "Failed to validate data window declaration: Failed to find unique value expressions that are expected to occur before the numeric size parameter [select * from SupportBean#rank(1, intPrimitive, theString desc)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#rank(theString, intPrimitive, 1)",
                "Failed to validate data window declaration: Failed to find sort key expressions after the numeric size parameter [select * from SupportBean#rank(theString, intPrimitive, 1)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#rank(theString, intPrimitive, theString desc)",
                "Failed to validate data window declaration: Failed to find constant value for the numeric size parameter [select * from SupportBean#rank(theString, intPrimitive, theString desc)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#rank(theString, 1, 1, intPrimitive, theString desc)",
                "Failed to validate data window declaration: Invalid view parameter expression 2 for rank view, the expression returns a constant result value, are you sure? [select * from SupportBean#rank(theString, 1, 1, intPrimitive, theString desc)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#rank(theString, intPrimitive, 1, intPrimitive, 1, theString desc)",
                "Failed to validate data window declaration: Invalid view parameter expression 4 for rank view, the expression returns a constant result value, are you sure? [select * from SupportBean#rank(theString, intPrimitive, 1, intPrimitive, 1, theString desc)]");
        }
    }


    private static void assertWindowAggAndPrev(RegressionEnvironment env, Object[][] expected) {
        String[] fields = "theString,intPrimitive,longPrimitive".split(",");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        SupportBeanAssertionUtil.assertPropsPerRow((Object[]) event.get("win"), fields, expected);
        for (int i = 0; i < 5; i++) {
            Object prevValue = event.get("prev" + i);
            if (prevValue == null && expected.length <= i) {
                continue;
            }
            SupportBeanAssertionUtil.assertPropsBean((SupportBean) prevValue, fields, expected[i]);
        }
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        return makeEvent(theString, intPrimitive, longPrimitive, 0d);
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        return bean;
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }
}