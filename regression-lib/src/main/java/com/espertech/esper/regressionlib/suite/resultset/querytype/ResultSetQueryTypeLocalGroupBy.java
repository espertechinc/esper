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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationGroupByLocalGroupDesc;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationLocalGroupByPlanForge;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportThreeArrayEvent;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.util.SupportAggLevelPlanHook;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ResultSetQueryTypeLocalGroupBy {
    public final static String PLAN_CALLBACK_HOOK = "@Hook(type=" + HookType.class.getName() + ".INTERNAL_AGGLOCALLEVEL,hook='" + SupportAggLevelPlanHook.class.getName() + "')";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetLocalUngroupedSumSimple());
        execs.add(new ResultSetLocalUngroupedAggSQLStandard());
        execs.add(new ResultSetLocalUngroupedAggEvent());
        execs.add(new ResultSetLocalUngroupedAggIterator());
        execs.add(new ResultSetLocalUngroupedParenSODA(false));
        execs.add(new ResultSetLocalUngroupedParenSODA(true));
        execs.add(new ResultSetLocalUngroupedColNameRendering());
        execs.add(new ResultSetLocalUngroupedHaving());
        execs.add(new ResultSetLocalUngroupedUnidirectionalJoin());
        execs.add(new ResultSetLocalUngroupedThreeLevelWTop());
        execs.add(new ResultSetLocalGroupedSimple());
        execs.add(new ResultSetLocalGroupedMultiLevelMethod());
        execs.add(new ResultSetLocalGroupedSolutionPattern());
        execs.add(new ResultSetLocalGroupedMultiLevelAccess());
        execs.add(new ResultSetLocalGroupedMultiLevelNoDefaultLvl());
        execs.add(new ResultSetLocalPlanning());
        execs.add(new ResultSetLocalInvalid());
        execs.add(new ResultSetAggregateFullyVersusNotFullyAgg());
        execs.add(new ResultSetLocalUngroupedSameKey());
        execs.add(new ResultSetLocalGroupedSameKey());
        execs.add(new ResultSetLocalUngroupedRowRemove());
        execs.add(new ResultSetLocalGroupedRowRemove());
        execs.add(new ResultSetLocalGroupedOnSelect());
        execs.add(new ResultSetLocalUngroupedOrderBy());
        execs.add(new ResultSetLocalEnumMethods(true));
        execs.add(new ResultSetLocalUngroupedAggAdditionalAndPlugin());
        execs.add(new ResultSetLocalMultikeyWArray());
        return execs;
    }

    public static class ResultSetLocalMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select " +
                "sum(value, group_by:(intArray)) as c0, " +
                "sum(value, group_by:(longArray)) as c1, " +
                "sum(value, group_by:(doubleArray)) as c2, " +
                "sum(value, group_by:(intArray, longArray, doubleArray)) as c3, " +
                "sum(value) as c4 " +
                "from SupportThreeArrayEvent";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, "E1", 10, new int[]{1}, new long[]{10}, new double[]{100}, 10, 10, 10, 10, 10);
            sendAssert(env, "E2", 11, new int[]{2}, new long[]{20}, new double[]{200}, 11, 11, 11, 11, 21);

            env.milestone(0);

            sendAssert(env, "E3", 12, new int[]{3}, new long[]{10}, new double[]{300}, 12, 22, 12, 12, 33);
            sendAssert(env, "E4", 13, new int[]{1}, new long[]{20}, new double[]{200}, 10 + 13, 11 + 13, 11 + 13, 13, 33 + 13);
            sendAssert(env, "E5", 14, new int[]{1}, new long[]{10}, new double[]{100}, 10 + 13 + 14, 10 + 12 + 14, 10 + 14, 10 + 14, 33 + 13 + 14);

            env.milestone(1);

            sendAssert(env, "E6", 15, new int[]{3}, new long[]{20}, new double[]{300}, 12 + 15, 11 + 13 + 15, 12 + 15, 15, 33 + 13 + 14 + 15);
            sendAssert(env, "E7", 16, new int[]{2}, new long[]{20}, new double[]{200}, 11 + 16, 11 + 13 + 15 + 16, 11 + 13 + 16, 11 + 16, 33 + 13 + 14 + 15 + 16);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String id, int value, int[] ints, long[] longs, double[] doubles, int c0, int c1, int c2, int c3, int c4) {
            final String[] fields = "c0,c1,c2,c3,c4".split(",");
            env.sendEventBean(new SupportThreeArrayEvent(id, value, ints, longs, doubles));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{c0, c1, c2, c3, c4});
        }
    }

    public static class ResultSetLocalUngroupedSumSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3".split(",");
            env.milestone(0);

            env.advanceTime(0);
            String epl = "@Name('s0') select " +
                "sum(longPrimitive, group_by:(theString, intPrimitive)) as c0, " +
                "sum(longPrimitive, group_by:(theString)) as c1, " +
                "sum(longPrimitive, group_by:(intPrimitive)) as c2, " +
                "sum(longPrimitive) as c3 " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(1);

            makeSendEvent(env, "E1", 1, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10L, 10L, 10L, 10L});

            env.milestone(2);

            makeSendEvent(env, "E2", 2, 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11L, 11L, 11L, 21L});

            env.milestone(3);

            makeSendEvent(env, "E1", 2, 12);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{12L, 10 + 12L, 11 + 12L, 10 + 11 + 12L});

            env.milestone(4);

            env.milestone(5);

            makeSendEvent(env, "E1", 1, 13);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10 + 13L, 10 + 12 + 13L, 10 + 13L, 10 + 11 + 12 + 13L});

            makeSendEvent(env, "E2", 1, 14);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{14L, 11 + 14L, 10 + 13 + 14L, 10 + 11 + 12 + 13 + 14L});

            env.undeployAll();
        }
    }

    private static class ResultSetLocalInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // not valid with count-min-sketch
            SupportMessageAssertUtil.tryInvalidCompile(env, "create table MyTable(approx countMinSketch(group_by:theString) @type(SupportBean))",
                "Failed to validate table-column expression 'countMinSketch(group_by:theString)': The 'group_by' and 'filter' parameter is not allowed in create-table statements");

            // not allowed with tables
            SupportMessageAssertUtil.tryInvalidCompile(env, "create table MyTable(col sum(int, group_by:theString) @type(SupportBean))",
                "Failed to validate table-column expression 'sum(int,group_by:theString)': The 'group_by' and 'filter' parameter is not allowed in create-table statements");

            // invalid named parameter
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sum(intPrimitive, xxx:theString) from SupportBean",
                "Failed to validate select-clause expression 'sum(intPrimitive,xxx:theString)': Invalid named parameter 'xxx' (did you mean 'group_by' or 'filter'?) [");

            // invalid group-by expression
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sum(intPrimitive, group_by:sum(intPrimitive)) from SupportBean",
                "Failed to validate select-clause expression 'sum(intPrimitive,group_by:sum(intPr...(44 chars)': Group-by expressions cannot contain aggregate functions");

            // other functions don't accept this named parameter
            SupportMessageAssertUtil.tryInvalidCompile(env, "select coalesce(0, 1, group_by:theString) from SupportBean",
                "Incorrect syntax near ':' at line 1 column 30");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select " + SupportStaticMethodLib.class.getName() + ".staticMethod(group_by:intPrimitive) from SupportBean",
                "Failed to validate select-clause expression 'com.espertech.esper.regressionlib.s...(104 chars)': Named parameters are not allowed");

            // not allowed in combination with roll-up
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sum(intPrimitive, group_by:theString) from SupportBean group by rollup(theString)",
                "Roll-up and group-by parameters cannot be combined ");

            // not allowed in combination with into-table
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table mytable (thesum sum(int))", path);
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "into table mytable select sum(intPrimitive, group_by:theString) as thesum from SupportBean",
                "Into-table and group-by parameters cannot be combined");

            // not allowed for match-rezognize measure clauses
            String eplMatchRecog = "select * from SupportBean match_recognize (" +
                "  measures count(B.intPrimitive, group_by:B.theString) pattern (A B* C))";
            SupportMessageAssertUtil.tryInvalidCompile(env, eplMatchRecog,
                "Match-recognize does not allow aggregation functions to specify a group-by");

            // disallow subqueries to specify their own local group-by
            String eplSubq = "select (select sum(intPrimitive, group_by:theString) from SupportBean#keepall) from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, eplSubq,
                "Failed to plan subquery number 1 querying SupportBean: Subselect aggregations functions cannot specify a group-by");

            env.undeployAll();
        }
    }

    private static class ResultSetLocalPlanning implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertNoPlan(env, "select sum(group_by:(),intPrimitive) as c0 from SupportBean");
            assertNoPlan(env, "select sum(group_by:(theString),intPrimitive) as c0 from SupportBean group by theString");
            assertNoPlan(env, "select sum(group_by:(theString, intPrimitive),longPrimitive) as c0 from SupportBean group by theString, intPrimitive");
            assertNoPlan(env, "select sum(group_by:(intPrimitive, theString),longPrimitive) as c0 from SupportBean group by theString, intPrimitive");

            // provide column count stays at 1
            assertCountColsAndLevels(env, "select sum(group_by:(theString),intPrimitive) as c0, sum(group_by:(theString),intPrimitive) as c1 from SupportBean",
                1, 1);

            // prove order of group-by expressions does not matter
            assertCountColsAndLevels(env, "select sum(group_by:(intPrimitive, theString),longPrimitive) as c0, sum(longPrimitive, group_by:(theString, intPrimitive)) as c1 from SupportBean",
                1, 1);

            // prove the number of levels stays the same even when group-by expressions vary
            assertCountColsAndLevels(env, "select sum(group_by:(intPrimitive, theString),longPrimitive) as c0, count(*, group_by:(theString, intPrimitive)) as c1 from SupportBean",
                2, 1);

            // prove there is one shared state factory
            String theEpl = PLAN_CALLBACK_HOOK + "@name('s0') select window(*, group_by:theString), last(*, group_by:theString) from SupportBean#length(2)";
            env.compile(theEpl);
            Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> plan = SupportAggLevelPlanHook.getAndReset();
            assertEquals(1, plan.getSecond().getAllLevelsForges().length);
            assertEquals(1, plan.getSecond().getAllLevelsForges()[0].getAccessStateForges().length);
        }
    }

    private static class ResultSetAggregateFullyVersusNotFullyAgg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final String[] colsC0 = "c0".split(",");

            // full-aggregated and un-grouped (row for all)
            tryAssertionAggAndFullyAgg(env, "select sum(group_by:(),intPrimitive) as c0 from SupportBean",
                new MyAssertion() {
                    public void doAssert(SupportListener listener) {
                        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), colsC0, new Object[]{60});
                    }
                });

            // aggregated and un-grouped (row for event)
            tryAssertionAggAndFullyAgg(env, "select sum(group_by:theString, intPrimitive) as c0 from SupportBean#keepall",
                new MyAssertion() {
                    public void doAssert(SupportListener listener) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), colsC0, new Object[][]{{10}, {50}, {50}});
                    }
                });

            // fully aggregated and grouped (row for group)
            tryAssertionAggAndFullyAgg(env, "select sum(intPrimitive, group_by:()) as c0, sum(group_by:theString, intPrimitive) as c1, theString " +
                    "from SupportBean group by theString",
                new MyAssertion() {
                    public void doAssert(SupportListener listener) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "theString,c0,c1".split(","), new Object[][]{{"E1", 60, 10}, {"E2", 60, 50}});
                    }
                });

            // aggregated and grouped (row for event)
            tryAssertionAggAndFullyAgg(env, "select sum(longPrimitive, group_by:()) as c0," +
                    " sum(longPrimitive, group_by:theString) as c1, " +
                    " sum(longPrimitive, group_by:intPrimitive) as c2, " +
                    " theString " +
                    "from SupportBean#keepall group by theString",
                new MyAssertion() {
                    public void doAssert(SupportListener listener) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(),
                            "theString,c0,c1,c2".split(","), new Object[][]{{"E1", 600L, 100L, 100L}, {"E2", 600L, 500L, 200L}, {"E2", 600L, 500L, 300L}});
                    }
                });
        }
    }

    public static class ResultSetLocalUngroupedRowRemove implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] cols = "theString,intPrimitive,c0,c1".split(",");
            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_S0 delete from MyWindow where p00 = theString and id = intPrimitive;\n" +
                "on SupportBean_S1 delete from MyWindow;\n" +
                "@name('s0') select theString, intPrimitive, sum(longPrimitive) as c0, " +
                "  sum(longPrimitive, group_by:theString) as c1 from MyWindow;\n";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10, 101);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 10, 101L, 101L});

            env.sendEventBean(new SupportBean_S0(10, "E1")); // delete event {"E1", 10}
            assertFalse(env.listener("s0").isInvoked());

            makeSendEvent(env, "E1", 20, 102);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 20, 102L, 102L});

            makeSendEvent(env, "E2", 30, 103);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E2", 30, 102 + 103L, 103L});

            makeSendEvent(env, "E1", 40, 104);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 40, 102 + 103 + 104L, 102 + 104L});

            env.sendEventBean(new SupportBean_S0(40, "E1")); // delete event {"E1", 40}
            assertFalse(env.listener("s0").isInvoked());

            makeSendEvent(env, "E1", 50, 105);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 50, 102 + 103 + 105L, 102 + 105L});

            env.sendEventBean(new SupportBean_S1(-1)); // delete all
            assertFalse(env.listener("s0").isInvoked());

            makeSendEvent(env, "E1", 60, 106);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 60, 106L, 106L});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedRowRemove implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] cols = "theString,intPrimitive,c0,c1".split(",");
            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_S0 delete from MyWindow where p00 = theString and id = intPrimitive;\n" +
                "on SupportBean_S1 delete from MyWindow;\n" +
                "@name('s0') select theString, intPrimitive, sum(longPrimitive) as c0, " +
                "  sum(longPrimitive, group_by:theString) as c1 " +
                "  from MyWindow group by theString, intPrimitive;\n";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10, 101);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 10, 101L, 101L});

            env.sendEventBean(new SupportBean_S0(10, "E1")); // delete event {"E1", 10}
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 10, null, null});

            makeSendEvent(env, "E1", 20, 102);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 20, 102L, 102L});

            makeSendEvent(env, "E2", 30, 103);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E2", 30, 103L, 103L});

            makeSendEvent(env, "E1", 40, 104);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 40, 104L, 102 + 104L});

            env.sendEventBean(new SupportBean_S0(40, "E1")); // delete event {"E1", 40}
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 40, null, 102L});

            makeSendEvent(env, "E1", 50, 105);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 50, 105L, 102 + 105L});

            env.sendEventBean(new SupportBean_S1(-1)); // delete all
            env.listener("s0").reset();

            makeSendEvent(env, "E1", 60, 106);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{"E1", 60, 106L, 106L});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedMultiLevelMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTime(env, 0);
            String[] fields = "theString,intPrimitive,c0,c1,c2,c3,c4".split(",");
            String epl = "@name('s0') select" +
                "   theString, intPrimitive," +
                "   sum(longPrimitive, group_by:(intPrimitive, theString)) as c0," +
                "   sum(longPrimitive) as c1," +
                "   sum(longPrimitive, group_by:(theString)) as c2," +
                "   sum(longPrimitive, group_by:(intPrimitive)) as c3," +
                "   sum(longPrimitive, group_by:()) as c4" +
                " from SupportBean" +
                " group by theString, intPrimitive" +
                " output snapshot every 10 seconds";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10, 100);
            makeSendEvent(env, "E1", 20, 202);
            makeSendEvent(env, "E2", 10, 303);
            makeSendEvent(env, "E1", 10, 404);
            makeSendEvent(env, "E2", 10, 505);
            sendTime(env, 10000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 504L, 504L, 706L, 1312L, 1514L}, {"E1", 20, 202L, 202L, 706L, 202L, 1514L}, {"E2", 10, 808L, 808L, 808L, 1312L, 1514L}});

            makeSendEvent(env, "E1", 10, 1);
            sendTime(env, 20000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 505L, 505L, 707L, 1313L, 1515L}, {"E1", 20, 202L, 202L, 707L, 202L, 1515L}, {"E2", 10, 808L, 808L, 808L, 1313L, 1515L}});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedMultiLevelAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTime(env, 0);
            String[] fields = "theString,intPrimitive,c0,c1,c2,c3,c4".split(",");
            String epl = "@name('s0') select" +
                "   theString, intPrimitive," +
                "   window(*, group_by:(intPrimitive, theString)) as c0," +
                "   window(*) as c1," +
                "   window(*, group_by:theString) as c2," +
                "   window(*, group_by:intPrimitive) as c3," +
                "   window(*, group_by:()) as c4" +
                " from SupportBean#keepall" +
                " group by theString, intPrimitive" +
                " output snapshot every 10 seconds" +
                " order by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");

            SupportBean b1 = makeSendEvent(env, "E1", 10, 100);
            SupportBean b2 = makeSendEvent(env, "E1", 20, 202);
            SupportBean b3 = makeSendEvent(env, "E2", 10, 303);
            SupportBean b4 = makeSendEvent(env, "E1", 10, 404);
            SupportBean b5 = makeSendEvent(env, "E2", 10, 505);
            sendTime(env, 10000);

            Object[] all = new Object[]{b1, b2, b3, b4, b5};
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields,
                new Object[]{"E1", 10, new Object[]{b1, b4}, new Object[]{b1, b4}, new Object[]{b1, b2, b4},
                    new Object[]{b1, b3, b4, b5}, all});
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[1], fields,
                new Object[]{"E1", 20, new Object[]{b2}, new Object[]{b2}, new Object[]{b1, b2, b4},
                    new Object[]{b2}, all});
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[2], fields,
                new Object[]{"E2", 10, new Object[]{b3, b5}, new Object[]{b3, b5}, new Object[]{b3, b5},
                    new Object[]{b1, b3, b4, b5}, all});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedMultiLevelNoDefaultLvl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTime(env, 0);
            String[] fields = "theString,intPrimitive,c0,c1,c2".split(",");
            String epl = "@name('s0') select" +
                "   theString, intPrimitive," +
                "   sum(longPrimitive, group_by:(theString)) as c0," +
                "   sum(longPrimitive, group_by:(intPrimitive)) as c1," +
                "   sum(longPrimitive, group_by:()) as c2" +
                " from SupportBean" +
                " group by theString, intPrimitive" +
                " output snapshot every 10 seconds";

            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10, 100);
            makeSendEvent(env, "E1", 20, 202);
            makeSendEvent(env, "E2", 10, 303);
            makeSendEvent(env, "E1", 10, 404);
            makeSendEvent(env, "E2", 10, 505);
            sendTime(env, 10000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 706L, 1312L, 1514L}, {"E1", 20, 706L, 202L, 1514L}, {"E2", 10, 808L, 1312L, 1514L}});

            makeSendEvent(env, "E1", 10, 1);
            sendTime(env, 20000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, 707L, 1313L, 1515L}, {"E1", 20, 707L, 202L, 1515L}, {"E2", 10, 808L, 1313L, 1515L}});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedSolutionPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTime(env, 0);
            String[] fields = "theString,pct".split(",");
            String epl = "@name('s0') select theString, count(*) / count(*, group_by:()) as pct" +
                " from SupportBean#time(30 sec)" +
                " group by theString" +
                " output snapshot every 10 seconds";
            env.compileDeploy(epl).addListener("s0");

            sendEventMany(env, "A", "B", "C", "B", "B", "C");
            sendTime(env, 10000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"A", 1 / 6d}, {"B", 3 / 6d}, {"C", 2 / 6d}});

            sendEventMany(env, "A", "B", "B", "B", "B", "A");
            sendTime(env, 20000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"A", 3 / 12d}, {"B", 7 / 12d}, {"C", 2 / 12d}});

            sendEventMany(env, "C", "A", "A", "A", "B", "A");
            sendTime(env, 30000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"A", 6 / 12d}, {"B", 5 / 12d}, {"C", 1 / 12d}});

            env.undeployAll();
        }
    }

    private static void tryAssertionAggAndFullyAgg(RegressionEnvironment env, String selected, MyAssertion assertion) {
        String epl = "create context StartS0EndS1 start SupportBean_S0 end SupportBean_S1;" +
            "@name('s0') context StartS0EndS1 " +
            selected +
            " output snapshot when terminated;";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean_S0(0));
        makeSendEvent(env, "E1", 10, 100);
        makeSendEvent(env, "E2", 20, 200);
        makeSendEvent(env, "E2", 30, 300);
        env.sendEventBean(new SupportBean_S1(0));

        assertion.doAssert(env.listener("s0"));

        // try an empty batch
        env.sendEventBean(new SupportBean_S0(1));
        env.sendEventBean(new SupportBean_S1(1));

        env.undeployAll();
    }

    public static class ResultSetLocalUngroupedParenSODA implements RegressionExecution {
        private final boolean soda;

        public ResultSetLocalUngroupedParenSODA(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String[] cols = "c0,c1,c2,c3,c4".split(",");
            String epl = "@name('s0') select longPrimitive, " +
                "sum(longPrimitive) as c0, " +
                "sum(group_by:(),longPrimitive) as c1, " +
                "sum(longPrimitive,group_by:()) as c2, " +
                "sum(longPrimitive,group_by:theString) as c3, " +
                "sum(longPrimitive,group_by:(theString,intPrimitive)) as c4" +
                " from SupportBean";
            env.compileDeploy(soda, epl).addListener("s0");

            makeSendEvent(env, "E1", 1, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{10L, 10L, 10L, 10L, 10L});

            makeSendEvent(env, "E1", 2, 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{21L, 21L, 21L, 21L, 11L});

            makeSendEvent(env, "E2", 1, 12);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{33L, 33L, 33L, 12L, 12L});

            makeSendEvent(env, "E2", 2, 13);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{46L, 46L, 46L, 25L, 13L});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedAggAdditionalAndPlugin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] cols = "c0,c1,c2,c3,c4,c5,c8,c9,c10,c11,c12,c13".split(",");
            String epl = "@name('s0') select intPrimitive, " +
                " countever(*, intPrimitive>0, group_by:(theString)) as c0," +
                " countever(*, intPrimitive>0, group_by:()) as c1," +
                " countever(*, group_by:(theString)) as c2," +
                " countever(*, group_by:()) as c3," +
                " concatstring(Integer.toString(intPrimitive), group_by:(theString)) as c4," +
                " concatstring(Integer.toString(intPrimitive), group_by:()) as c5," +
                " sc(intPrimitive, group_by:(theString)) as c6," +
                " sc(intPrimitive, group_by:()) as c7," +
                " leaving(group_by:(theString)) as c8," +
                " leaving(group_by:()) as c9," +
                " rate(3, group_by:(theString)) as c10," +
                " rate(3, group_by:()) as c11," +
                " nth(intPrimitive, 1, group_by:(theString)) as c12," +
                " nth(intPrimitive, 1, group_by:()) as c13" +
                " from SupportBean as sb";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10);
            assertScalarColl(env.listener("s0").getLastNewData()[0], new Integer[]{10}, new Integer[]{10});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{1L, 1L, 1L, 1L, "10", "10", false, false,
                null, null, null, null});

            makeSendEvent(env, "E2", 20);
            assertScalarColl(env.listener("s0").getLastNewData()[0], new Integer[]{20}, new Integer[]{10, 20});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{1L, 2L, 1L, 2L, "20", "10 20", false, false,
                null, null, null, 10});

            makeSendEvent(env, "E1", -1);
            assertScalarColl(env.listener("s0").getLastNewData()[0], new Integer[]{10, -1}, new Integer[]{10, 20, -1});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{1L, 2L, 2L, 3L, "10 -1", "10 20 -1", false, false,
                null, null, 10, 20});

            makeSendEvent(env, "E2", 30);
            assertScalarColl(env.listener("s0").getLastNewData()[0], new Integer[]{20, 30}, new Integer[]{10, 20, -1, 30});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{2L, 3L, 2L, 4L, "20 30", "10 20 -1 30", false, false,
                null, null, 20, -1});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedAggEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] cols = "first0,first1,last0,last1,window0,window1,maxby0,maxby1,minby0,minby1,sorted0,sorted1,maxbyever0,maxbyever1,minbyever0,minbyever1,firstever0,firstever1,lastever0,lastever1".split(",");
            String epl = "@name('s0') select intPrimitive as c0, " +
                " first(sb, group_by:(theString)) as first0," +
                " first(sb, group_by:()) as first1," +
                " last(sb, group_by:(theString)) as last0," +
                " last(sb, group_by:()) as last1," +
                " window(sb, group_by:(theString)) as window0," +
                " window(sb, group_by:()) as window1," +
                " maxby(intPrimitive, group_by:(theString)) as maxby0," +
                " maxby(intPrimitive, group_by:()) as maxby1," +
                " minby(intPrimitive, group_by:(theString)) as minby0," +
                " minby(intPrimitive, group_by:()) as minby1," +
                " sorted(intPrimitive, group_by:(theString)) as sorted0," +
                " sorted(intPrimitive, group_by:()) as sorted1," +
                " maxbyever(intPrimitive, group_by:(theString)) as maxbyever0," +
                " maxbyever(intPrimitive, group_by:()) as maxbyever1," +
                " minbyever(intPrimitive, group_by:(theString)) as minbyever0," +
                " minbyever(intPrimitive, group_by:()) as minbyever1," +
                " firstever(sb, group_by:(theString)) as firstever0," +
                " firstever(sb, group_by:()) as firstever1," +
                " lastever(sb, group_by:(theString)) as lastever0," +
                " lastever(sb, group_by:()) as lastever1" +
                " from SupportBean#length(3) as sb";
            env.compileDeploy(epl).addListener("s0");

            SupportBean b1 = makeSendEvent(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{b1, b1, b1, b1, new Object[]{b1}, new Object[]{b1},
                b1, b1, b1, b1, new Object[]{b1}, new Object[]{b1}, b1, b1, b1, b1,
                b1, b1, b1, b1});

            SupportBean b2 = makeSendEvent(env, "E2", 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{b2, b1, b2, b2, new Object[]{b2}, new Object[]{b1, b2},
                b2, b2, b2, b1, new Object[]{b2}, new Object[]{b1, b2}, b2, b2, b2, b1,
                b2, b1, b2, b2});

            SupportBean b3 = makeSendEvent(env, "E1", 15);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{b1, b1, b3, b3, new Object[]{b1, b3}, new Object[]{b1, b2, b3},
                b3, b2, b1, b1, new Object[]{b1, b3}, new Object[]{b1, b3, b2}, b3, b2, b1, b1,
                b1, b1, b3, b3});

            SupportBean b4 = makeSendEvent(env, "E3", 16);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{b4, b2, b4, b4, new Object[]{b4}, new Object[]{b2, b3, b4},
                b4, b2, b4, b3, new Object[]{b4}, new Object[]{b3, b4, b2}, b4, b2, b4, b1,
                b4, b1, b4, b4});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedAggSQLStandard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,sum0,sum1,avedev0,avg0,max0,fmax0,min0,fmin0,maxever0,fmaxever0,minever0,fminever0,median0,stddev0".split(",");
            String epl = "@name('s0') select intPrimitive as c0, " +
                "sum(intPrimitive, group_by:()) as sum0, " +
                "sum(intPrimitive, group_by:(theString)) as sum1," +
                "avedev(intPrimitive, group_by:(theString)) as avedev0," +
                "avg(intPrimitive, group_by:(theString)) as avg0," +
                "max(intPrimitive, group_by:(theString)) as max0," +
                "fmax(intPrimitive, intPrimitive>0, group_by:(theString)) as fmax0," +
                "min(intPrimitive, group_by:(theString)) as min0," +
                "fmin(intPrimitive, intPrimitive>0, group_by:(theString)) as fmin0," +
                "maxever(intPrimitive, group_by:(theString)) as maxever0," +
                "fmaxever(intPrimitive, intPrimitive>0, group_by:(theString)) as fmaxever0," +
                "minever(intPrimitive, group_by:(theString)) as minever0," +
                "fminever(intPrimitive, intPrimitive>0, group_by:(theString)) as fminever0," +
                "median(intPrimitive, group_by:(theString)) as median0," +
                "Math.round(coalesce(stddev(intPrimitive, group_by:(theString)), 0)) as stddev0" +
                " from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 10, 10,
                0.0d, 10d, 10, 10, 10, 10, 10, 10, 10, 10, 10.0, 0L});

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 10 + 20, 20,
                0.0d, 20d, 20, 20, 20, 20, 20, 20, 20, 20, 20.0, 0L});

            env.sendEventBean(new SupportBean("E1", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{30, 10 + 20 + 30, 10 + 30,
                10.0d, 20d, 30, 30, 10, 10, 30, 30, 10, 10, 20.0, 14L});

            env.sendEventBean(new SupportBean("E2", 40));
            Object[] expected = new Object[]{40, 10 + 20 + 30 + 40, 20 + 40,
                10.0d, 30d, 40, 40, 20, 20, 40, 40, 20, 20, 30.0, 14L};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedSameKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create objectarray schema MyEventOne (d1 String, d2 String, val int);\n" +
                "@name('s0') select sum(val, group_by: d1) as c0, sum(val, group_by: d2) as c1 from MyEventOne";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            String[] cols = "c0,c1".split(",");

            env.sendEventObjectArray(new Object[]{"E1", "E1", 10}, "MyEventOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{10, 10});

            env.sendEventObjectArray(new Object[]{"E1", "E2", 11}, "MyEventOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{21, 11});

            env.sendEventObjectArray(new Object[]{"E2", "E1", 12}, "MyEventOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{12, 22});

            env.sendEventObjectArray(new Object[]{"E3", "E1", 13}, "MyEventOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{13, 35});

            env.sendEventObjectArray(new Object[]{"E3", "E3", 14}, "MyEventOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{27, 14});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedSameKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create objectarray schema MyEventTwo (g1 String, d1 String, d2 String, val int);\n" +
                "@name('s0') select sum(val) as c0, sum(val, group_by: d1) as c1, sum(val, group_by: d2) as c2 from MyEventTwo group by g1";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            String[] cols = "c0,c1,c2".split(",");

            env.sendEventObjectArray(new Object[]{"E1", "E1", "E1", 10}, "MyEventTwo");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{10, 10, 10});

            env.sendEventObjectArray(new Object[]{"E1", "E1", "E2", 11}, "MyEventTwo");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{21, 21, 11});

            env.sendEventObjectArray(new Object[]{"E1", "E2", "E1", 12}, "MyEventTwo");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{33, 12, 22});

            env.sendEventObjectArray(new Object[]{"X", "E1", "E1", 13}, "MyEventTwo");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{13, 10 + 11 + 13, 10 + 12 + 13});

            env.sendEventObjectArray(new Object[]{"E1", "E2", "E3", 14}, "MyEventTwo");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), cols, new Object[]{10 + 11 + 12 + 14, 12 + 14, 14});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedAggIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,sum0,sum1".split(",");
            String epl = "@name('s0') select intPrimitive as c0, " +
                "sum(intPrimitive, group_by:()) as sum0, " +
                "sum(intPrimitive, group_by:(theString)) as sum1 " +
                " from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{10, 10, 10}});

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{10, 30, 10}, {20, 30, 20}});

            env.sendEventBean(new SupportBean("E1", 30));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{10, 60, 40}, {20, 60, 20}, {30, 60, 40}});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean having sum(intPrimitive, group_by:theString) > 100";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 95);
            makeSendEvent(env, "E2", 10);
            assertFalse(env.listener("s0").isInvoked());

            makeSendEvent(env, "E1", 10);
            assertTrue(env.listener("s0").isInvoked());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedOrderBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context StartS0EndS1 start SupportBean_S0 end SupportBean_S1;" +
                "@name('s0') context StartS0EndS1 select theString, sum(intPrimitive, group_by:theString) as c0 " +
                " from SupportBean#keepall " +
                " output snapshot when terminated" +
                " order by sum(intPrimitive, group_by:theString)" +
                ";";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(0));
            makeSendEvent(env, "E1", 10);
            makeSendEvent(env, "E2", 20);
            makeSendEvent(env, "E1", 30);
            makeSendEvent(env, "E3", 40);
            makeSendEvent(env, "E2", 50);
            env.sendEventBean(new SupportBean_S1(0));

            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "theString,c0".split(","), new Object[][]{
                {"E1", 40}, {"E1", 40}, {"E3", 40}, {"E2", 70}, {"E2", 70}});

            // try an empty batch
            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S1(1));

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedOnSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindow#keepall as SupportBean;" +
                "insert into MyWindow select * from SupportBean;" +
                "@name('s0') on SupportBean_S0 select theString, sum(intPrimitive) as c0, sum(intPrimitive, group_by:()) as c1" +
                " from MyWindow group by theString;";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10);
            makeSendEvent(env, "E2", 20);
            makeSendEvent(env, "E1", 30);
            makeSendEvent(env, "E3", 40);
            makeSendEvent(env, "E2", 50);

            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "theString,c0,c1".split(","), new Object[][]{
                {"E1", 40, 150}, {"E2", 70, 150}, {"E3", 40, 150}});

            makeSendEvent(env, "E1", 60);

            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "theString,c0,c1".split(","), new Object[][]{
                {"E1", 100, 210}, {"E2", 70, 210}, {"E3", 40, 210}});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedUnidirectionalJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString, sum(intPrimitive, group_by:theString) as c0 from SupportBean#keepall, SupportBean_S0 unidirectional";
            env.compileDeploy(epl).addListener("s0");

            makeSendEvent(env, "E1", 10);
            makeSendEvent(env, "E2", 20);
            makeSendEvent(env, "E1", 30);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "theString,c0".split(","),
                new Object[][]{{"E1", 40}, {"E1", 40}, {"E2", 20}});

            makeSendEvent(env, "E1", 40);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "theString,c0".split(","),
                new Object[][]{{"E1", 80}, {"E1", 80}, {"E1", 80}, {"E2", 20}});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalUngroupedThreeLevelWTop implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9".split(",");
            String epl = "@Name('s0') select " +
                "sum(longPrimitive, group_by:theString) as c0," +
                "count(*, group_by:theString) as c1," +
                "window(*, group_by:theString) as c2," +
                "sum(longPrimitive, group_by:intPrimitive) as c3," +
                "count(*, group_by:intPrimitive) as c4," +
                "window(*, group_by:intPrimitive) as c5," +
                "sum(longPrimitive, group_by:(theString, intPrimitive)) as c6," +
                "count(*, group_by:(theString, intPrimitive)) as c7," +
                "window(*, group_by:(theString, intPrimitive)) as c8," +
                "sum(longPrimitive) as c9 " +
                "from SupportBean#length(4)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            SupportBean b1 = makeSendEvent(env, "E1", 10, 100L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{100L, 1L, new Object[]{b1}, 100L, 1L, new Object[]{b1}, 100L, 1L, new Object[]{b1}, 100L});

            env.milestone(1);

            SupportBean b2 = makeSendEvent(env, "E2", 10, 101L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{101L, 1L, new Object[]{b2}, 201L, 2L, new Object[]{b1, b2}, 101L, 1L, new Object[]{b2}, 201L});

            env.milestone(2);

            SupportBean b3 = makeSendEvent(env, "E1", 20, 102L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{202L, 2L, new Object[]{b1, b3}, 102L, 1L, new Object[]{b3}, 102L, 1L, new Object[]{b3}, 303L});

            env.milestone(3);

            SupportBean b4 = makeSendEvent(env, "E1", 10, 103L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{305L, 3L, new Object[]{b1, b3, b4}, 304L, 3L, new Object[]{b1, b2, b4}, 203L, 2L, new Object[]{b1, b4}, 406L});

            env.milestone(4);

            SupportBean b5 = makeSendEvent(env, "E1", 10, 104L); // expires b1
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{309L, 3L, new Object[]{b3, b4, b5}, 308L, 3L, new Object[]{b2, b4, b5}, 207L, 2L, new Object[]{b4, b5}, 410L});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalGroupedSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9".split(",");
            String epl = "@Name('s0') select " +
                "sum(longPrimitive, group_by:theString) as c0," +
                "count(*, group_by:theString) as c1," +
                "window(*, group_by:theString) as c2," +
                "sum(longPrimitive, group_by:intPrimitive) as c3," +
                "count(*, group_by:intPrimitive) as c4," +
                "window(*, group_by:intPrimitive) as c5," +
                "sum(longPrimitive, group_by:()) as c6," +
                "count(*, group_by:()) as c7," +
                "window(*, group_by:()) as c8," +
                "sum(longPrimitive) as c9 " +
                "from SupportBean#length(4)" +
                "group by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            SupportBean b1 = makeSendEvent(env, "E1", 10, 100L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{100L, 1L, new Object[]{b1}, 100L, 1L, new Object[]{b1}, 100L, 1L, new Object[]{b1}, 100L});

            env.milestone(1);

            SupportBean b2 = makeSendEvent(env, "E2", 10, 101L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{101L, 1L, new Object[]{b2}, 201L, 2L, new Object[]{b1, b2}, 201L, 2L, new Object[]{b1, b2}, 101L});

            env.milestone(2);

            SupportBean b3 = makeSendEvent(env, "E1", 20, 102L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{202L, 2L, new Object[]{b1, b3}, 102L, 1L, new Object[]{b3}, 303L, 3L, new Object[]{b1, b2, b3}, 102L});

            env.milestone(3);

            SupportBean b4 = makeSendEvent(env, "E1", 10, 103L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{305L, 3L, new Object[]{b1, b3, b4}, 304L, 3L, new Object[]{b1, b2, b4}, 406L, 4L, new Object[]{b1, b2, b3, b4}, 203L});

            env.milestone(4);

            SupportBean b5 = makeSendEvent(env, "E1", 10, 104L); // expires b1
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{309L, 3L, new Object[]{b3, b4, b5}, 308L, 3L, new Object[]{b2, b4, b5}, 410L, 4L, new Object[]{b2, b3, b4, b5}, 207L});

            env.undeployAll();
        }
    }

    public static class ResultSetLocalEnumMethods implements RegressionExecution {
        private final boolean grouped;

        public ResultSetLocalEnumMethods(boolean grouped) {
            this.grouped = grouped;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select" +
                " window(*, group_by:()).firstOf() as c0," +
                " window(*, group_by:theString).firstOf() as c1," +
                " window(intPrimitive, group_by:()).firstOf() as c2," +
                " window(intPrimitive, group_by:theString).firstOf() as c3," +
                " first(*, group_by:()).intPrimitive as c4," +
                " first(*, group_by:theString).intPrimitive as c5 " +
                " from SupportBean#keepall " +
                (grouped ? "group by theString, intPrimitive" : "");
            env.compileDeploy(epl).addListener("s0");

            SupportBean b1 = makeSendEvent(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5".split(","),
                new Object[]{b1, b1, 10, 10, 10, 10});

            env.undeployAll();
        }
    }

    private static void sendTime(RegressionEnvironment env, long msec) {
        env.advanceTime(msec);
    }

    private static void sendEventMany(RegressionEnvironment env, String... theString) {
        for (String value : theString) {
            sendEvent(env, value);
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }

    private static SupportBean makeSendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        env.sendEventBean(b);
        return b;
    }

    private static SupportBean makeSendEvent(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        env.sendEventBean(b);
        return b;
    }

    protected interface MyAssertion {
        void doAssert(SupportListener listener);
    }

    private static void assertCountColsAndLevels(RegressionEnvironment env, String epl, int colCount, int lvlCount) {
        String theEpl = PLAN_CALLBACK_HOOK + epl;
        env.compile(theEpl);
        Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> plan = SupportAggLevelPlanHook.getAndReset();
        assertEquals(colCount, plan.getFirst().getNumColumns());
        assertEquals(lvlCount, plan.getFirst().getLevels().length);
    }

    private static void assertNoPlan(RegressionEnvironment env, String epl) {
        String theEpl = PLAN_CALLBACK_HOOK + epl;
        env.compile(theEpl);
        assertNull(SupportAggLevelPlanHook.getAndReset());
    }

    public static class ResultSetLocalUngroupedColNameRendering implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "count(*, group_by:(theString, intPrimitive)), " +
                "count(group_by:theString, *) " +
                "from SupportBean";
            env.compileDeploy(epl);
            assertEquals("count(*,group_by:(theString,intPrimitive))", env.statement("s0").getEventType().getPropertyNames()[0]);
            assertEquals("count(group_by:theString,*)", env.statement("s0").getEventType().getPropertyNames()[1]);
            env.undeployAll();
        }
    }

    private static void assertScalarColl(EventBean eventBean, Integer[] expectedC6, Integer[] expectedC7) {
        Collection c6 = (Collection) eventBean.get("c6");
        Collection c7 = (Collection) eventBean.get("c7");
        EPAssertionUtil.assertEqualsExactOrder(expectedC6, c6.toArray());
        EPAssertionUtil.assertEqualsExactOrder(expectedC7, c7.toArray());
    }
}
