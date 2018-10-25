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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ResultSetQueryTypeRollupHavingAndOrderBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeHaving(false));
        execs.add(new ResultSetQueryTypeHaving(true));
        execs.add(new ResultSetQueryTypeIteratorWindow(false));
        execs.add(new ResultSetQueryTypeIteratorWindow(true));
        execs.add(new ResultSetQueryTypeOrderByTwoCriteriaAsc(false));
        execs.add(new ResultSetQueryTypeOrderByTwoCriteriaAsc(true));
        execs.add(new ResultSetQueryTypeUnidirectional());
        execs.add(new ResultSetQueryTypeOrderByOneCriteriaDesc());
        return execs;
    }

    private static class ResultSetQueryTypeIteratorWindow implements RegressionExecution {
        private final boolean join;

        public ResultSetQueryTypeIteratorWindow(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");
            String epl = "@Name('s0')" +
                "select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#length(3) " + (join ? ", SupportBean_S0#keepall " : "") +
                "group by rollup(theString)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {null, 1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {null, 3}});

            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 4}, {"E2", 2}, {null, 6}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 6}, {"E1", 3}, {null, 9}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnidirectional implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean_S0 unidirectional, SupportBean#keepall " +
                "group by cube(theString, intPrimitive)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("E1", 10, 100));
            env.sendEventBean(makeEvent("E2", 20, 200));
            env.sendEventBean(makeEvent("E1", 11, 300));
            env.sendEventBean(makeEvent("E2", 20, 400));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E1", 10, 100L},
                    {"E2", 20, 600L},
                    {"E1", 11, 300L},
                    {"E1", null, 400L},
                    {"E2", null, 600L},
                    {null, 10, 100L},
                    {null, 20, 600L},
                    {null, 11, 300L},
                    {null, null, 1000L}
                });

            env.sendEventBean(makeEvent("E1", 10, 1));
            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E1", 10, 101L},
                    {"E2", 20, 600L},
                    {"E1", 11, 300L},
                    {"E1", null, 401L},
                    {"E2", null, 600L},
                    {null, 10, 101L},
                    {null, 20, 600L},
                    {null, 11, 300L},
                    {null, null, 1001L}
                });

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeHaving implements RegressionExecution {
        private final boolean join;

        public ResultSetQueryTypeHaving(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {

            // test having on the aggregation alone
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#keepall " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive)" +
                "having sum(longPrimitive) > 1000";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 10, 100));
            env.sendEventBean(makeEvent("E2", 20, 200));
            env.sendEventBean(makeEvent("E1", 11, 300));
            env.sendEventBean(makeEvent("E2", 20, 400));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEvent("E1", 11, 500));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, null, 1500L}});

            env.sendEventBean(makeEvent("E2", 20, 600));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 1200L}, {"E2", null, 1200L}, {null, null, 2100L}});
            env.undeployAll();

            // test having on the aggregation alone
            String[] fieldsC0C1 = "c0,c1".split(",");
            epl = "@Name('s0')" +
                "select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#keepall " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString) " +
                "having " +
                "(theString is null and sum(intPrimitive) > 100) " +
                "or " +
                "(theString is not null and sum(intPrimitive) > 200)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(new SupportBean("E1", 50));
            env.sendEventBean(new SupportBean("E2", 50));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsC0C1,
                new Object[][]{{null, 120}});

            env.sendEventBean(new SupportBean("E3", -300));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E1", 200));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsC0C1,
                new Object[][]{{"E1", 250}});

            env.sendEventBean(new SupportBean("E2", 500));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsC0C1,
                new Object[][]{{"E2", 570}, {null, 520}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeOrderByTwoCriteriaAsc implements RegressionExecution {
        private final boolean join;

        public ResultSetQueryTypeOrderByTwoCriteriaAsc(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#time_batch(1 sec) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive) " +
                "order by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E2", 10, 100));
            env.sendEventBean(makeEvent("E1", 11, 200));

            env.milestone(0);

            env.sendEventBean(makeEvent("E1", 10, 300));
            env.sendEventBean(makeEvent("E1", 11, 400));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 1000L},
                    {"E1", null, 900L},
                    {"E1", 10, 300L},
                    {"E1", 11, 600L},
                    {"E2", null, 100L},
                    {"E2", 10, 100L},
                },
                new Object[][]{{null, null, null},
                    {"E1", null, null},
                    {"E1", 10, null},
                    {"E1", 11, null},
                    {"E2", null, null},
                    {"E2", 10, null},
                });

            env.sendEventBean(makeEvent("E1", 11, 500));
            env.sendEventBean(makeEvent("E1", 10, 600));
            env.sendEventBean(makeEvent("E1", 12, 700));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 1800L},
                    {"E1", null, 1800L},
                    {"E1", 10, 600L},
                    {"E1", 11, 500L},
                    {"E1", 12, 700L},
                    {"E2", null, null},
                    {"E2", 10, null},
                },
                new Object[][]{{null, null, 1000L},
                    {"E1", null, 900L},
                    {"E1", 10, 300L},
                    {"E1", 11, 600L},
                    {"E1", 12, null},
                    {"E2", null, 100L},
                    {"E2", 10, 100L},
                });

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeOrderByOneCriteriaDesc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#time_batch(1 sec) " +
                "group by rollup(theString, intPrimitive) " +
                "order by theString desc;";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("E2", 10, 100));
            env.sendEventBean(makeEvent("E1", 11, 200));
            env.sendEventBean(makeEvent("E1", 10, 300));

            env.milestone(0);

            env.sendEventBean(makeEvent("E1", 11, 400));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E2", 10, 100L},
                    {"E2", null, 100L},
                    {"E1", 11, 600L},
                    {"E1", 10, 300L},
                    {"E1", null, 900L},
                    {null, null, 1000L},
                },
                new Object[][]{
                    {"E2", 10, null},
                    {"E2", null, null},
                    {"E1", 11, null},
                    {"E1", 10, null},
                    {"E1", null, null},
                    {null, null, null},
                });

            env.sendEventBean(makeEvent("E1", 11, 500));
            env.sendEventBean(makeEvent("E1", 10, 600));

            env.milestone(1);

            env.sendEventBean(makeEvent("E1", 12, 700));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E2", 10, null},
                    {"E2", null, null},
                    {"E1", 11, 500L},
                    {"E1", 10, 600L},
                    {"E1", 12, 700L},
                    {"E1", null, 1800L},
                    {null, null, 1800L},
                },
                new Object[][]{
                    {"E2", 10, 100L},
                    {"E2", null, 100L},
                    {"E1", 11, 600L},
                    {"E1", 10, 300L},
                    {"E1", 12, null},
                    {"E1", null, 900L},
                    {null, null, 1000L},
                });

            env.undeployAll();
        }
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        return sb;
    }
}
