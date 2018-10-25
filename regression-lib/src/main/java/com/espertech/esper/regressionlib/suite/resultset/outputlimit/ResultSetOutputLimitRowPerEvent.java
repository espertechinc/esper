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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertExecution;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertExecutionTestSelector;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertTestResult;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertTrue;

public class ResultSetOutputLimitRowPerEvent {
    private static final String EVENT_NAME = SupportMarketDataBean.class.getSimpleName();
    private final static String JOIN_KEY = "KEY";
    private final static String CATEGORY = "Aggregated and Un-grouped";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
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
        execs.add(new ResultSet11AllHavingNoJoinHinted());
        execs.add(new ResultSet12AllHavingJoin());
        execs.add(new ResultSet13LastNoHavingNoJoin());
        execs.add(new ResultSet14LastNoHavingJoin());
        execs.add(new ResultSet15LastHavingNoJoin());
        execs.add(new ResultSet16LastHavingJoin());
        execs.add(new ResultSet17FirstNoHavingNoJoinIStreamOnly());
        execs.add(new ResultSet17FirstNoHavingNoJoinIRStream());
        execs.add(new ResultSet18SnapshotNoHavingNoJoin());
        execs.add(new ResultSetHaving());
        execs.add(new ResultSetHavingJoin());
        execs.add(new ResultSetMaxTimeWindow());
        execs.add(new ResultSetLimitSnapshot());
        execs.add(new ResultSetLimitSnapshotJoin());
        execs.add(new ResultSetJoinSortWindow());
        execs.add(new ResultSetRowPerEventNoJoinLast());
        execs.add(new ResultSetRowPerEventJoinAll());
        execs.add(new ResultSetRowPerEventJoinLast());
        execs.add(new ResultSetTime());
        execs.add(new ResultSetCount());
        return execs;
    }

    private static class ResultSet1NoneNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec)";
            tryAssertion12(env, stmtText, "none");
        }
    }

    private static class ResultSet2NoneNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol";
            tryAssertion12(env, stmtText, "none");
        }
    }

    private static class ResultSet3NoneHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                " having sum(price) > 100";
            tryAssertion34(env, stmtText, "none");
        }
    }

    private static class ResultSet4NoneHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                " having sum(price) > 100";
            tryAssertion34(env, stmtText, "none");
        }
    }

    private static class ResultSet5DefaultNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output every 1 seconds";
            tryAssertion56(env, stmtText, "default");
        }
    }

    private static class ResultSet6DefaultNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output every 1 seconds";
            tryAssertion56(env, stmtText, "default");
        }
    }

    private static class ResultSet7DefaultHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) \n" +
                "having sum(price) > 100" +
                "output every 1 seconds";
            tryAssertion78(env, stmtText, "default");
        }
    }

    private static class ResultSet8DefaultHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having sum(price) > 100" +
                "output every 1 seconds";
            tryAssertion78(env, stmtText, "default");
        }
    }

    private static class ResultSet9AllNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion9AllNoHavingNoJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion9AllNoHavingNoJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec) " +
            "output all every 1 seconds";
        tryAssertion56(env, stmtText, "all");
    }

    private static class ResultSet10AllNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion10AllNoHavingJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion10AllNoHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "output all every 1 seconds";
        tryAssertion56(env, stmtText, "all");
    }

    private static class ResultSet11AllHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "having sum(price) > 100" +
                "output all every 1 seconds";
            tryAssertion78(env, stmtText, "all");
        }
    }

    private static class ResultSet11AllHavingNoJoinHinted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion11AllHavingNoJoinHinted(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion11AllHavingNoJoinHinted(RegressionEnvironment env, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec) " +
            "having sum(price) > 100" +
            "output all every 1 seconds";
        tryAssertion78(env, stmtText, "all");
    }

    private static class ResultSet12AllHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion12AllHavingJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion12AllHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "having sum(price) > 100" +
            "output all every 1 seconds";
        tryAssertion78(env, stmtText, "all");
    }

    private static class ResultSet13LastNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion13LastNoHavingNoJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion13LastNoHavingNoJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec)" +
            "output last every 1 seconds";
        tryAssertion13_14(env, stmtText, "last");
    }

    private static class ResultSet14LastNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion14LastNoHavingJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion14LastNoHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "output last every 1 seconds";
        tryAssertion13_14(env, stmtText, "last");
    }

    private static class ResultSet15LastHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion15LastHavingNoJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion15LastHavingNoJoin(RegressionEnvironment env, SupportOutputLimitOpt hint) {
        String stmtText = hint.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec)" +
            "having sum(price) > 100 " +
            "output last every 1 seconds";
        tryAssertion15_16(env, stmtText, "last");
    }

    private static class ResultSet16LastHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion16LastHavingJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion16LastHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, sum(price) " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "having sum(price) > 100 " +
            "output last every 1 seconds";
        tryAssertion15_16(env, stmtText, "last");
    }

    private static class ResultSet17FirstNoHavingNoJoinIStreamOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output first every 1 seconds";
            tryAssertion17IStreamOnly(env, stmtText, "first");
        }
    }

    private static class ResultSet17FirstNoHavingNoJoinIRStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output first every 1 seconds";
            tryAssertion17IRStream(env, stmtText, "first");
        }
    }

    private static class ResultSet18SnapshotNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, sum(price) " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output snapshot every 1 seconds";
            tryAssertion18(env, stmtText, "first");
        }
    }

    private static class ResultSetHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select symbol, avg(price) as avgPrice " +
                "from SupportMarketDataBean#time(3 sec) " +
                "having avg(price) > 10" +
                "output every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionHaving(env);
        }
    }

    private static class ResultSetHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select symbol, avg(price) as avgPrice " +
                "from SupportMarketDataBean#time(3 sec) as md, " +
                "SupportBean#keepall as s where s.theString = md.symbol " +
                "having avg(price) > 10" +
                "output every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("SYM1", -1));

            tryAssertionHaving(env);
        }
    }

    private static class ResultSetMaxTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select irstream volume, max(price) as maxVol" +
                " from SupportMarketDataBean#time(1 sec) " +
                "output every 1 seconds";
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

    private static class ResultSetLimitSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String selectStmt = "@name('s0') select symbol, sum(price) as sumprice from SupportMarketDataBean" +
                "#time(10 seconds) output snapshot every 1 seconds order by symbol asc";
            env.compileDeploy(selectStmt).addListener("s0");

            sendEvent(env, "ABC", 20);

            sendTimer(env, 500);
            sendEvent(env, "IBM", 16);
            sendEvent(env, "MSFT", 14);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 1000);
            String[] fields = new String[]{"symbol", "sumprice"};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"ABC", 50d}, {"IBM", 50d}, {"MSFT", 50d}});
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 1500);
            sendEvent(env, "YAH", 18);
            sendEvent(env, "s4", 30);

            sendTimer(env, 10000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"ABC", 98d}, {"IBM", 98d}, {"MSFT", 98d}, {"YAH", 98d}, {"s4", 98d}});
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"YAH", 48d}, {"s4", 48d}});
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 12000);
            TestCase.assertTrue(env.listener("s0").isInvoked());
            TestCase.assertNull(env.listener("s0").getLastNewData());
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 13000);
            TestCase.assertTrue(env.listener("s0").isInvoked());
            TestCase.assertNull(env.listener("s0").getLastNewData());
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetLimitSnapshotJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String selectStmt = "@name('s0') select irstream symbol, sum(price) as sumprice from SupportMarketDataBean" +
                "#time(10 seconds) as m, SupportBean" +
                "#keepall as s where s.theString = m.symbol output snapshot every 1 seconds order by symbol asc";
            env.compileDeploy(selectStmt).addListener("s0");

            env.sendEventBean(new SupportBean("ABC", 1));
            env.sendEventBean(new SupportBean("IBM", 2));
            env.sendEventBean(new SupportBean("MSFT", 3));
            env.sendEventBean(new SupportBean("YAH", 4));
            env.sendEventBean(new SupportBean("s4", 5));

            sendEvent(env, "ABC", 20);

            sendTimer(env, 500);
            sendEvent(env, "IBM", 16);
            sendEvent(env, "MSFT", 14);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 1000);
            String[] fields = new String[]{"symbol", "sumprice"};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"ABC", 50d}, {"IBM", 50d}, {"MSFT", 50d}});
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 1500);
            sendEvent(env, "YAH", 18);
            sendEvent(env, "s4", 30);

            sendTimer(env, 10000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"ABC", 98d}, {"IBM", 98d}, {"MSFT", 98d}, {"YAH", 98d}, {"s4", 98d}});
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 10500);
            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"YAH", 48d}, {"s4", 48d}});
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 11500);
            sendTimer(env, 12000);
            TestCase.assertTrue(env.listener("s0").isInvoked());
            TestCase.assertNull(env.listener("s0").getLastNewData());
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 13000);
            TestCase.assertTrue(env.listener("s0").isInvoked());
            TestCase.assertNull(env.listener("s0").getLastNewData());
            TestCase.assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetJoinSortWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select irstream volume, max(price) as maxVol" +
                " from SupportMarketDataBean#sort(1, volume desc) as s0," +
                "SupportBean#keepall as s1 " +
                "output every 1 seconds";
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

    private static class ResultSetRowPerEventNoJoinLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionRowPerEventNoJoinLast(env, outputLimitOpt);
            }
        }

        private static void tryAssertionRowPerEventNoJoinLast(RegressionEnvironment env, SupportOutputLimitOpt opt) {
            String epl = opt.getHint() + "@name('s0') select longBoxed, sum(longBoxed) as result " +
                "from SupportBean#length(3) " +
                "having sum(longBoxed) > 0 " +
                "output last every 2 events";

            createStmtAndListenerNoJoin(env, epl);
            tryAssertLastSum(env);

            epl = opt.getHint() + "@name('s0') select longBoxed, sum(longBoxed) as result " +
                "from SupportBean#length(3) " +
                "output last every 2 events";
            createStmtAndListenerNoJoin(env, epl);
            tryAssertLastSum(env);
        }
    }

    private static class ResultSetRowPerEventJoinAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionRowPerEventJoinAll(env, outputLimitOpt);
            }
        }

        private static void tryAssertionRowPerEventJoinAll(RegressionEnvironment env, SupportOutputLimitOpt opt) {
            String epl = opt.getHint() + "@name('s0') select longBoxed, sum(longBoxed) as result " +
                "from SupportBeanString#length(3) as one, " +
                "SupportBean#length(3) as two " +
                "having sum(longBoxed) > 0 " +
                "output all every 2 events";

            createStmtAndListenerJoin(env, epl);
            tryAssertAllSum(env);

            epl = opt.getHint() + "@name('s0') select longBoxed, sum(longBoxed) as result " +
                "from SupportBeanString#length(3) as one, " +
                "SupportBean#length(3) as two " +
                "output every 2 events";

            createStmtAndListenerJoin(env, epl);
            tryAssertAllSum(env);

            env.undeployAll();
        }
    }

    private static class ResultSetRowPerEventJoinLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select longBoxed, sum(longBoxed) as result " +
                "from SupportBeanString#length(3) as one, " +
                "SupportBean#length(3) as two " +
                "having sum(longBoxed) > 0 " +
                "output last every 2 events";

            createStmtAndListenerJoin(env, epl);
            tryAssertLastSum(env);

            epl = "@name('s0') select longBoxed, sum(longBoxed) as result " +
                "from SupportBeanString#length(3) as one, " +
                "SupportBean#length(3) as two " +
                "output last every 2 events";

            createStmtAndListenerJoin(env, epl);
            tryAssertLastSum(env);

            env.undeployAll();
        }
    }

    private static class ResultSetTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Set the clock to 0
            AtomicLong currentTime = new AtomicLong();
            sendTimeEventRelative(env, 0, currentTime);

            // Create the EPL statement and add a listener
            String epl = "@name('s0') select symbol, sum(volume) from " + EVENT_NAME + "#length(5) output first every 3 seconds";
            env.compileDeploy(epl).addListener("s0");
            env.listener("s0").reset();

            // Send the first event of the batch; should be output
            sendMarketDataEvent(env, 10L);
            assertEvent(env, 10L);

            // Send another event, not the first, for aggregation
            // update only, no output
            sendMarketDataEvent(env, 20L);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // Update time
            sendTimeEventRelative(env, 3000, currentTime);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // Send first event of the next batch, should be output.
            // The aggregate value is computed over all events
            // received: 10 + 20 + 30 = 60
            sendMarketDataEvent(env, 30L);
            assertEvent(env, 60L);

            // Send the next event of the batch, no output
            sendMarketDataEvent(env, 40L);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // Update time
            sendTimeEventRelative(env, 3000, currentTime);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // Send first event of third batch
            sendMarketDataEvent(env, 1L);
            assertEvent(env, 101L);

            // Update time
            sendTimeEventRelative(env, 3000, currentTime);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // Update time: no first event this batch, so a callback
            // is made at the end of the interval
            sendTimeEventRelative(env, 3000, currentTime);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Create the EPL statement and add a listener
            String statementText = "@name('s0') select symbol, sum(volume) from " + EVENT_NAME + "#length(5) output first every 3 events";
            env.compileDeploy(statementText).addListener("s0");
            env.listener("s0").reset();

            // Send the first event of the batch, should be output
            sendEventLong(env, 10L);
            assertEvent(env, 10L);

            // Send the second event of the batch, not output, used
            // for updating the aggregate value only
            sendEventLong(env, 20L);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // Send the third event of the batch, still not output,
            // but should reset the batch
            sendEventLong(env, 30L);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // First event, next batch, aggregate value should be
            // 10 + 20 + 30 + 40 = 100
            sendEventLong(env, 40L);
            assertEvent(env, 100L);

            // Next event again not output
            sendEventLong(env, 50L);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static void sendEventLong(RegressionEnvironment env, long volume) {
        env.sendEventBean(new SupportMarketDataBean("DELL", 0.0, volume, null));
    }

    private static void createStmtAndListenerNoJoin(RegressionEnvironment env, String epl) {
        env.compileDeploy(epl).addListener("s0");
    }

    private static void tryAssertAllSum(RegressionEnvironment env) {
        // send an event
        sendEvent(env, 1);

        // check no update
        TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

        // send another event
        sendEvent(env, 2);

        // check update, all events present
        TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(2, env.listener("s0").getLastNewData().length);
        Assert.assertEquals(1L, env.listener("s0").getLastNewData()[0].get("longBoxed"));
        Assert.assertEquals(1L, env.listener("s0").getLastNewData()[0].get("result"));
        Assert.assertEquals(2L, env.listener("s0").getLastNewData()[1].get("longBoxed"));
        Assert.assertEquals(3L, env.listener("s0").getLastNewData()[1].get("result"));
        TestCase.assertNull(env.listener("s0").getLastOldData());

        env.undeployAll();
    }

    private static void tryAssertion12(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 34d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 59d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 85d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 112d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 87d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 88d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 79d}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion34(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 112d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion13_14(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 85d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"YAH", 112d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 88d}}, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion15_16(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"YAH", 112d}});
        expected.addResultInsRem(6200, 0, null, null);
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion78(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsRem(1200, 0, null, null);
        expected.addResultInsRem(2200, 0, null, null);
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsRem(5200, 0, new Object[][]{{"IBM", 109d}, {"YAH", 112d}}, null);
        expected.addResultInsRem(6200, 0, null, null);
        expected.addResultInsRem(7200, 0, null, null);

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion56(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 25d}, {"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 58d}, {"YAH", 59d}, {"IBM", 85d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 109d}, {"YAH", 112d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 88d}}, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"MSFT", 79d}, {"IBM", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion17IStreamOnly(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 88d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private static void tryAssertion17IRStream(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 58d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 87d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 109d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 87d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 79d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private static void tryAssertion18(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "sum(price)"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 34d}, {"MSFT", 34d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 85d}, {"MSFT", 85d}, {"IBM", 85d}, {"YAH", 85d}, {"IBM", 85d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 85d}, {"MSFT", 85d}, {"IBM", 85d}, {"YAH", 85d}, {"IBM", 85d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 87d}, {"MSFT", 87d}, {"IBM", 87d}, {"YAH", 87d}, {"IBM", 87d}, {"YAH", 87d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 112d}, {"MSFT", 112d}, {"IBM", 112d}, {"YAH", 112d}, {"IBM", 112d}, {"YAH", 112d}, {"IBM", 112d}, {"YAH", 112d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"IBM", 88d}, {"YAH", 88d}, {"YAH", 88d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 54d}, {"YAH", 54d}, {"IBM", 54d}, {"YAH", 54d}, {"YAH", 54d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertionHaving(RegressionEnvironment env) {
        sendEvent(env, "SYM1", 10d);
        sendEvent(env, "SYM1", 11d);
        sendEvent(env, "SYM1", 9);

        sendTimer(env, 1000);
        String[] fields = "symbol,avgPrice".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"SYM1", 10.5});

        sendEvent(env, "SYM1", 13d);
        sendEvent(env, "SYM1", 10d);
        sendEvent(env, "SYM1", 9);
        sendTimer(env, 2000);

        Assert.assertEquals(3, env.listener("s0").getLastNewData().length);
        TestCase.assertNull(env.listener("s0").getLastOldData());
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"SYM1", 43 / 4.0}, {"SYM1", 53.0 / 5.0}, {"SYM1", 62 / 6.0}});

        env.undeployAll();
    }

    private static void tryAssertLastSum(RegressionEnvironment env) {
        // send an event
        sendEvent(env, 1);

        // check no update
        TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

        // send another event
        sendEvent(env, 2);

        // check update, all events present
        TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
        Assert.assertEquals(2L, env.listener("s0").getLastNewData()[0].get("longBoxed"));
        Assert.assertEquals(3L, env.listener("s0").getLastNewData()[0].get("result"));
        TestCase.assertNull(env.listener("s0").getLastOldData());

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed) {
        sendEvent(env, longBoxed, 0, (short) 0);
    }

    private static void sendMarketDataEvent(RegressionEnvironment env, long volume) {
        env.sendEventBean(new SupportMarketDataBean("SYM1", 0, volume, null));
    }

    private static void sendTimeEventRelative(RegressionEnvironment env, int timeIncrement, AtomicLong currentTime) {
        currentTime.addAndGet(timeIncrement);
        env.advanceTime(currentTime.get());
    }

    private static void createStmtAndListenerJoin(RegressionEnvironment env, String epl) {
        env.compileDeploy(epl).addListener("s0");
        env.sendEventBean(new SupportBeanString(JOIN_KEY));
    }

    private static void assertEvent(RegressionEnvironment env, long volume) {
        TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());
        assertTrue(env.listener("s0").getLastNewData() != null);
        Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
        Assert.assertEquals(volume, env.listener("s0").getLastNewData()[0].get("sum(volume)"));
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendTimer(RegressionEnvironment env, long time) {
        env.advanceTime(time);
    }
}
