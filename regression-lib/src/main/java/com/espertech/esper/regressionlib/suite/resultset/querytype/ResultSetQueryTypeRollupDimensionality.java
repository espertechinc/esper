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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportThreeArrayEvent;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResultSetQueryTypeRollupDimensionality {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeBoundRollup2Dim());
        execs.add(new ResultSetQueryTypeUnboundRollup2Dim());
        execs.add(new ResultSetQueryTypeUnboundRollup1Dim());
        execs.add(new ResultSetQueryTypeUnboundRollup2DimBatchWindow());
        execs.add(new ResultSetQueryTypeUnboundRollup3Dim());
        execs.add(new ResultSetQueryTypeMixedAccessAggregation());
        execs.add(new ResultSetQueryTypeNonBoxedTypeWithRollup());
        execs.add(new ResultSetQueryTypeGroupByWithComputation());
        execs.add(new ResultSetQueryTypeUnboundRollupUnenclosed());
        execs.add(new ResultSetQueryTypeUnboundCubeUnenclosed());
        execs.add(new ResultSetQueryTypeUnboundGroupingSet2LevelUnenclosed());
        execs.add(new ResultSetQueryTypeBoundCube3Dim());
        execs.add(new ResultSetQueryTypeBoundGroupingSet2LevelNoTopNoDetail());
        execs.add(new ResultSetQueryTypeBoundGroupingSet2LevelTopAndDetail());
        execs.add(new ResultSetQueryTypeUnboundCube4Dim());
        execs.add(new ResultSetQueryTypeInvalid());
        execs.add(new ResultSetQueryTypeOutputWhenTerminated());
        execs.add(new ResultSetQueryTypeContextPartitionAlsoRollup());
        execs.add(new ResultSetQueryTypeOnSelect());
        execs.add(new ResultSetQueryTypeNamedWindowCube2Dim());
        execs.add(new ResultSetQueryTypeRollupMultikeyWArray(false, true));
        execs.add(new ResultSetQueryTypeRollupMultikeyWArray(false, false));
        execs.add(new ResultSetQueryTypeRollupMultikeyWArray(true, false));
        execs.add(new ResultSetQueryTypeRollupMultikeyWArrayGroupingSet());
        return execs;
    }

    public static class ResultSetQueryTypeRollupMultikeyWArrayGroupingSet implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select sum(value) as thesum from SupportThreeArrayEvent group by grouping sets((intArray), (longArray), (doubleArray))";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, "E1", 1, new int[] {1, 2}, new long[] {10, 20}, new double[] {300, 400}, 1, 1, 1);

            env.milestone(0);

            sendAssert(env, "E2", 2, new int[] {1, 2}, new long[] {10, 20}, new double[] {300, 400}, 3, 3, 3);
            sendAssert(env, "E3", 3, new int[] {1, 2}, new long[] {11, 21}, new double[] {300, 400}, 6, 3, 6);
            sendAssert(env, "E4", 4, new int[] {1, 3}, new long[] {10}, new double[] {300, 400}, 4, 4, 10);
            sendAssert(env, "E5", 5, new int[] {1, 2}, new long[] {10, 21}, new double[] {301, 400}, 11, 5, 5);

            env.milestone(1);

            sendAssert(env, "E6", 6, new int[] {1, 2}, new long[] {10, 20}, new double[] {300, 400}, 17, 9, 16);
            sendAssert(env, "E7", 7, new int[] {1, 3}, new long[] {11, 21}, new double[] {300, 400}, 11, 10, 23);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String id, int value, int[] ints, long[] longs, double[] doubles, int expectedIntArray, int expectedLongArray, int expectedDoubleArray) {
            env.sendEventBean(new SupportThreeArrayEvent(id, value, ints, longs, doubles));
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            assertEquals(expectedIntArray, events[0].get("thesum"));
            assertEquals(expectedLongArray, events[1].get("thesum"));
            assertEquals(expectedDoubleArray, events[2].get("thesum"));
        }
    }

    public static class ResultSetQueryTypeRollupMultikeyWArray implements RegressionExecution {
        private final boolean join;
        private final boolean unbound;

        public ResultSetQueryTypeRollupMultikeyWArray(boolean join, boolean unbound) {
            this.join = join;
            this.unbound = unbound;
        }

        public void run(RegressionEnvironment env) {
            String epl = join ?
                "@Name('s0') select array, value, count(*) as cnt from SupportEventWithIntArray#keepall, SupportBean#keepall group by rollup(array, value)" :
                (unbound ?
                    "@Name('s0') select array, value, count(*) as cnt from SupportEventWithIntArray group by rollup(array, value)" :
                    "@Name('s0') select array, value, count(*) as cnt from SupportEventWithIntArray#keepall group by rollup(array, value)"
                );

            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean());

            sendAssertIntArray(env, "E1", new int[] {1, 2}, 5, 1, 1, 1);

            env.milestone(0);

            sendAssertIntArray(env, "E2", new int[] {1, 2}, 5, 2, 2, 2);
            sendAssertIntArray(env, "E3", new int[] {4, 5}, 5, 3, 1, 1);
            sendAssertIntArray(env, "E4", new int[] {1, 2}, 6, 4, 3, 1);

            env.milestone(1);

            sendAssertIntArray(env, "E5", new int[] {1, 2}, 5, 5, 4, 3);
            sendAssertIntArray(env, "E6", new int[] {4, 5}, 5, 6, 2, 2);
            sendAssertIntArray(env, "E7", new int[] {1, 2}, 6, 7, 5, 2);
            sendAssertIntArray(env, "E8", new int[] {1}, 5, 8, 1, 1);

            env.undeployAll();
        }

        private void sendAssertIntArray(RegressionEnvironment env, String id, int[] array, int value, long expectedTotal, long expectedByArray, long expectedByArrayAndValue) {
            final String[] fields = new String[] {"array", "value", "cnt"};
            env.sendEventBean(new SupportEventWithIntArray(id, array, value));
            EventBean[] out = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertProps(out[0], fields, new Object[] {array, value, expectedByArrayAndValue});
            EPAssertionUtil.assertProps(out[1], fields, new Object[] {array, null, expectedByArray});
            EPAssertionUtil.assertProps(out[2], fields, new Object[] {null, null, expectedTotal});
        }
    }

    private static class ResultSetQueryTypeOutputWhenTerminated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionOutputWhenTerminated(env, "last", outputLimitOpt, milestone);
            }

            tryAssertionOutputWhenTerminated(env, "all", SupportOutputLimitOpt.DEFAULT, milestone);
            tryAssertionOutputWhenTerminated(env, "snapshot", SupportOutputLimitOpt.DEFAULT, milestone);
        }
    }

    private static class ResultSetQueryTypeGroupByWithComputation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select longPrimitive as c0, sum(intPrimitive) as c1 " +
                "from SupportBean group by rollup(case when longPrimitive > 0 then 1 else 0 end)";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("c0"));
            String[] fields = "c0,c1".split(",");

            env.sendEventBean(makeEvent("E1", 1, 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{10L, 1}, {null, 1}});

            env.sendEventBean(makeEvent("E2", 2, 11));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{11L, 3}, {null, 3}});

            env.sendEventBean(makeEvent("E3", 5, -10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{-10L, 5}, {null, 8}});

            env.sendEventBean(makeEvent("E4", 6, -11));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{-11L, 11}, {null, 14}});

            env.sendEventBean(makeEvent("E5", 3, 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{12L, 6}, {null, 17}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeContextPartitionAlsoRollup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context SegmentedByString partition by theString from SupportBean;\n" +
                "@name('s0') context SegmentedByString select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean group by rollup(theString, intPrimitive)";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2".split(",");
            env.milestone(0);

            env.sendEventBean(makeEvent("E1", 1, 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 1, 10L}, {"E1", null, 10L}, {null, null, 10L}});

            env.sendEventBean(makeEvent("E1", 2, 20));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 2, 20L}, {"E1", null, 30L}, {null, null, 30L}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E2", 1, 25));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 1, 25L}, {"E2", null, 25L}, {null, null, 25L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeOnSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "@name('s0') on SupportBean_S0 as s0 select mw.theString as c0, sum(mw.intPrimitive) as c1, count(*) as c2 from MyWindow mw group by rollup(mw.theString);\n";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2".split(",");

            // {E0, 0}, {E1, 1}, {E2, 2}, {E0, 3}, {E1, 4}, {E2, 5}, {E0, 6}, {E1, 7}, {E2, 8}, {E0, 9}
            for (int i = 0; i < 10; i++) {
                String theString = "E" + i % 3;
                env.sendEventBean(new SupportBean(theString, i));
            }

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E0", 18, 4L}, {"E1", 12, 3L}, {"E2", 15, 3L}, {null, 18 + 12 + 15, 10L}});

            env.sendEventBean(new SupportBean("E1", 6));
            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E0", 18, 4L}, {"E1", 12 + 6, 4L}, {"E2", 15, 3L}, {null, 18 + 12 + 15 + 6, 11L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundRollupUnenclosed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionUnboundRollupUnenclosed(env, "theString, rollup(intPrimitive, longPrimitive)", milestone);
            tryAssertionUnboundRollupUnenclosed(env, "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "theString)", milestone);
            tryAssertionUnboundRollupUnenclosed(env, "theString, grouping sets(" +
                "(intPrimitive, longPrimitive)," +
                "(intPrimitive), ())", milestone);
        }
    }

    private static class ResultSetQueryTypeUnboundCubeUnenclosed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionUnboundCubeUnenclosed(env, "theString, cube(intPrimitive, longPrimitive)", milestone);
            tryAssertionUnboundCubeUnenclosed(env, "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "(theString, longPrimitive)," +
                "theString)", milestone);
            tryAssertionUnboundCubeUnenclosed(env, "theString, grouping sets(" +
                "(intPrimitive, longPrimitive)," +
                "(intPrimitive)," +
                "(longPrimitive)," +
                "())", milestone);
        }

        private static void tryAssertionUnboundCubeUnenclosed(RegressionEnvironment env, String groupBy, AtomicInteger milestone) {

            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, sum(doublePrimitive) as c3 from SupportBean " +
                "group by " + groupBy;
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("E1", 10, 100, 1000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L, 1000d}, {"E1", 10, null, 1000d}, {"E1", null, 100L, 1000d}, {"E1", null, null, 1000d}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E1", 10, 200, 2000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 200L, 2000d}, {"E1", 10, null, 3000d}, {"E1", null, 200L, 2000d}, {"E1", null, null, 3000d}});

            env.sendEventBean(makeEvent("E1", 20, 100, 4000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, 100L, 4000d}, {"E1", 20, null, 4000d}, {"E1", null, 100L, 5000d}, {"E1", null, null, 7000d}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 10, 100, 5000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 10, 100L, 5000d}, {"E2", 10, null, 5000d}, {"E2", null, 100L, 5000d}, {"E2", null, null, 5000d}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundGroupingSet2LevelUnenclosed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionUnboundGroupingSet2LevelUnenclosed(env, "theString, grouping sets(intPrimitive, longPrimitive)");
            tryAssertionUnboundGroupingSet2LevelUnenclosed(env, "grouping sets((theString, intPrimitive), (theString, longPrimitive))");
        }

        private static void tryAssertionUnboundGroupingSet2LevelUnenclosed(RegressionEnvironment env, String groupBy) {

            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, sum(doublePrimitive) as c3 from SupportBean " +
                "group by " + groupBy;
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c1"));
            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("c2"));

            env.sendEventBean(makeEvent("E1", 10, 100, 1000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, null, 1000d}, {"E1", null, 100L, 1000d}});

            env.sendEventBean(makeEvent("E1", 20, 200, 2000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, null, 2000d}, {"E1", null, 200L, 2000d}});

            env.sendEventBean(makeEvent("E1", 10, 200, 3000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, null, 4000d}, {"E1", null, 200L, 5000d}});

            env.sendEventBean(makeEvent("E1", 20, 100, 4000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 20, null, 6000d}, {"E1", null, 100L, 5000d}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeBoundGroupingSet2LevelNoTopNoDetail implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#length(4) " +
                "group by grouping sets(theString, intPrimitive)";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c1"));

            env.sendEventBean(makeEvent("E1", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 100L}, {null, 10, 100L}},
                new Object[][]{{"E1", null, null}, {null, 10, null}});

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 20, 200));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", null, 200L}, {null, 20, 200L}},
                new Object[][]{{"E2", null, null}, {null, 20, null}});

            env.sendEventBean(makeEvent("E1", 20, 300));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", null, 400L}, {null, 20, 500L}},
                new Object[][]{{"E1", null, 100L}, {null, 20, 200L}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E2", 10, 400));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", null, 600L}, {null, 10, 500L}},
                new Object[][]{{"E2", null, 200L}, {null, 10, 100L}});

            env.sendEventBean(makeEvent("E2", 20, 500));   // removes E1/10/100
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", null, 1100L}, {"E1", null, 300L}, {null, 20, 1000L}, {null, 10, 400L}},
                new Object[][]{{"E2", null, 600L}, {"E1", null, 400L}, {null, 20, 500L}, {null, 10, 500L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeBoundGroupingSet2LevelTopAndDetail implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#length(4) " +
                "group by grouping sets((), (theString, intPrimitive))";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c1"));

            env.sendEventBean(makeEvent("E1", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 100L}, {"E1", 10, 100L}},
                new Object[][]{{null, null, null}, {"E1", 10, null}});

            env.sendEventBean(makeEvent("E1", 10, 200));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 300L}, {"E1", 10, 300L}},
                new Object[][]{{null, null, 100L}, {"E1", 10, 100L}});

            env.sendEventBean(makeEvent("E2", 20, 300));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 600L}, {"E2", 20, 300L}},
                new Object[][]{{null, null, 300L}, {"E2", 20, null}});

            env.milestone(0);

            env.sendEventBean(makeEvent("E1", 10, 400));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{null, null, 1000L}, {"E1", 10, 700L}},
                new Object[][]{{null, null, 600L}, {"E1", 10, 300L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundCube4Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, longPrimitive as c2, doublePrimitive as c3, sum(intBoxed) as c4 from SupportBean " +
                "group by cube(theString, intPrimitive, longPrimitive, doublePrimitive)";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c1"));
            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("c2"));
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("c3"));

            env.sendEventBean(makeEvent("E1", 1, 10, 100, 1000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E1", 1, 10L, 100d, 1000},  // {0, 1, 2, 3}
                    {"E1", 1, 10L, null, 1000},  // {0, 1, 2}
                    {"E1", 1, null, 100d, 1000},  // {0, 1, 3}
                    {"E1", 1, null, null, 1000},  // {0, 1}
                    {"E1", null, 10L, 100d, 1000},  // {0, 2, 3}
                    {"E1", null, 10L, null, 1000},  // {0, 2}
                    {"E1", null, null, 100d, 1000},  // {0, 3}
                    {"E1", null, null, null, 1000},  // {0}
                    {null, 1, 10L, 100d, 1000},  // {1, 2, 3}
                    {null, 1, 10L, null, 1000},  // {1, 2}
                    {null, 1, null, 100d, 1000},  // {1, 3}
                    {null, 1, null, null, 1000},  // {1}
                    {null, null, 10L, 100d, 1000},  // {2, 3}
                    {null, null, 10L, null, 1000},  // {2}
                    {null, null, null, 100d, 1000},  // {3}
                    {null, null, null, null, 1000}   // {}
                });

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 1, 20, 100, 2000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 1, 20L, 100d, 2000},  // {0, 1, 2, 3}
                    {"E2", 1, 20L, null, 2000},  // {0, 1, 2}
                    {"E2", 1, null, 100d, 2000},  // {0, 1, 3}
                    {"E2", 1, null, null, 2000},  // {0, 1}
                    {"E2", null, 20L, 100d, 2000},  // {0, 2, 3}
                    {"E2", null, 20L, null, 2000},  // {0, 2}
                    {"E2", null, null, 100d, 2000},  // {0, 3}
                    {"E2", null, null, null, 2000},  // {0}
                    {null, 1, 20L, 100d, 2000},  // {1, 2, 3}
                    {null, 1, 20L, null, 2000},  // {1, 2}
                    {null, 1, null, 100d, 3000},  // {1, 3}
                    {null, 1, null, null, 3000},  // {1}
                    {null, null, 20L, 100d, 2000},  // {2, 3}
                    {null, null, 20L, null, 2000},  // {2}
                    {null, null, null, 100d, 3000},  // {3}
                    {null, null, null, null, 3000}   // {}
                });

            env.milestone(1);

            env.sendEventBean(makeEvent("E1", 2, 10, 100, 4000));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E1", 2, 10L, 100d, 4000},  // {0, 1, 2, 3}
                    {"E1", 2, 10L, null, 4000},  // {0, 1, 2}
                    {"E1", 2, null, 100d, 4000},  // {0, 1, 3}
                    {"E1", 2, null, null, 4000},  // {0, 1}
                    {"E1", null, 10L, 100d, 5000},  // {0, 2, 3}
                    {"E1", null, 10L, null, 5000},  // {0, 2}
                    {"E1", null, null, 100d, 5000},  // {0, 3}
                    {"E1", null, null, null, 5000},  // {0}
                    {null, 2, 10L, 100d, 4000},  // {1, 2, 3}
                    {null, 2, 10L, null, 4000},  // {1, 2}
                    {null, 2, null, 100d, 4000},  // {1, 3}
                    {null, 2, null, null, 4000},  // {1}
                    {null, null, 10L, 100d, 5000},  // {2, 3}
                    {null, null, 10L, null, 5000},  // {2}
                    {null, null, null, 100d, 7000},  // {3}
                    {null, null, null, null, 7000}   // {}
                });

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeBoundCube3Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionBoundCube(env, "cube(theString, intPrimitive, longPrimitive)", milestone);
            tryAssertionBoundCube(env, "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "(theString, longPrimitive)," +
                "(theString)," +
                "(intPrimitive, longPrimitive)," +
                "(intPrimitive)," +
                "(longPrimitive)," +
                "()" +
                ")", milestone);
        }
    }

    private static class ResultSetQueryTypeNamedWindowCube2Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionNamedWindowCube2Dim(env, "cube(theString, intPrimitive)");
            tryAssertionNamedWindowCube2Dim(env, "grouping sets(" +
                "(theString, intPrimitive)," +
                "(theString)," +
                "(intPrimitive)," +
                "()" +
                ")");
        }

        private static void tryAssertionNamedWindowCube2Dim(RegressionEnvironment env, String groupBy) {

            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean(intBoxed = 0);\n" +
                "on SupportBean(intBoxed = 3) delete from MyWindow;\n" +
                "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from MyWindow " +
                "group by " + groupBy;
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2".split(",");

            env.sendEventBean(makeEvent(0, "E1", 10, 100));    // insert event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, 10, 100L}, {null, null, 100L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, 10, null}, {null, null, null}});

            env.milestone(0);

            env.sendEventBean(makeEvent(0, "E1", 11, 200));    // insert event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 11, 200L}, {"E1", null, 300L}, {null, 11, 200L}, {null, null, 300L}},
                new Object[][]{{"E1", 11, null}, {"E1", null, 100L}, {null, 11, null}, {null, null, 100L}});

            env.sendEventBean(makeEvent(0, "E1", 10, 300));    // insert event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 600L}, {null, 10, 400L}, {null, null, 600L}},
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 300L}, {null, 10, 100L}, {null, null, 300L}});

            env.sendEventBean(makeEvent(0, "E2", 11, 400));    // insert event
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 11, 400L}, {"E2", null, 400L}, {null, 11, 600L}, {null, null, 1000L}},
                new Object[][]{{"E2", 11, null}, {"E2", null, null}, {null, 11, 200L}, {null, null, 600L}});

            env.milestone(1);

            env.sendEventBean(makeEvent(3, null, -1, -1));    // delete-all
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", 11, null}, {"E2", 11, null},
                    {"E1", null, null}, {"E2", null, null}, {null, 10, null}, {null, 11, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 400L}, {"E1", 11, 200L}, {"E2", 11, 400L},
                    {"E1", null, 600L}, {"E2", null, 400L}, {null, 10, 400L}, {null, 11, 600L}, {null, null, 1000L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeNamedWindowDeleteAndRStream2Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionNamedWindowDeleteAndRStream2Dim(env, "rollup(theString, intPrimitive)", milestone);
            tryAssertionNamedWindowDeleteAndRStream2Dim(env, "grouping sets(" +
                "(theString, intPrimitive)," +
                "(theString)," +
                "())", milestone);
        }

        private static void tryAssertionNamedWindowDeleteAndRStream2Dim(RegressionEnvironment env, String groupBy, AtomicInteger milestone) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean(intBoxed = 0);\n" +
                "on SupportBean(intBoxed = 1) as sb " +
                "delete from MyWindow mw where sb.theString = mw.theString and sb.intPrimitive = mw.intPrimitive;\n" +
                "on SupportBean(intBoxed = 2) as sb " +
                "delete from MyWindow mw where sb.theString = mw.theString and sb.intPrimitive = mw.intPrimitive and sb.longPrimitive = mw.longPrimitive;\n" +
                "on SupportBean(intBoxed = 3) delete from MyWindow;\n" +
                "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from MyWindow " +
                "group by " + groupBy;
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent(0, "E1", 10, 100));    // insert event intBoxed=0
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(1, "E1", 10, 100));   // delete (intBoxed = 1)
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

            env.sendEventBean(makeEvent(0, "E1", 10, 200));   // insert
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 200L}, {"E1", null, 200L}, {null, null, 200L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(0, "E2", 20, 300));   // insert
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 20, 300L}, {"E2", null, 300L}, {null, null, 500L}},
                new Object[][]{{"E2", 20, null}, {"E2", null, null}, {null, null, 200L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(3, null, 0, 0));   // delete all
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 10, null}, {"E2", 20, null},
                    {"E1", null, null}, {"E2", null, null},
                    {null, null, null}},
                new Object[][]{
                    {"E1", 10, 200L}, {"E2", 20, 300L},
                    {"E1", null, 200L}, {"E2", null, 300L},
                    {null, null, 500L}});

            env.sendEventBean(makeEvent(0, "E1", 10, 400));   // insert
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 400L}, {null, null, 400L}},
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}});

            env.sendEventBean(makeEvent(0, "E1", 20, 500));   // insert
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 20, 500L}, {"E1", null, 900L}, {null, null, 900L}},
                new Object[][]{{"E1", 20, null}, {"E1", null, 400L}, {null, null, 400L}});

            env.sendEventBean(makeEvent(0, "E2", 20, 600));   // insert
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 20, 600L}, {"E2", null, 600L}, {null, null, 1500L}},
                new Object[][]{{"E2", 20, null}, {"E2", null, null}, {null, null, 900L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(0, "E1", 10, 700));   // insert
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 1100L}, {"E1", null, 1600L}, {null, null, 2200L}},
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 900L}, {null, null, 1500L}});

            env.sendEventBean(makeEvent(3, null, 0, 0));   // delete all
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{
                    {"E1", 10, null}, {"E1", 20, null}, {"E2", 20, null},
                    {"E1", null, null}, {"E2", null, null},
                    {null, null, null}},
                new Object[][]{
                    {"E1", 10, 1100L}, {"E1", 20, 500L}, {"E2", 20, 600L},
                    {"E1", null, 1600L}, {"E2", null, 600L},
                    {null, null, 2200L}});

            env.sendEventBean(makeEvent(0, "E1", 10, 100));   // insert
            env.sendEventBean(makeEvent(0, "E1", 20, 200));   // insert
            env.sendEventBean(makeEvent(0, "E1", 10, 300));   // insert
            env.sendEventBean(makeEvent(0, "E1", 20, 400));   // insert
            env.listener("s0").reset();

            env.sendEventBean(makeEvent(1, "E1", 20, -1));   // delete (intBoxed = 1)
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 20, null}, {"E1", null, 400L}, {null, null, 400L}},
                new Object[][]{{"E1", 20, 600L}, {"E1", null, 1000L}, {null, null, 1000L}});

            env.sendEventBean(makeEvent(1, "E1", 10, -1));   // delete (intBoxed = 1)
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 400L}, {null, null, 400L}});

            env.sendEventBean(makeEvent(0, "E1", 10, 100));   // insert
            env.sendEventBean(makeEvent(0, "E1", 10, 200));   // insert

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(0, "E1", 10, 300));   // insert
            env.sendEventBean(makeEvent(0, "E1", 20, 400));   // insert
            env.sendEventBean(makeEvent(0, "E2", 20, 500));   // insert
            env.listener("s0").reset();

            env.sendEventBean(makeEvent(2, "E1", 10, 200));   // delete specific (intBoxed = 2)
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 800L}, {null, null, 1300L}},
                new Object[][]{{"E1", 10, 600L}, {"E1", null, 1000L}, {null, null, 1500L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(2, "E1", 10, 300));   // delete specific
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 500L}, {null, null, 1000L}},
                new Object[][]{{"E1", 10, 400L}, {"E1", null, 800L}, {null, null, 1300L}});

            env.sendEventBean(makeEvent(2, "E1", 20, 400));   // delete specific
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 20, null}, {"E1", null, 100L}, {null, null, 600L}},
                new Object[][]{{"E1", 20, 400L}, {"E1", null, 500L}, {null, null, 1000L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent(2, "E2", 20, 500));   // delete specific
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E2", 20, null}, {"E2", null, null}, {null, null, 100L}},
                new Object[][]{{"E2", 20, 500L}, {"E2", null, 500L}, {null, null, 600L}});

            env.sendEventBean(makeEvent(2, "E1", 10, 100));   // delete specific
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, null}, {"E1", null, null}, {null, null, null}},
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeBoundRollup2Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionBoundRollup2Dim(env, false, milestone);
            tryAssertionBoundRollup2Dim(env, true, milestone);
        }

        private static void tryAssertionBoundRollup2Dim(RegressionEnvironment env, boolean join, AtomicInteger milestone) {

            String[] fields = "c0,c1,c2".split(",");
            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 " +
                "from SupportBean#length(3) " + (join ? ", SupportBean_S0#lastevent " : "") +
                "group by rollup(theString, intPrimitive)";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));

            env.sendEventBean(makeEvent("E1", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 20, 200));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 200L}, {"E2", null, 200L}, {null, null, 300L}});

            env.sendEventBean(makeEvent("E1", 11, 300));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 11, 300L}, {"E1", null, 400L}, {null, null, 600L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 20, 400));   // expires {theString="E1", intPrimitive=10, longPrimitive=100}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 20, 600L}, {"E1", 10, null},
                    {"E2", null, 600L}, {"E1", null, 300L},
                    {null, null, 900L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 20, 500));   // expires {theString="E2", intPrimitive=20, longPrimitive=200}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 20, 900L},
                    {"E2", null, 900L},
                    {null, null, 1200L}});

            env.sendEventBean(makeEvent("E2", 21, 600));   // expires {theString="E1", intPrimitive=11, longPrimitive=300}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 21, 600L}, {"E1", 11, null},
                    {"E2", null, 1500L}, {"E1", null, null},
                    {null, null, 1500L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 21, 700));   // expires {theString="E2", intPrimitive=20, longPrimitive=400}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 21, 1300L}, {"E2", 20, 500L},
                    {"E2", null, 1800L},
                    {null, null, 1800L}});

            env.sendEventBean(makeEvent("E2", 21, 800));   // expires {theString="E2", intPrimitive=20, longPrimitive=500}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 21, 2100L}, {"E2", 20, null},
                    {"E2", null, 2100L},
                    {null, null, 2100L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E1", 10, 900));   // expires {theString="E2", intPrimitive=21, longPrimitive=600}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E1", 10, 900L}, {"E2", 21, 1500L},
                    {"E1", null, 900L}, {"E2", null, 1500L},
                    {null, null, 2400L}});

            env.sendEventBean(makeEvent("E1", 11, 1000));   // expires {theString="E2", intPrimitive=21, longPrimitive=700}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E1", 11, 1000L}, {"E2", 21, 800L},
                    {"E1", null, 1900L}, {"E2", null, 800L},
                    {null, null, 2700L}});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("E2", 20, 1100));   // expires {theString="E2", intPrimitive=21, longPrimitive=800}
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{
                    {"E2", 20, 1100L}, {"E2", 21, null},
                    {"E2", null, 1100L},
                    {null, null, 3000L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundRollup2Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@Name('s0')" +
                "select theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean " +
                "group by rollup(theString, intPrimitive)";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c1"));

            env.sendEventBean(makeEvent("E1", 10, 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E1", null, 100L}, {null, null, 100L}});

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 20, 200));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 200L}, {"E2", null, 200L}, {null, null, 300L}});

            env.sendEventBean(makeEvent("E1", 11, 300));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 11, 300L}, {"E1", null, 400L}, {null, null, 600L}});

            env.milestone(1);

            env.sendEventBean(makeEvent("E2", 20, 400));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", 20, 600L}, {"E2", null, 600L}, {null, null, 1000L}});

            env.sendEventBean(makeEvent("E1", 11, 500));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 11, 800L}, {"E1", null, 900L}, {null, null, 1500L}});

            env.milestone(2);

            env.sendEventBean(makeEvent("E1", 10, 600));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", 10, 700L}, {"E1", null, 1500L}, {null, null, 2100L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundRollup1Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionUnboundRollup1Dim(env, "rollup(theString)", milestone);
            tryAssertionUnboundRollup1Dim(env, "cube(theString)", milestone);
        }
    }

    private static class ResultSetQueryTypeUnboundRollup2DimBatchWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0')" +
                "select irstream theString as c0, intPrimitive as c1, sum(longPrimitive) as c2 from SupportBean#length_batch(4) " +
                "group by rollup(theString, intPrimitive)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("E1", 10, 100));
            env.sendEventBean(makeEvent("E2", 20, 200));
            env.sendEventBean(makeEvent("E1", 11, 300));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(makeEvent("E2", 20, 400));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1", 10, 100L}, {"E2", 20, 600L}, {"E1", 11, 300L},
                    {"E1", null, 400L}, {"E2", null, 600L},
                    {null, null, 1000L}},
                new Object[][]{{"E1", 10, null}, {"E2", 20, null}, {"E1", 11, null},
                    {"E1", null, null}, {"E2", null, null},
                    {null, null, null}});

            env.sendEventBean(makeEvent("E1", 11, 500));
            env.sendEventBean(makeEvent("E2", 20, 600));
            env.sendEventBean(makeEvent("E1", 11, 700));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.sendEventBean(makeEvent("E2", 20, 800));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getDataListsFlattened(), fields,
                new Object[][]{{"E1", 11, 1200L}, {"E2", 20, 1400L}, {"E1", 10, null},
                    {"E1", null, 1200L}, {"E2", null, 1400L},
                    {null, null, 2600L}},
                new Object[][]{{"E1", 11, 300L}, {"E2", 20, 600L}, {"E1", 10, 100L},
                    {"E1", null, 400L}, {"E2", null, 600L},
                    {null, null, 1000L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundRollup3Dim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String rollupEpl = "rollup(theString, intPrimitive, longPrimitive)";
            tryAssertionUnboundRollup3Dim(env, rollupEpl, false, milestone);
            tryAssertionUnboundRollup3Dim(env, rollupEpl, true, milestone);

            String gsEpl = "grouping sets(" +
                "(theString, intPrimitive, longPrimitive)," +
                "(theString, intPrimitive)," +
                "(theString)," +
                "()" +
                ")";
            tryAssertionUnboundRollup3Dim(env, gsEpl, false, milestone);
            tryAssertionUnboundRollup3Dim(env, gsEpl, true, milestone);
        }
    }

    private static class ResultSetQueryTypeMixedAccessAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@name('s0') select sum(intPrimitive) as c0, theString as c1, window(*) as c2 " +
                "from SupportBean#length(2) sb group by rollup(theString) order by theString";
            env.compileDeploy(epl).addListener("s0");

            Object eventOne = new SupportBean("E1", 1);
            env.sendEventBean(eventOne);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{1, null, new Object[]{eventOne}}, {1, "E1", new Object[]{eventOne}}});

            Object eventTwo = new SupportBean("E1", 2);
            env.sendEventBean(eventTwo);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{3, null, new Object[]{eventOne, eventTwo}}, {3, "E1", new Object[]{eventOne, eventTwo}}});

            env.milestone(0);

            Object eventThree = new SupportBean("E2", 3);
            env.sendEventBean(eventThree);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{5, null, new Object[]{eventTwo, eventThree}}, {2, "E1", new Object[]{eventTwo}}, {3, "E2", new Object[]{eventThree}}});

            Object eventFour = new SupportBean("E1", 4);
            env.sendEventBean(eventFour);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{7, null, new Object[]{eventThree, eventFour}}, {4, "E1", new Object[]{eventFour}}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeNonBoxedTypeWithRollup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatement stmtOne = env.compileDeploy("@name('s0') select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by intPrimitive, rollup(doublePrimitive, longPrimitive)").statement("s0");
            assertTypesC0C1C2(stmtOne, Integer.class, Double.class, Long.class);

            EPStatement stmtTwo = env.compileDeploy("@name('s1') select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by grouping sets ((intPrimitive, doublePrimitive, longPrimitive))").statement("s1");
            assertTypesC0C1C2(stmtTwo, Integer.class, Double.class, Long.class);

            EPStatement stmtThree = env.compileDeploy("@name('s2') select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by grouping sets ((intPrimitive, doublePrimitive, longPrimitive), (intPrimitive, doublePrimitive))").statement("s2");
            assertTypesC0C1C2(stmtThree, Integer.class, Double.class, Long.class);

            EPStatement stmtFour = env.compileDeploy("@name('s3') select intPrimitive as c0, doublePrimitive as c1, longPrimitive as c2, sum(shortPrimitive) " +
                "from SupportBean group by grouping sets ((doublePrimitive, intPrimitive), (longPrimitive, intPrimitive))").statement("s3");
            assertTypesC0C1C2(stmtFour, Integer.class, Double.class, Long.class);

            env.undeployAll();
        }
    }


    private static class ResultSetQueryTypeInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String prefix = "select theString, sum(intPrimitive) from SupportBean group by ";

            // invalid rollup expressions
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "rollup()",
                "Incorrect syntax near ')' at line 1 column 69, please check the group-by clause [select theString, sum(intPrimitive) from SupportBean group by rollup()]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "rollup(theString, theString)",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString) [select theString, sum(intPrimitive) from SupportBean group by rollup(theString, theString)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "rollup(x)",
                "Failed to validate group-by-clause expression 'x': Property named 'x' is not valid in any stream [select theString, sum(intPrimitive) from SupportBean group by rollup(x)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "rollup(longPrimitive)",
                "Group-by with rollup requires a fully-aggregated query, the query is not full-aggregated because of property 'theString' [select theString, sum(intPrimitive) from SupportBean group by rollup(longPrimitive)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "rollup((theString, longPrimitive), (theString, longPrimitive))",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString, longPrimitive) [select theString, sum(intPrimitive) from SupportBean group by rollup((theString, longPrimitive), (theString, longPrimitive))]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "rollup((theString, longPrimitive), (longPrimitive, theString))",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString, longPrimitive) [select theString, sum(intPrimitive) from SupportBean group by rollup((theString, longPrimitive), (longPrimitive, theString))]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "grouping sets((theString, theString))",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString) [select theString, sum(intPrimitive) from SupportBean group by grouping sets((theString, theString))]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "grouping sets(theString, theString)",
                "Failed to validate the group-by clause, found duplicate specification of expressions (theString) [select theString, sum(intPrimitive) from SupportBean group by grouping sets(theString, theString)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "grouping sets((), ())",
                "Failed to validate the group-by clause, found duplicate specification of the overall grouping '()' [select theString, sum(intPrimitive) from SupportBean group by grouping sets((), ())]");
            SupportMessageAssertUtil.tryInvalidCompile(env, prefix + "grouping sets(())",
                "Failed to validate the group-by clause, the overall grouping '()' cannot be the only grouping [select theString, sum(intPrimitive) from SupportBean group by grouping sets(())]");

            // invalid select clause for this type of query
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean group by grouping sets(theString)",
                "Group-by with rollup requires that the select-clause does not use wildcard [select * from SupportBean group by grouping sets(theString)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sb.* from SupportBean sb group by grouping sets(theString)",
                "Group-by with rollup requires that the select-clause does not use wildcard [select sb.* from SupportBean sb group by grouping sets(theString)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "@Hint('disable_reclaim_group') select theString, count(*) from SupportBean sb group by grouping sets(theString)",
                "Reclaim hints are not available with rollup [@Hint('disable_reclaim_group') select theString, count(*) from SupportBean sb group by grouping sets(theString)]");
        }
    }

    private static void tryAssertionOutputWhenTerminated(RegressionEnvironment env, String outputLimit, SupportOutputLimitOpt opt, AtomicInteger milestone) {
        String epl = "@name('ctx') create context MyContext start SupportBean_S0(id=1) end SupportBean_S0(id=0);\n" +
            "@name('s0') context MyContext select theString as c0, sum(intPrimitive) as c1 " +
            "from SupportBean group by rollup(theString) output " + outputLimit + " when terminated";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1));
        env.sendEventBean(new SupportBean("E1", 1));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 2));
        env.sendEventBean(new SupportBean("E1", 3));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "c0,c1".split(","),
            new Object[][]{{"E1", 4}, {"E2", 2}, {null, 6}});

        env.sendEventBean(new SupportBean_S0(1));
        env.sendEventBean(new SupportBean("E2", 4));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E1", 5));
        env.sendEventBean(new SupportBean("E1", 6));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "c0,c1".split(","),
            new Object[][]{{"E2", 4}, {"E1", 11}, {null, 15}});

        env.undeployAll();
    }

    private static void tryAssertionBoundCube(RegressionEnvironment env, String groupBy, AtomicInteger milestone) {

        String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(",");

        String epl = "@Name('s0')" +
            "select theString as c0, " +
            "intPrimitive as c1, " +
            "longPrimitive as c2, " +
            "count(*) as c3, " +
            "sum(doublePrimitive) as c4," +
            "grouping(theString) as c5," +
            "grouping(intPrimitive) as c6," +
            "grouping(longPrimitive) as c7," +
            "grouping_id(theString, intPrimitive, longPrimitive) as c8 " +
            "from SupportBean#length(4) " +
            "group by " + groupBy;
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(makeEvent("E1", 1, 10, 100));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{
                {"E1", 1, 10L, 1L, 100d, 0, 0, 0, 0},  // {0, 1, 2}
                {"E1", 1, null, 1L, 100d, 0, 0, 1, 1},  // {0, 1}
                {"E1", null, 10L, 1L, 100d, 0, 1, 0, 2},  // {0, 2}
                {"E1", null, null, 1L, 100d, 0, 1, 1, 3},  // {0}
                {null, 1, 10L, 1L, 100d, 1, 0, 0, 4},  // {1, 2}
                {null, 1, null, 1L, 100d, 1, 0, 1, 5},  // {1}
                {null, null, 10L, 1L, 100d, 1, 1, 0, 6},  // {2}
                {null, null, null, 1L, 100d, 1, 1, 1, 7}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E2", 1, 20, 200));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{
                {"E2", 1, 20L, 1L, 200d, 0, 0, 0, 0},
                {"E2", 1, null, 1L, 200d, 0, 0, 1, 1},
                {"E2", null, 20L, 1L, 200d, 0, 1, 0, 2},
                {"E2", null, null, 1L, 200d, 0, 1, 1, 3},
                {null, 1, 20L, 1L, 200d, 1, 0, 0, 4},
                {null, 1, null, 2L, 300d, 1, 0, 1, 5},
                {null, null, 20L, 1L, 200d, 1, 1, 0, 6},
                {null, null, null, 2L, 300d, 1, 1, 1, 7}});

        env.sendEventBean(makeEvent("E1", 2, 10, 300));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{
                {"E1", 2, 10L, 1L, 300d, 0, 0, 0, 0},
                {"E1", 2, null, 1L, 300d, 0, 0, 1, 1},
                {"E1", null, 10L, 2L, 400d, 0, 1, 0, 2},
                {"E1", null, null, 2L, 400d, 0, 1, 1, 3},
                {null, 2, 10L, 1L, 300d, 1, 0, 0, 4},
                {null, 2, null, 1L, 300d, 1, 0, 1, 5},
                {null, null, 10L, 2L, 400d, 1, 1, 0, 6},
                {null, null, null, 3L, 600d, 1, 1, 1, 7}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E2", 2, 20, 400));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{
                {"E2", 2, 20L, 1L, 400d, 0, 0, 0, 0},
                {"E2", 2, null, 1L, 400d, 0, 0, 1, 1},
                {"E2", null, 20L, 2L, 600d, 0, 1, 0, 2},
                {"E2", null, null, 2L, 600d, 0, 1, 1, 3},
                {null, 2, 20L, 1L, 400d, 1, 0, 0, 4},
                {null, 2, null, 2L, 700d, 1, 0, 1, 5},
                {null, null, 20L, 2L, 600d, 1, 1, 0, 6},
                {null, null, null, 4L, 1000d, 1, 1, 1, 7}});

        env.milestoneInc(milestone);

        // expiring/removing ("E1", 1, 10, 100)
        env.sendEventBean(makeEvent("E2", 1, 10, 500));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{
                {"E2", 1, 10L, 1L, 500d, 0, 0, 0, 0},
                {"E1", 1, 10L, 0L, null, 0, 0, 0, 0},
                {"E2", 1, null, 2L, 700d, 0, 0, 1, 1},
                {"E1", 1, null, 0L, null, 0, 0, 1, 1},
                {"E2", null, 10L, 1L, 500d, 0, 1, 0, 2},
                {"E1", null, 10L, 1L, 300d, 0, 1, 0, 2},
                {"E2", null, null, 3L, 1100d, 0, 1, 1, 3},
                {"E1", null, null, 1L, 300d, 0, 1, 1, 3},
                {null, 1, 10L, 1L, 500d, 1, 0, 0, 4},
                {null, 1, null, 2L, 700d, 1, 0, 1, 5},
                {null, null, 10L, 2L, 800d, 1, 1, 0, 6},
                {null, null, null, 4L, 1400d, 1, 1, 1, 7}});

        env.undeployAll();
    }

    private static void tryAssertionUnboundRollupUnenclosed(RegressionEnvironment env, String groupBy, AtomicInteger milestone) {

        String[] fields = "c0,c1,c2,c3".split(",");
        String epl = "@Name('s0')" +
            "select theString as c0, intPrimitive as c1, longPrimitive as c2, sum(doublePrimitive) as c3 from SupportBean " +
            "group by " + groupBy;
        env.compileDeploy(epl).addListener("s0");

        assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c1"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("c2"));

        env.sendEventBean(makeEvent("E1", 10, 100, 1000));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 10, 100L, 1000d}, {"E1", 10, null, 1000d}, {"E1", null, null, 1000d}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 10, 200, 2000));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 10, 200L, 2000d}, {"E1", 10, null, 3000d}, {"E1", null, null, 3000d}});

        env.sendEventBean(makeEvent("E1", 20, 100, 3000));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 20, 100L, 3000d}, {"E1", 20, null, 3000d}, {"E1", null, null, 6000d}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 10, 100, 4000));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 10, 100L, 5000d}, {"E1", 10, null, 7000d}, {"E1", null, null, 10000d}});

        env.undeployAll();
    }

    private static void tryAssertionUnboundRollup1Dim(RegressionEnvironment env, String rollup, AtomicInteger milestone) {

        String[] fields = "c0,c1".split(",");

        String epl = "@Name('s0')" +
            "select theString as c0, sum(intPrimitive) as c1 from SupportBean " +
            "group by " + rollup;
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 10}, {null, 10}});

        env.sendEventBean(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E2", 20}, {null, 30}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E1", 30));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 40}, {null, 60}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 40));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E2", 60}, {null, 100}});

        env.undeployAll();
    }

    private static void tryAssertionUnboundRollup3Dim(RegressionEnvironment env, String groupByClause, boolean isJoin, AtomicInteger milestone) {

        String[] fields = "c0,c1,c2,c3,c4".split(",");

        String epl = "@Name('s0')" +
            "select theString as c0, intPrimitive as c1, longPrimitive as c2, count(*) as c3, sum(doublePrimitive) as c4 " +
            "from SupportBean#keepall " + (isJoin ? ", SupportBean_S0#lastevent " : "") +
            "group by " + groupByClause;
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1));

        env.sendEventBean(makeEvent("E1", 1, 10, 100));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 1, 10L, 1L, 100d}, {"E1", 1, null, 1L, 100d}, {"E1", null, null, 1L, 100d}, {null, null, null, 1L, 100d}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 1, 11, 200));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 1, 11L, 1L, 200d}, {"E1", 1, null, 2L, 300d}, {"E1", null, null, 2L, 300d}, {null, null, null, 2L, 300d}});

        env.sendEventBean(makeEvent("E1", 2, 10, 300));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 2, 10L, 1L, 300d}, {"E1", 2, null, 1L, 300d}, {"E1", null, null, 3L, 600d}, {null, null, null, 3L, 600d}});

        env.sendEventBean(makeEvent("E2", 1, 10, 400));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E2", 1, 10L, 1L, 400d}, {"E2", 1, null, 1L, 400d}, {"E2", null, null, 1L, 400d}, {null, null, null, 4L, 1000d}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 1, 10, 500));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 1, 10L, 2L, 600d}, {"E1", 1, null, 3L, 800d}, {"E1", null, null, 4L, 1100d}, {null, null, null, 5L, 1500d}});

        env.milestoneInc(milestone);

        env.sendEventBean(makeEvent("E1", 1, 11, 600));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"E1", 1, 11L, 2L, 800d}, {"E1", 1, null, 4L, 1400d}, {"E1", null, null, 5L, 1700d}, {null, null, null, 6L, 2100d}});

        env.undeployAll();
    }

    private static SupportBean makeEvent(int intBoxed, String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setIntBoxed(intBoxed);
        return sb;
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        return sb;
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive, int intBoxed) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        sb.setIntBoxed(intBoxed);
        return sb;
    }

    private static void assertTypesC0C1C2(EPStatement stmtOne, Class expectedC0, Class expectedC1, Class expectedC2) {
        assertEquals(expectedC0, stmtOne.getEventType().getPropertyType("c0"));
        assertEquals(expectedC1, stmtOne.getEventType().getPropertyType("c1"));
        assertEquals(expectedC2, stmtOne.getEventType().getPropertyType("c2"));
    }
}
