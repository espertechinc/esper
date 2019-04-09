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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertExecution;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertTestResult;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ResultSetOutputLimitAggregateGrouped {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";
    private final static String CATEGORY = "Aggregated and Grouped";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetUnaggregatedOutputFirst());
        execs.add(new ResultSet1NoneNoHavingNoJoin());
        execs.add(new ResultSet2NoneNoHavingJoin());
        execs.add(new ResultSet3NoneHavingNoJoin());
        execs.add(new ResultSet4NoneHavingJoin());
        execs.add(new ResultSet5DefaultNoHavingNoJoin());
        execs.add(new ResultSet6DefaultNoHavingJoin());
        execs.add(new ResultSet7DefaultHavingNoJoin());
        execs.add(new ResultSet8DefaultHavingJoin());
        execs.add(new ResultSet9AllNoHavingNoJoin());
        execs.add(new ResultSet10AllNoHavingJoin());
        execs.add(new ResultSet11AllHavingNoJoin());
        execs.add(new ResultSet12AllHavingJoin());
        execs.add(new ResultSet13LastNoHavingNoJoin());
        execs.add(new ResultSet14LastNoHavingJoin());
        execs.add(new ResultSet15LastHavingNoJoin());
        execs.add(new ResultSet16LastHavingJoin());
        execs.add(new ResultSet17FirstNoHavingNoJoin());
        execs.add(new ResultSet17FirstNoHavingJoin());
        execs.add(new ResultSet18SnapshotNoHavingNoJoin());
        execs.add(new ResultSetHaving());
        execs.add(new ResultSetHavingJoin());
        execs.add(new ResultSetJoinSortWindow());
        execs.add(new ResultSetLimitSnapshot());
        execs.add(new ResultSetLimitSnapshotJoin());
        execs.add(new ResultSetMaxTimeWindow());
        execs.add(new ResultSetNoJoinLast());
        execs.add(new ResultSetNoOutputClauseView());
        execs.add(new ResultSetNoJoinDefault());
        execs.add(new ResultSetJoinDefault());
        execs.add(new ResultSetNoJoinAll());
        execs.add(new ResultSetJoinAll());
        execs.add(new ResultSetJoinLast());
        execs.add(new ResultSetOutputFirstHavingJoinNoJoin());
        execs.add(new ResultSetOutputAllMultikeyWArray());
        execs.add(new ResultSetOutputLastMultikeyWArray());
        return execs;
    }

    private static class ResultSetOutputLastMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = "theString,longPrimitive,intPrimitive,thesum".split(",");
            String epl = "@name('s0') select theString, longPrimitive, intPrimitive, sum(intPrimitive) as thesum from SupportBean#keepall " +
                "group by theString, longPrimitive output last every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            sendBeanEvent(env, "A", 0, 10);
            sendBeanEvent(env, "B", 1, 11);

            env.milestone(0);

            sendBeanEvent(env, "A", 0, 12);
            sendBeanEvent(env, "C", 0, 13);

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][] {
                {"B", 1L, 11, 11}, {"A", 0L, 12, 22}, {"C", 0L, 13, 13}});

            sendBeanEvent(env, "A", 0, 14);

            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][] {
                {"A", 0L, 14, 36}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputAllMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = "theString,longPrimitive,intPrimitive,thesum".split(",");
            String epl = "@name('s0') select theString, longPrimitive, intPrimitive, sum(intPrimitive) as thesum from SupportBean#keepall " +
                "group by theString, longPrimitive output all every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            sendBeanEvent(env, "A", 0, 10);
            sendBeanEvent(env, "B", 1, 11);

            env.milestone(0);

            sendBeanEvent(env, "A", 0, 12);
            sendBeanEvent(env, "C", 0, 13);

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][] {
                {"A", 0L, 10, 10}, {"B", 1L, 11, 11}, {"A", 0L, 12, 22}, {"C", 0L, 13, 13}});

            sendBeanEvent(env, "A", 0, 14);

            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][] {
                {"A", 0L, 14, 36}, {"B", 1L, 11, 11}, {"C", 0L, 13, 13}});

            env.undeployAll();
        }
    }

    private static class ResultSetUnaggregatedOutputFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String[] fields = "theString,intPrimitive".split(",");
            String epl = "@name('s0') select * from SupportBean\n" +
                "     group by theString\n" +
                "     output first every 10 seconds";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 3});

            env.milestone(1);

            sendTimer(env, 5000);

            env.milestone(2);

            env.sendEventBean(new SupportBean("E3", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 4});

            env.sendEventBean(new SupportBean("E2", 5));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 10000);

            env.milestone(3);

            env.sendEventBean(new SupportBean("E3", 6));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E1", 7));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 7});

            env.sendEventBean(new SupportBean("E1", 8));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            env.sendEventBean(new SupportBean("E2", 9));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 9});

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 11));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetOutputFirstHavingJoinNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String stmtText = "@name('s0') select theString, longPrimitive, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events";
            tryOutputFirstHaving(env, stmtText, milestone);

            String stmtTextJoin = "@name('s0') select theString, longPrimitive, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events";
            tryOutputFirstHaving(env, stmtTextJoin, milestone);

            String stmtTextOrder = "@name('s0') select theString, longPrimitive, sum(intPrimitive) as value from MyWindow group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
            tryOutputFirstHaving(env, stmtTextOrder, milestone);

            String stmtTextOrderJoin = "@name('s0') select theString, longPrimitive, sum(intPrimitive) as value from MyWindow mv, SupportBean_A#keepall a where a.id = mv.theString " +
                "group by theString having sum(intPrimitive) > 20 output first every 2 events order by theString asc";
            tryOutputFirstHaving(env, stmtTextOrderJoin, milestone);
        }
    }

    private static class ResultSet1NoneNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "group by symbol";
            tryAssertion12(env, stmtText, "none");
        }
    }

    private static class ResultSet2NoneNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol";
            tryAssertion12(env, stmtText, "none");
        }
    }

    private static class ResultSet3NoneHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "group by symbol " +
                " having sum(price) > 50";
            tryAssertion34(env, stmtText, "none");
        }
    }

    private static class ResultSet4NoneHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50";
            tryAssertion34(env, stmtText, "none");
        }
    }

    private static class ResultSet5DefaultNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "group by symbol " +
                "output every 1 seconds";
            tryAssertion56(env, stmtText, "default");
        }
    }

    private static class ResultSet6DefaultNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output every 1 seconds";
            tryAssertion56(env, stmtText, "default");
        }
    }

    private static class ResultSet7DefaultHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) \n" +
                "group by symbol " +
                "having sum(price) > 50" +
                "output every 1 seconds";
            tryAssertion78(env, stmtText, "default");
        }
    }

    private static class ResultSet8DefaultHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "having sum(price) > 50" +
                "output every 1 seconds";
            tryAssertion78(env, stmtText, "default");
        }
    }

    private static class ResultSet9AllNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "group by symbol " +
                "output all every 1 seconds " +
                "order by symbol";
            tryAssertion9_10(env, stmtText, "all");
        }
    }

    private static class ResultSet10AllNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output all every 1 seconds " +
                "order by symbol";
            tryAssertion9_10(env, stmtText, "all");
        }
    }

    private static class ResultSet11AllHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion11AllHavingNoJoin(env, outputLimitOpt);
            }
        }
    }

    private static class ResultSet12AllHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion12AllHavingJoin(env, outputLimitOpt);
            }
        }
    }

    private static class ResultSet13LastNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "group by symbol " +
                "output last every 1 seconds " +
                "order by symbol";
            tryAssertion13_14(env, stmtText, "last");
        }
    }

    private static class ResultSet14LastNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output last every 1 seconds " +
                "order by symbol";
            tryAssertion13_14(env, stmtText, "last");
        }
    }

    private static class ResultSet15LastHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion15LastHavingNoJoin(env, outputLimitOpt);
            }
        }
    }

    private static class ResultSet16LastHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion16LastHavingJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion16LastHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "group by symbol " +
            "having sum(price) > 50 " +
            "output last every 1 seconds";
        tryAssertion15_16(env, stmtText, "last");
    }

    private static class ResultSet17FirstNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "group by symbol " +
                "output first every 1 seconds";
            tryAssertion17(env, stmtText, "first");
        }
    }

    private static class ResultSet17FirstNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "group by symbol " +
                "output first every 1 seconds";
            tryAssertion17(env, stmtText, "first");
        }
    }

    private static class ResultSet18SnapshotNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "group by symbol " +
                "output snapshot every 1 seconds";
            tryAssertion18(env, stmtText, "snapshot");
        }
    }

    private static class ResultSetHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select irstream symbol, volume, sum(price) as sumprice" +
                " from SupportMarketDataBean#time(10 sec) " +
                "group by symbol " +
                "having sum(price) >= 10 " +
                "output every 3 events";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionHavingDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select irstream symbol, volume, sum(price) as sumprice" +
                " from SupportMarketDataBean#time(10 sec) as s0," +
                "SupportBean#keepall as s1 " +
                "where s0.symbol = s1.theString " +
                "group by symbol " +
                "having sum(price) >= 10 " +
                "output every 3 events";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("IBM", 0));

            tryAssertionHavingDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetJoinSortWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select irstream symbol, volume, max(price) as maxVol" +
                " from SupportMarketDataBean#sort(1, volume) as s0," +
                "SupportBean#keepall as s1 where s1.theString = s0.symbol " +
                "group by symbol output every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("JOIN_KEY", -1));

            sendEvent(env, "JOIN_KEY", 1d);
            sendEvent(env, "JOIN_KEY", 2d);
            env.listener("s0").reset();

            // moves all events out of the window,
            sendTimer(env, 1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
            UniformPair<EventBean[]> result = env.listener("s0").getDataListsFlattened();
            Assert.assertEquals(2, result.getFirst().length);
            Assert.assertEquals(1.0, result.getFirst()[0].get("maxVol"));
            Assert.assertEquals(2.0, result.getFirst()[1].get("maxVol"));
            Assert.assertEquals(1, result.getSecond().length);
            Assert.assertEquals(2.0, result.getSecond()[0].get("maxVol"));

            env.undeployAll();
        }
    }

    private static class ResultSetLimitSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String selectStmt = "@name('s0') select symbol, volume, sum(price) as sumprice from SupportMarketDataBean" +
                "#time(10 seconds) group by symbol output snapshot every 1 seconds";
            env.compileDeploy(selectStmt).addListener("s0");

            sendEvent(env, "s0", 1, 20);

            sendTimer(env, 500);
            sendEvent(env, "IBM", 2, 16);
            sendEvent(env, "s0", 3, 14);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 1000);
            String[] fields = new String[]{"symbol", "volume", "sumprice"};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"s0", 1L, 34d}, {"IBM", 2L, 16d}, {"s0", 3L, 34d}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 1500);
            sendEvent(env, "MSFT", 4, 18);
            sendEvent(env, "IBM", 5, 30);

            sendTimer(env, 10000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{{"s0", 1L, 34d}, {"IBM", 2L, 46d}, {"s0", 3L, 34d}, {"MSFT", 4L, 18d}, {"IBM", 5L, 46d}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"MSFT", 4L, 18d}, {"IBM", 5L, 30d}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 12000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 13000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetLimitSnapshotJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String selectStmt = "@name('s0') select symbol, volume, sum(price) as sumprice from SupportMarketDataBean" +
                "#time(10 seconds) as m, SupportBean" +
                "#keepall as s where s.theString = m.symbol group by symbol output snapshot every 1 seconds order by symbol, volume asc";
            env.compileDeploy(selectStmt).addListener("s0");

            env.sendEventBean(new SupportBean("ABC", 1));
            env.sendEventBean(new SupportBean("IBM", 2));
            env.sendEventBean(new SupportBean("MSFT", 3));

            sendEvent(env, "ABC", 1, 20);

            sendTimer(env, 500);
            sendEvent(env, "IBM", 2, 16);
            sendEvent(env, "ABC", 3, 14);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 1000);
            String[] fields = new String[]{"symbol", "volume", "sumprice"};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"ABC", 1L, 34d}, {"ABC", 3L, 34d}, {"IBM", 2L, 16d}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 1500);
            sendEvent(env, "MSFT", 4, 18);
            sendEvent(env, "IBM", 5, 30);

            sendTimer(env, 10000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
                new Object[][]{{"ABC", 1L, 34d}, {"ABC", 3L, 34d}, {"IBM", 2L, 46d}, {"IBM", 5L, 46d}, {"MSFT", 4L, 18d}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 10500);
            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"IBM", 5L, 30d}, {"MSFT", 4L, 18d}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 11500);
            sendTimer(env, 12000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 13000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetMaxTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select irstream symbol, " +
                "volume, max(price) as maxVol" +
                " from SupportMarketDataBean#time(1 sec) " +
                "group by symbol output every 1 seconds";
            env.compileDeploy(epl).addListener("s0");


            sendEvent(env, "SYM1", 1d);
            sendEvent(env, "SYM1", 2d);
            env.listener("s0").reset();

            // moves all events out of the window,
            sendTimer(env, 1000);        // newdata is 2 eventa, old data is the same 2 events, therefore the sum is null
            UniformPair<EventBean[]> result = env.listener("s0").getDataListsFlattened();
            Assert.assertEquals(2, result.getFirst().length);
            Assert.assertEquals(1.0, result.getFirst()[0].get("maxVol"));
            Assert.assertEquals(2.0, result.getFirst()[1].get("maxVol"));
            Assert.assertEquals(2, result.getSecond().length);
            Assert.assertEquals(null, result.getSecond()[0].get("maxVol"));
            Assert.assertEquals(null, result.getSecond()[1].get("maxVol"));

            env.undeployAll();
        }
    }

    private static class ResultSetNoJoinLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionNoJoinLast(env, outputLimitOpt);
            }
        }
    }

    private static class ResultSetNoOutputClauseView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum " +
                "from SupportMarketDataBean#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol ";

            env.compileDeploy(epl).addListener("s0");

            tryAssertionSingle(env);

            env.undeployAll();
        }
    }

    private static class ResultSetNoJoinDefault implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum " +
                "from SupportMarketDataBean#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output every 2 events";

            env.compileDeploy(epl).addListener("s0");

            tryAssertionDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetJoinDefault implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "output every 2 events";

            env.compileDeploy(epl).addListener("s0");


            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

            tryAssertionDefault(env);

            env.undeployAll();
        }
    }

    private static class ResultSetNoJoinAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionNoJoinAll(env, outputLimitOpt);
            }
        }

        private static void tryAssertionNoJoinAll(RegressionEnvironment env, SupportOutputLimitOpt opt) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = opt.getHint() + "@name('s0') select symbol, volume, sum(price) as mySum " +
                "from SupportMarketDataBean#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "output all every 2 events";

            env.compileDeploy(epl).addListener("s0");


            tryAssertionAll(env);

            env.undeployAll();
        }
    }

    private static class ResultSetJoinAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionJoinAll(env, outputLimitOpt);
            }
        }

        private static void tryAssertionJoinAll(RegressionEnvironment env, SupportOutputLimitOpt opt) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = opt.getHint() + "@name('s0') select symbol, volume, sum(price) as mySum " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "output all every 2 events";

            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

            tryAssertionAll(env);

            env.undeployAll();
        }
    }

    private static class ResultSetJoinLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionJoinLast(env, outputLimitOpt);
            }
        }
    }

    private static void tryAssertionJoinLast(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = opt.getHint() +
            "@name('s0') select symbol, volume, sum(price) as mySum " +
            "from SupportBeanString#length(100) as one, " +
            "SupportMarketDataBean#length(5) as two " +
            "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
            "  and one.theString = two.symbol " +
            "group by symbol " +
            "output last every 2 events";

        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
        env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

        tryAssertionLast(env);

        env.undeployAll();
    }

    private static void tryAssertionHavingDefault(RegressionEnvironment env) {
        sendEvent(env, "IBM", 1, 5);
        sendEvent(env, "IBM", 2, 6);
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, "IBM", 3, -3);
        String[] fields = "symbol,volume,sumprice".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"IBM", 2L, 11.0});

        sendTimer(env, 5000);
        sendEvent(env, "IBM", 4, 10);
        sendEvent(env, "IBM", 5, 0);
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, "IBM", 6, 1);
        Assert.assertEquals(3, env.listener("s0").getLastNewData().length);
        EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"IBM", 4L, 18.0});
        EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[1], fields, new Object[]{"IBM", 5L, 18.0});
        EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[2], fields, new Object[]{"IBM", 6L, 19.0});
        env.listener("s0").reset();

        sendTimer(env, 11000);
        Assert.assertEquals(3, env.listener("s0").getLastOldData().length);
        EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"IBM", 1L, 11.0});
        EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[1], fields, new Object[]{"IBM", 2L, 11.0});
        env.listener("s0").reset();
    }

    private static void tryAssertionDefault(RegressionEnvironment env) {
        // assert select result type
        EventType eventType = env.statement("s0").getEventType();
        Assert.assertEquals(String.class, eventType.getPropertyType("symbol"));
        Assert.assertEquals(Long.class, eventType.getPropertyType("volume"));
        Assert.assertEquals(Double.class, eventType.getPropertyType("mySum"));

        sendEvent(env, SYMBOL_IBM, 500, 20);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, SYMBOL_DELL, 10000, 51);
        String[] fields = "symbol,volume,mySum".split(",");
        UniformPair<EventBean[]> events = env.listener("s0").getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 10000L, 51.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 10000L, 51.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(env.listener("s0").getLastOldData());

        env.listener("s0").reset();

        sendEvent(env, SYMBOL_DELL, 20000, 52);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, SYMBOL_DELL, 40000, 45);
        events = env.listener("s0").getDataListsFlattened();
        EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
            new Object[][]{{SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}});
        assertNull(env.listener("s0").getLastOldData());
    }

    private static void tryAssertionAll(RegressionEnvironment env) {
        // assert select result type
        EventType eventType = env.statement("s0").getEventType();
        Assert.assertEquals(String.class, eventType.getPropertyType("symbol"));
        Assert.assertEquals(Long.class, eventType.getPropertyType("volume"));
        Assert.assertEquals(Double.class, eventType.getPropertyType("mySum"));

        sendEvent(env, SYMBOL_IBM, 500, 20);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, SYMBOL_DELL, 10000, 51);
        String[] fields = "symbol,volume,mySum".split(",");
        UniformPair<EventBean[]> events = env.listener("s0").getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 10000L, 51.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 10000L, 51.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(env.listener("s0").getLastOldData());
        env.listener("s0").reset();

        sendEvent(env, SYMBOL_DELL, 20000, 52);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, SYMBOL_DELL, 40000, 45);
        events = env.listener("s0").getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_IBM)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_IBM, 500L, 20.0}, {SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 20000L, 51.0 + 52.0}, {SYMBOL_DELL, 40000L, 51.0 + 52.0 + 45.0}, {SYMBOL_IBM, 500L, 20.0}});
        }
        assertNull(env.listener("s0").getLastOldData());
    }

    private static void tryAssertionLast(RegressionEnvironment env) {
        String[] fields = "symbol,volume,mySum".split(",");
        sendEvent(env, SYMBOL_DELL, 10000, 51);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, SYMBOL_DELL, 20000, 52);
        UniformPair<EventBean[]> events = env.listener("s0").getDataListsFlattened();
        EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
            new Object[][]{{SYMBOL_DELL, 20000L, 103.0}});
        assertNull(env.listener("s0").getLastOldData());
        env.listener("s0").reset();

        sendEvent(env, SYMBOL_DELL, 30000, 70);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, SYMBOL_IBM, 10000, 20);
        events = env.listener("s0").getDataListsFlattened();
        if (events.getFirst()[0].get("symbol").equals(SYMBOL_DELL)) {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_DELL, 30000L, 173.0}, {SYMBOL_IBM, 10000L, 20.0}});
        } else {
            EPAssertionUtil.assertPropsPerRow(events.getFirst(), fields,
                new Object[][]{{SYMBOL_IBM, 10000L, 20.0}, {SYMBOL_DELL, 30000L, 173.0}});
        }
        assertNull(env.listener("s0").getLastOldData());
    }

    private static void tryOutputFirstHaving(RegressionEnvironment env, String statementText, AtomicInteger milestone) {
        String[] fields = "theString,longPrimitive,value".split(",");
        String[] fieldsLimited = "theString,value".split(",");
        String epl = "create window MyWindow#keepall as SupportBean;\n" +
            "insert into MyWindow select * from SupportBean;\n" +
            "on SupportMarketDataBean md delete from MyWindow mw where mw.intPrimitive = md.price;\n" +
            statementText;
        EPCompiled compiled = env.compile(epl);
        env.deploy(compiled).addListener("s0");

        env.sendEventBean(new SupportBean_A("E1"));
        env.sendEventBean(new SupportBean_A("E2"));

        env.milestoneInc(milestone);

        sendBeanEvent(env, "E1", 101, 10);
        sendBeanEvent(env, "E2", 102, 15);
        sendBeanEvent(env, "E1", 103, 10);
        sendBeanEvent(env, "E2", 104, 5);
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        sendBeanEvent(env, "E2", 105, 5);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 105L, 25});

        sendBeanEvent(env, "E2", 106, -6);    // to 19, does not count toward condition
        sendBeanEvent(env, "E2", 107, 2);    // to 21, counts toward condition
        assertFalse(env.listener("s0").isInvoked());
        sendBeanEvent(env, "E2", 108, 1);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 108L, 22});

        env.milestoneInc(milestone);

        sendBeanEvent(env, "E2", 109, 1);    // to 23, counts toward condition
        assertFalse(env.listener("s0").isInvoked());
        sendBeanEvent(env, "E2", 110, 1);     // to 24
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 110L, 24});

        sendBeanEvent(env, "E2", 111, -10);    // to 14
        sendBeanEvent(env, "E2", 112, 10);    // to 24, counts toward condition
        assertFalse(env.listener("s0").isInvoked());
        sendBeanEvent(env, "E2", 113, 0);    // to 24, counts toward condition
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 113L, 24});

        env.milestoneInc(milestone);

        sendBeanEvent(env, "E2", 114, -10);    // to 14
        sendBeanEvent(env, "E2", 115, 1);     // to 15
        sendBeanEvent(env, "E2", 116, 5);     // to 20
        sendBeanEvent(env, "E2", 117, 0);     // to 20
        sendBeanEvent(env, "E2", 118, 1);     // to 21    // counts
        assertFalse(env.listener("s0").isInvoked());

        sendBeanEvent(env, "E2", 119, 0);    // to 21
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 119L, 21});

        // remove events
        sendMDEvent(env, "E2", 0);   // remove 113, 117, 119 (any order of delete!)
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 21});

        env.milestoneInc(milestone);

        // remove events
        sendMDEvent(env, "E2", -10); // remove 111, 114
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 41});

        env.milestoneInc(milestone);

        // remove events
        sendMDEvent(env, "E2", -6);  // since there is 3*0 we output the next one
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLimited, new Object[]{"E2", 47});

        sendMDEvent(env, "E2", 2);
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static void tryAssertion12(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 49d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 10000L, 1d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 11500L, 6d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 7d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 5000L, null}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion34(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 72d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion13_14(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}, {"YAH", 10000L, 1d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 7d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion15_16(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion78(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion56(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 49d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 7d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"MSFT", 5000L, null}, {"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion9_10(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 49d}, {"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 10000L, 1d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 10000L, 1d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 155L, 75d}, {"MSFT", 5000L, 9d}, {"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}, {"MSFT", 5000L, 9d}, {"YAH", 11500L, 6d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 150L, 72d}, {"MSFT", 5000L, 9d}, {"YAH", 10500L, 7d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, new Object[][]{{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10500L, 6d}}, new Object[][]{{"IBM", 150L, 48d}, {"MSFT", 5000L, null}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion11_12(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 155L, 75d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"IBM", 150L, 72d}}, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion17(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 49d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 10000L, 1d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 3d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 97d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 11500L, 6d}});
        expected.addResultInsert(5700, 0, new Object[][]{{"IBM", 100L, 72d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 7d}});
        expected.addResultInsert(6300, 0, new Object[][]{{"MSFT", 5000L, null}});
        expected.addResultInsert(7000, 0, new Object[][]{{"IBM", 150L, 48d}, {"YAH", 10000L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion18(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 75d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 100L, 75d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 75d}, {"YAH", 10000L, 3d}, {"IBM", 155L, 75d}, {"YAH", 11000L, 3d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 100L, 97d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 97d}, {"YAH", 10000L, 6d}, {"IBM", 155L, 97d}, {"YAH", 11000L, 6d}, {"IBM", 150L, 97d}, {"YAH", 11500L, 6d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 5000L, 9d}, {"IBM", 150L, 72d}, {"YAH", 10000L, 7d}, {"IBM", 155L, 72d}, {"YAH", 11000L, 7d}, {"IBM", 150L, 72d}, {"YAH", 11500L, 7d}, {"YAH", 10500L, 7d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 155L, 48d}, {"YAH", 11000L, 6d}, {"IBM", 150L, 48d}, {"YAH", 11500L, 6d}, {"YAH", 10500L, 6d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void runAssertion15LastHavingNoJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec)" +
            "group by symbol " +
            "having sum(price) > 50 " +
            "output last every 1 seconds";
        tryAssertion15_16(env, stmtText, "last");
    }

    private static void runAssertion12AllHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "group by symbol " +
            "having sum(price) > 50 " +
            "output all every 1 seconds";
        tryAssertion11_12(env, stmtText, "all");
    }

    private static void runAssertion6LastHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "group by symbol " +
            "having sum(price) > 50 " +
            "output last every 1 seconds";
        tryAssertion15_16(env, stmtText, "last");
    }

    private static void runAssertion11AllHavingNoJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec) " +
            "group by symbol " +
            "having sum(price) > 50 " +
            "output all every 1 seconds";
        tryAssertion11_12(env, stmtText, "all");
    }

    private static void tryAssertionNoJoinLast(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = opt.getHint() +
            "@name('s0') select symbol, volume, sum(price) as mySum " +
            "from SupportMarketDataBean#length(5) " +
            "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
            "group by symbol " +
            "output last every 2 events";

        env.compileDeploy(epl).addListener("s0");

        tryAssertionLast(env);

        env.undeployAll();
    }

    private static void assertEvent(RegressionEnvironment env, String symbol, Double mySum, Long volume) {
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertEquals(1, newData.length);

        Assert.assertEquals(symbol, newData[0].get("symbol"));
        Assert.assertEquals(mySum, newData[0].get("mySum"));
        Assert.assertEquals(volume, newData[0].get("volume"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void tryAssertionSingle(RegressionEnvironment env) {
        // assert select result type
        Assert.assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        Assert.assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("mySum"));
        Assert.assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("volume"));

        sendEvent(env, SYMBOL_DELL, 10, 100);
        assertTrue(env.listener("s0").isInvoked());
        assertEvent(env, SYMBOL_DELL, 100d, 10L);

        sendEvent(env, SYMBOL_IBM, 15, 50);
        assertEvent(env, SYMBOL_IBM, 50d, 15L);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendBeanEvent(RegressionEnvironment env, String theString, long longPrimitive, int intPrimitive) {
        SupportBean b = new SupportBean();
        b.setTheString(theString);
        b.setLongPrimitive(longPrimitive);
        b.setIntPrimitive(intPrimitive);
        env.sendEventBean(b);
    }

    private static void sendMDEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }
}
