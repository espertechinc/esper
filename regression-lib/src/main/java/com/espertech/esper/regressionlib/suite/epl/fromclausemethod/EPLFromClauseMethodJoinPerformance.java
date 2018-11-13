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
package com.espertech.esper.regressionlib.suite.epl.fromclausemethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class EPLFromClauseMethodJoinPerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLFromClauseMethod1Stream2HistInnerJoinPerformance());
        execs.add(new EPLFromClauseMethod1Stream2HistOuterJoinPerformance());
        execs.add(new EPLFromClauseMethod2Stream1HistTwoSidedEntryIdenticalIndex());
        execs.add(new EPLFromClauseMethod2Stream1HistTwoSidedEntryMixedIndex());
        return execs;
    }

    private static class EPLFromClauseMethod1Stream2HistInnerJoinPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', 100) as h1 " +
                "where h0.index = p00 and h1.index = p00";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1".split(",");
            Random random = new Random();

            long start = System.currentTimeMillis();
            for (int i = 1; i < 5000; i++) {
                int num = random.nextInt(98) + 1;
                sendBeanInt(env, "E1", num);

                Object[][] result = new Object[][]{{"E1", "H0" + num, "H1" + num}};
                EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            }
            long end = System.currentTimeMillis();
            long delta = end - start;
            env.undeployAll();
            assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
        }
    }

    private static class EPLFromClauseMethod1Stream2HistOuterJoinPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0 " +
                " on h0.index = p00 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', 100) as h1 " +
                " on h1.index = p00";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1".split(",");
            Random random = new Random();

            long start = System.currentTimeMillis();
            for (int i = 1; i < 5000; i++) {
                int num = random.nextInt(98) + 1;
                sendBeanInt(env, "E1", num);

                Object[][] result = new Object[][]{{"E1", "H0" + num, "H1" + num}};
                EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            }
            long end = System.currentTimeMillis();
            long delta = end - start;
            env.undeployAll();
            assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
        }
    }

    private static class EPLFromClauseMethod2Stream1HistTwoSidedEntryIdenticalIndex implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as s0id, s1.id as s1id, h0.val as valh0 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0, " +
                "SupportBeanInt(id like 'F%')#lastevent as s1 " +
                "where h0.index = s0.p00 and h0.index = s1.p00";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "s0id,s1id,valh0".split(",");
            Random random = new Random();

            long start = System.currentTimeMillis();
            for (int i = 1; i < 1000; i++) {
                int num = random.nextInt(98) + 1;
                sendBeanInt(env, "E1", num);
                sendBeanInt(env, "F1", num);

                Object[][] result = new Object[][]{{"E1", "F1", "H0" + num}};
                EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);

                // send reset events to avoid duplicate matches
                sendBeanInt(env, "E1", 0);
                sendBeanInt(env, "F1", 0);
                env.listener("s0").reset();
            }
            long end = System.currentTimeMillis();
            long delta = end - start;
            assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2Stream1HistTwoSidedEntryMixedIndex implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as s0id, s1.id as s1id, h0.val as valh0, h0.index as indexh0 from " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0, " +
                "SupportBeanInt(id like 'H%')#lastevent as s1, " +
                "SupportBeanInt(id like 'E%')#lastevent as s0 " +
                "where h0.index = s0.p00 and h0.val = s1.id";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "s0id,s1id,valh0,indexh0".split(",");
            Random random = new Random();

            long start = System.currentTimeMillis();
            for (int i = 1; i < 1000; i++) {
                int num = random.nextInt(98) + 1;
                sendBeanInt(env, "E1", num);
                sendBeanInt(env, "H0" + num, num);

                Object[][] result = new Object[][]{{"E1", "H0" + num, "H0" + num, num}};
                EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);

                // send reset events to avoid duplicate matches
                sendBeanInt(env, "E1", 0);
                sendBeanInt(env, "F1", 0);
                env.listener("s0").reset();
            }
            long end = System.currentTimeMillis();
            long delta = end - start;
            env.undeployAll();
            assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
        }
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00, int p01, int p02, int p03) {
        env.sendEventBean(new SupportBeanInt(id, p00, p01, p02, p03, -1, -1));
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00) {
        sendBeanInt(env, id, p00, -1, -1, -1);
    }
}
