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
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertExecution;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertExecutionTestSelector;
import com.espertech.esper.regressionlib.support.patternassert.ResultAssertTestResult;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class ResultSetOutputLimitSimple {
    private final static String JOIN_KEY = "KEY";
    private final static String CATEGORY = "Un-aggregated and Un-grouped";

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
        execs.add(new ResultSet12AllHavingJoin());
        execs.add(new ResultSet13LastNoHavingNoJoin());
        execs.add(new ResultSet14LastNoHavingJoin());
        execs.add(new ResultSet15LastHavingNoJoin());
        execs.add(new ResultSet16LastHavingJoin());
        execs.add(new ResultSet17FirstNoHavingNoJoinIStream());
        execs.add(new ResultSet17FirstNoHavingJoinIStream());
        execs.add(new ResultSet17FirstNoHavingNoJoinIRStream());
        execs.add(new ResultSet17FirstNoHavingJoinIRStream());
        execs.add(new ResultSet18SnapshotNoHavingNoJoin());
        execs.add(new ResultSetOutputEveryTimePeriod());
        execs.add(new ResultSetOutputEveryTimePeriodVariable());
        execs.add(new ResultSetAggAllHaving());
        execs.add(new ResultSetAggAllHavingJoin());
        execs.add(new ResultSetIterator());
        execs.add(new ResultSetLimitEventJoin());
        execs.add(new ResultSetLimitTime());
        execs.add(new ResultSetTimeBatchOutputEvents());
        execs.add(new ResultSetSimpleNoJoinAll());
        execs.add(new ResultSetSimpleNoJoinLast());
        execs.add(new ResultSetSimpleJoinAll());
        execs.add(new ResultSetSimpleJoinLast());
        execs.add(new ResultSetLimitEventSimple());
        execs.add(new ResultSetLimitSnapshot());
        execs.add(new ResultSetFirstSimpleHavingAndNoHaving());
        execs.add(new ResultSetLimitSnapshotJoin());
        execs.add(new ResultSetSnapshotMonthScoped());
        execs.add(new ResultSetFirstMonthScoped());
        execs.add(new ResultSetOutputFirstUnidirectionalJoinNamedWindow());
        return execs;
    }

    private static class ResultSet1NoneNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec)";
            tryAssertion12(env, stmtText, "none");
        }
    }

    private static class ResultSet2NoneNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol";
            tryAssertion12(env, stmtText, "none");
        }
    }

    private static class ResultSet3NoneHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                " having price > 10";
            tryAssertion34(env, stmtText, "none");
        }
    }

    private static class ResultSet4NoneHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                " having price > 10";
            tryAssertion34(env, stmtText, "none");
        }
    }

    private static class ResultSet5DefaultNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output every 1 seconds";
            tryAssertion56(env, stmtText, "default");
        }
    }

    private static class ResultSet6DefaultNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output every 1 seconds";
            tryAssertion56(env, stmtText, "default");
        }
    }

    private static class ResultSet7DefaultHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec) \n" +
                "having price > 10" +
                "output every 1 seconds";
            tryAssertion78(env, stmtText, "default");
        }
    }

    private static class ResultSet8DefaultHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having price > 10" +
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
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, price " +
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

    private static void runAssertion10AllNoHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, price " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "output all every 1 seconds";
        tryAssertion56(env, stmtText, "all");
    }

    private static class ResultSet11AllHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertion11AllHavingNoJoin(env, outputLimitOpt);
            }
        }
    }

    private static void runAssertion11AllHavingNoJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, price " +
            "from SupportMarketDataBean#time(5.5 sec) " +
            "having price > 10" +
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

    private static void runAssertion12AllHavingJoin(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String stmtText = opt.getHint() + "@name('s0') select symbol, volume, price " +
            "from SupportMarketDataBean#time(5.5 sec), " +
            "SupportBean#keepall where theString=symbol " +
            "having price > 10" +
            "output all every 1 seconds";
        tryAssertion78(env, stmtText, "all");
    }

    private static class ResultSet13LastNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "output last every 1 seconds";
            tryAssertion13_14(env, stmtText, "last");
        }
    }

    private static class ResultSet14LastNoHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output last every 1 seconds";
            tryAssertion13_14(env, stmtText, "last");
        }
    }

    private static class ResultSet15LastHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec)" +
                "having price > 10 " +
                "output last every 1 seconds";
            tryAssertion15_16(env, stmtText, "last");
        }
    }

    private static class ResultSet16LastHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "having price > 10 " +
                "output last every 1 seconds";
            tryAssertion15_16(env, stmtText, "last");
        }
    }

    private static class ResultSet17FirstNoHavingNoJoinIStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output first every 1 seconds";
            tryAssertion17IStream(env, stmtText, "first");
        }
    }

    private static class ResultSet17FirstNoHavingJoinIStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec)," +
                "SupportBean#keepall where theString=symbol " +
                "output first every 1 seconds";
            tryAssertion17IStream(env, stmtText, "first");
        }
    }

    private static class ResultSet17FirstNoHavingNoJoinIRStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output first every 1 seconds";
            tryAssertion17IRStream(env, stmtText, "first");
        }
    }

    private static class ResultSet17FirstNoHavingJoinIRStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec), " +
                "SupportBean#keepall where theString=symbol " +
                "output first every 1 seconds";
            tryAssertion17IRStream(env, stmtText, "first");
        }
    }

    private static class ResultSet18SnapshotNoHavingNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, price " +
                "from SupportMarketDataBean#time(5.5 sec) " +
                "output snapshot every 1 seconds";
            tryAssertion18(env, stmtText, "first");
        }
    }

    private static class ResultSetOutputFirstUnidirectionalJoinNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String[] fields = "c0,c1".split(",");
            String epl =
                "create window MyWindow#keepall as SupportBean_S0;\n" +
                    "insert into MyWindow select * from SupportBean_S0;\n" +
                    "@name('s0') select myWindow.id as c0, s1.id as c1\n" +
                    "from SupportBean_S1 as s1 unidirectional, MyWindow as myWindow\n" +
                    "where myWindow.p00 = s1.p10\n" +
                    "output first every 1 minutes;";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(10, "a"));
            env.sendEventBean(new SupportBean_S0(20, "b"));
            env.sendEventBean(new SupportBean_S1(1000, "b"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 1000});

            env.sendEventBean(new SupportBean_S1(1001, "b"));
            env.sendEventBean(new SupportBean_S1(1002, "a"));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(60 * 1000);
            env.sendEventBean(new SupportBean_S1(1003, "a"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 1003});

            env.sendEventBean(new SupportBean_S1(1004, "a"));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(120 * 1000);
            env.sendEventBean(new SupportBean_S1(1005, "a"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 1005});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputEveryTimePeriod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(2000);

            String stmtText = "@name('s0') select symbol from SupportMarketDataBean#keepall output snapshot every 1 day 2 hours 3 minutes 4 seconds 5 milliseconds";
            env.compileDeploy(stmtText).addListener("s0");

            sendMDEvent(env, "E1", 0);

            long deltaSec = 26 * 60 * 60 + 3 * 60 + 4;
            long deltaMSec = deltaSec * 1000 + 5 + 2000;
            env.advanceTime(deltaMSec - 1);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(deltaMSec);
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("symbol"));

            env.undeployAll();
        }
    }

    private static class ResultSetOutputEveryTimePeriodVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(2000);

            String stmtText = "@name('s0') select symbol from SupportMarketDataBean#keepall output snapshot every D days H hours M minutes S seconds MS milliseconds";
            env.compileDeploy(stmtText).addListener("s0");

            sendMDEvent(env, "E1", 0);

            long deltaSec = 26 * 60 * 60 + 3 * 60 + 4;
            long deltaMSec = deltaSec * 1000 + 5 + 2000;
            env.advanceTime(deltaMSec - 1);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(deltaMSec);
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("symbol"));

            // test statement model
            EPStatementObjectModel model = env.eplToModel(stmtText);
            Assert.assertEquals(stmtText, model.toEPL());

            env.undeployAll();
        }
    }

    private static class ResultSetAggAllHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume " +
                "from SupportMarketDataBean#length(10) as two " +
                "having volume > 0 " +
                "output every 5 events";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = new String[]{"symbol", "volume"};

            sendMDEvent(env, "S0", 20);
            sendMDEvent(env, "IBM", -1);
            sendMDEvent(env, "MSFT", -2);
            sendMDEvent(env, "YAH", 10);
            assertFalse(env.listener("s0").isInvoked());

            sendMDEvent(env, "IBM", 0);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"S0", 20L}, {"YAH", 10L}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggAllHavingJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume " +
                "from SupportMarketDataBean#length(10) as one," +
                "SupportBean#length(10) as two " +
                "where one.symbol=two.theString " +
                "having volume > 0 " +
                "output every 5 events";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = new String[]{"symbol", "volume"};
            env.sendEventBean(new SupportBean("S0", 0));
            env.sendEventBean(new SupportBean("IBM", 0));
            env.sendEventBean(new SupportBean("MSFT", 0));
            env.sendEventBean(new SupportBean("YAH", 0));

            sendMDEvent(env, "S0", 20);
            sendMDEvent(env, "IBM", -1);
            sendMDEvent(env, "MSFT", -2);
            sendMDEvent(env, "YAH", 10);
            assertFalse(env.listener("s0").isInvoked());

            sendMDEvent(env, "IBM", 0);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"S0", 20L}, {"YAH", 10L}});

            env.undeployAll();
        }
    }

    private static class ResultSetIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "price"};
            String epl = "@name('s0') select symbol, theString, price from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 3 events";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString("CAT"));
            env.sendEventBean(new SupportBeanString("IBM"));

            // Output limit clause ignored when iterating, for both joins and no-join
            sendEvent(env, "CAT", 50);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"CAT", 50d}});

            sendEvent(env, "CAT", 60);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}});

            sendEvent(env, "IBM", 70);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}, {"IBM", 70d}});

            sendEvent(env, "IBM", 90);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"CAT", 50d}, {"CAT", 60d}, {"IBM", 70d}, {"IBM", 90d}});

            env.undeployAll();
        }
    }

    private static class ResultSetLimitEventJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement =
                "select * from SupportBean#length(5) as event1," +
                    "SupportBean_A#length(5) as event2" +
                    " where event1.theString = event2.id";
            String outputStmt1 = joinStatement + " output every 1 events";
            String outputStmt3 = joinStatement + " output every 3 events";

            env.compileDeploy("@name('s1') " + outputStmt1).addListener("s1");
            env.compileDeploy("@name('s3') " + outputStmt3).addListener("s3");

            // send event 1
            sendJoinEvents(env, "IBM");

            assertTrue(env.listener("s1").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s1").getLastNewData().length);
            assertNull(env.listener("s1").getLastOldData());

            assertFalse(env.listener("s3").getAndClearIsInvoked());
            assertNull(env.listener("s3").getLastNewData());
            assertNull(env.listener("s3").getLastOldData());

            // send event 2
            sendJoinEvents(env, "MSFT");

            assertTrue(env.listener("s1").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s1").getLastNewData().length);
            assertNull(env.listener("s1").getLastOldData());

            assertFalse(env.listener("s3").getAndClearIsInvoked());
            assertNull(env.listener("s3").getLastNewData());
            assertNull(env.listener("s3").getLastOldData());

            // send event 3
            sendJoinEvents(env, "YAH");

            assertTrue(env.listener("s1").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s1").getLastNewData().length);
            assertNull(env.listener("s1").getLastOldData());

            assertTrue(env.listener("s3").getAndClearIsInvoked());
            Assert.assertEquals(3, env.listener("s3").getLastNewData().length);
            assertNull(env.listener("s3").getLastOldData());

            env.undeployAll();
        }
    }

    private static class ResultSetLimitTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String selectStatement = "@name('s0') select * from SupportBean#length(5)";

            // test integer seconds
            String statementString1 = selectStatement +
                " output every 3 seconds";
            timeCallback(env, statementString1, 3000);

            // test fractional seconds
            String statementString2 = selectStatement +
                " output every 3.3 seconds";
            timeCallback(env, statementString2, 3300);

            // test integer minutes
            String statementString3 = selectStatement +
                " output every 2 minutes";
            timeCallback(env, statementString3, 120000);

            // test fractional minutes
            String statementString4 =
                "@name('s0') select * from SupportBean#length(5) output every .05 minutes";
            timeCallback(env, statementString4, 3000);
        }
    }

    private static class ResultSetTimeBatchOutputEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportBean#time_batch(10 seconds) output every 10 seconds";
            env.compileDeploy(stmtText).addListener("s0");

            sendTimer(env, 0);
            sendTimer(env, 10000);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 20000);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "e1");
            sendTimer(env, 30000);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 40000);
            EventBean[] newEvents = env.listener("s0").getAndResetLastNewData();
            assertEquals(1, newEvents.length);
            Assert.assertEquals("e1", newEvents[0].get("theString"));
            env.listener("s0").reset();

            sendTimer(env, 50000);
            assertTrue(env.listener("s0").isInvoked());
            env.listener("s0").reset();

            sendTimer(env, 60000);
            assertTrue(env.listener("s0").isInvoked());
            env.listener("s0").reset();

            sendTimer(env, 70000);
            assertTrue(env.listener("s0").isInvoked());
            env.listener("s0").reset();

            sendEvent(env, "e2");
            sendEvent(env, "e3");
            sendTimer(env, 80000);
            newEvents = env.listener("s0").getAndResetLastNewData();
            assertEquals(2, newEvents.length);
            Assert.assertEquals("e2", newEvents[0].get("theString"));
            Assert.assertEquals("e3", newEvents[1].get("theString"));

            sendTimer(env, 90000);
            assertTrue(env.listener("s0").isInvoked());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetSimpleNoJoinAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionSimpleNoJoinAll(env, outputLimitOpt);
            }
        }

        private static void tryAssertionSimpleNoJoinAll(RegressionEnvironment env, SupportOutputLimitOpt opt) {
            String epl = opt.getHint() + "@name('s0') select longBoxed " +
                "from SupportBean#length(3) " +
                "output all every 2 events";

            env.compileDeploy(epl).addListener("s0");
            tryAssertAll(env);

            epl = opt.getHint() + "@name('s0') select longBoxed " +
                "from SupportBean#length(3) " +
                "output every 2 events";

            env.compileDeploy(epl).addListener("s0");
            tryAssertAll(env);

            epl = opt.getHint() + "@name('s0') select * " +
                "from SupportBean#length(3) " +
                "output every 2 events";

            env.compileDeploy(epl).addListener("s0");
            tryAssertAll(env);

            env.undeployAll();
        }
    }

    private static class ResultSetSimpleNoJoinLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select longBoxed " +
                "from SupportBean#length(3) " +
                "output last every 2 events";

            env.compileDeploy(epl).addListener("s0");
            tryAssertLast(env);

            epl = "@name('s0') select * " +
                "from SupportBean#length(3) " +
                "output last every 2 events";

            env.compileDeploy(epl).addListener("s0");
            tryAssertLast(env);

            env.undeployAll();
        }
    }

    private static class ResultSetSimpleJoinAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                tryAssertionSimpleJoinAll(env, outputLimitOpt);
            }
        }
    }

    private static class ResultSetSimpleJoinLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select longBoxed " +
                "from SupportBeanString#length(3) as one, " +
                "SupportBean#length(3) as two " +
                "output last every 2 events";

            createStmtAndListenerJoin(env, epl);
            tryAssertLast(env);
            env.undeployAll();
        }
    }

    private static class ResultSetLimitEventSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String selectStmt = "select * from SupportBean#length(5)";
            String statement1 = "@name('s0') " + selectStmt + " output every 1 events";
            String statement2 = "@name('s1') " + selectStmt + " output every 2 events";
            String statement3 = "@name('s2') " + selectStmt + " output every 3 events";

            env.compileDeploy(statement1).addListener("s0");
            env.compileDeploy(statement2).addListener("s1");
            env.compileDeploy(statement3).addListener("s2");

            // send event 1
            sendEvent(env, "IBM");

            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            assertNull(env.listener("s0").getLastOldData());

            assertFalse(env.listener("s1").getAndClearIsInvoked());
            assertNull(env.listener("s1").getLastNewData());
            assertNull(env.listener("s1").getLastOldData());

            assertFalse(env.listener("s2").getAndClearIsInvoked());
            assertNull(env.listener("s2").getLastNewData());
            assertNull(env.listener("s2").getLastOldData());

            // send event 2
            sendEvent(env, "MSFT");

            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            assertNull(env.listener("s0").getLastOldData());

            assertTrue(env.listener("s1").getAndClearIsInvoked());
            Assert.assertEquals(2, env.listener("s1").getLastNewData().length);
            assertNull(env.listener("s1").getLastOldData());

            assertFalse(env.listener("s2").getAndClearIsInvoked());

            // send event 3
            sendEvent(env, "YAH");

            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            assertNull(env.listener("s0").getLastOldData());

            assertFalse(env.listener("s1").getAndClearIsInvoked());

            assertTrue(env.listener("s2").getAndClearIsInvoked());
            Assert.assertEquals(3, env.listener("s2").getLastNewData().length);
            assertNull(env.listener("s2").getLastOldData());

            env.undeployAll();
        }
    }

    private static class ResultSetLimitSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String selectStmt = "@name('s0') select * from SupportBean#time(10) output snapshot every 3 events";
            env.compileDeploy(selectStmt).addListener("s0");

            sendTimer(env, 1000);
            sendEvent(env, "IBM");
            sendEvent(env, "MSFT");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 2000);
            sendEvent(env, "YAH");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"IBM"}, {"MSFT"}, {"YAH"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 3000);
            sendEvent(env, "s4");
            sendEvent(env, "s5");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 10000);
            sendEvent(env, "s6");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"IBM"}, {"MSFT"}, {"YAH"}, {"s4"}, {"s5"}, {"s6"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 11000);
            sendEvent(env, "s7");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "s8");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "s9");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"YAH"}, {"s4"}, {"s5"}, {"s6"}, {"s7"}, {"s8"}, {"s9"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 14000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s6"}, {"s7"}, {"s8"}, {"s9"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendEvent(env, "s10");
            sendEvent(env, "s11");
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 23000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s10"}, {"s11"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendEvent(env, "s12");
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetFirstSimpleHavingAndNoHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionFirstSimpleHavingAndNoHaving(env, "");
            tryAssertionFirstSimpleHavingAndNoHaving(env, "having intPrimitive != 0");
        }

        private static void tryAssertionFirstSimpleHavingAndNoHaving(RegressionEnvironment env, String having) {
            String epl = "@name('s0') select theString from SupportBean " + having + " output first every 3 events";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E4"});

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetLimitSnapshotJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            sendTimer(env, 0);
            String selectStmt = "@name('s0') select theString from SupportBean#time(10) as s," +
                "SupportMarketDataBean#keepall as m where s.theString = m.symbol output snapshot every 3 events order by symbol asc";
            env.compileDeploy(selectStmt).addListener("s0");

            for (String symbol : "s0,s1,s2,s3,s4,s5,s6,s7,s8,s9,s10,s11".split(",")) {
                env.sendEventBean(new SupportMarketDataBean(symbol, 0, 0L, ""));
            }

            sendTimer(env, 1000);
            sendEvent(env, "s0");
            sendEvent(env, "s1");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 2000);
            sendEvent(env, "s2");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s0"}, {"s1"}, {"s2"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 3000);
            sendEvent(env, "s4");
            sendEvent(env, "s5");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimer(env, 10000);
            sendEvent(env, "s6");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s0"}, {"s1"}, {"s2"}, {"s4"}, {"s5"}, {"s6"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 11000);
            sendEvent(env, "s7");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "s8");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "s9");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s2"}, {"s4"}, {"s5"}, {"s6"}, {"s7"}, {"s8"}, {"s9"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendTimer(env, 14000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s6"}, {"s7"}, {"s8"}, {"s9"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendEvent(env, "s10");
            sendEvent(env, "s11");
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 23000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), new String[]{"theString"}, new Object[][]{{"s10"}, {"s11"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendEvent(env, "s12");
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetSnapshotMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");

            String epl = "@name('s0') select * from SupportBean#lastevent output snapshot every 1 month";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class ResultSetFirstMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");

            String epl = "@name('s0') select * from SupportBean#lastevent output first every 1 month";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E2", 2));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.sendEventBean(new SupportBean("E3", 3));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E4"}});

            env.undeployAll();
        }
    }

    private static void createStmtAndListenerJoin(RegressionEnvironment env, String epl) {
        env.compileDeploy(epl).addListener("s0");
        env.sendEventBean(new SupportBeanString(JOIN_KEY));
    }

    private static void tryAssertLast(RegressionEnvironment env) {
        // send an event
        sendEvent(env, 1);

        // check no update
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // send another event
        sendEvent(env, 2);

        // check update, only the last event present
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
        Assert.assertEquals(2L, env.listener("s0").getLastNewData()[0].get("longBoxed"));
        assertNull(env.listener("s0").getLastOldData());

        env.undeployAll();
    }

    private static void sendTimer(RegressionEnvironment env, long time) {
        env.advanceTime(time);
    }

    private static void sendEvent(RegressionEnvironment env, String s) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setDoubleBoxed(0.0);
        bean.setIntPrimitive(0);
        bean.setIntBoxed(0);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed) {
        sendEvent(env, longBoxed, 0, (short) 0);
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }

    private static void timeCallback(RegressionEnvironment env, String epl, int timeToCallback) {
        // set the clock to 0
        AtomicLong currentTime = new AtomicLong();
        sendTimeEvent(env, 0, currentTime);

        // create the EPL statement and add a listener
        env.compileDeploy(epl).addListener("s0");

        // send an event
        sendEvent(env, "IBM");

        // check that the listener hasn't been updated
        sendTimeEvent(env, timeToCallback - 1, currentTime);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(env, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
        assertNull(env.listener("s0").getLastOldData());

        // send another event
        sendEvent(env, "MSFT");

        // check that the listener hasn't been updated
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(env, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
        assertNull(env.listener("s0").getLastOldData());

        // don't send an event
        // check that the listener hasn't been updated
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(env, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        assertNull(env.listener("s0").getLastNewData());
        assertNull(env.listener("s0").getLastOldData());

        // don't send an event
        // check that the listener hasn't been updated
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(env, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        assertNull(env.listener("s0").getLastNewData());
        assertNull(env.listener("s0").getLastOldData());

        // send several events
        sendEvent(env, "YAH");
        sendEvent(env, "s4");
        sendEvent(env, "s5");

        // check that the listener hasn't been updated
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // update the clock
        sendTimeEvent(env, timeToCallback, currentTime);

        // check that the listener has been updated
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(3, env.listener("s0").getLastNewData().length);
        assertNull(env.listener("s0").getLastOldData());

        env.undeployAll();
    }

    private static void tryAssertionSimpleJoinAll(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "@name('s0') select longBoxed  " +
            "from SupportBeanString#length(3) as one, " +
            "SupportBean#length(3) as two " +
            "output all every 2 events";

        createStmtAndListenerJoin(env, epl);
        tryAssertAll(env);

        env.undeployAll();
    }

    private static void tryAssertAll(RegressionEnvironment env) {
        // send an event
        sendEvent(env, 1);

        // check no update
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        // send another event
        sendEvent(env, 2);

        // check update, all events present
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        Assert.assertEquals(2, env.listener("s0").getLastNewData().length);
        Assert.assertEquals(1L, env.listener("s0").getLastNewData()[0].get("longBoxed"));
        Assert.assertEquals(2L, env.listener("s0").getLastNewData()[1].get("longBoxed"));
        assertNull(env.listener("s0").getLastOldData());

        env.undeployAll();
    }

    private static void tryAssertion34(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};

        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion15_16(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);

        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion12(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(800, 1, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(1500, 2, new Object[][]{{"YAH", 10000L, 1d}});
        expected.addResultInsert(2100, 1, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsert(4900, 1, new Object[][]{{"YAH", 11500L, 3d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 1d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultRemove(7000, 0, new Object[][]{{"IBM", 150L, 24d}, {"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion13_14(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"YAH", 11500L, 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 1d}}, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion78(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 24d}, {"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsRem(4200, 0, null, null);
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsRem(6200, 0, null, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"IBM", 150L, 24d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion56(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsRem(3200, 0, null, null);
        expected.addResultInsert(4200, 0, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 150L, 22d}, {"YAH", 11500L, 3d}});
        expected.addResultInsRem(6200, 0, new Object[][]{{"YAH", 10500L, 1d}}, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(7200, 0, new Object[][]{{"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void tryAssertion17IStream(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultInsert(5900, 1, new Object[][]{{"YAH", 10500L, 1.0d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private static void tryAssertion17IRStream(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(200, 1, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultInsert(1500, 1, new Object[][]{{"IBM", 150L, 24d}});
        expected.addResultInsert(3500, 1, new Object[][]{{"YAH", 11000L, 2d}});
        expected.addResultInsert(4300, 1, new Object[][]{{"IBM", 150L, 22d}});
        expected.addResultRemove(5700, 0, new Object[][]{{"IBM", 100L, 25d}});
        expected.addResultRemove(6300, 0, new Object[][]{{"MSFT", 5000L, 9d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected, ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED);
        execution.execute(false);
    }

    private static void tryAssertion18(RegressionEnvironment env, String stmtText, String outputLimit) {
        sendTimer(env, 0);
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = new String[]{"symbol", "volume", "price"};
        ResultAssertTestResult expected = new ResultAssertTestResult(CATEGORY, outputLimit, fields);
        expected.addResultInsert(1200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}});
        expected.addResultInsert(2200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsert(3200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}});
        expected.addResultInsert(4200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}});
        expected.addResultInsert(5200, 0, new Object[][]{{"IBM", 100L, 25d}, {"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}});
        expected.addResultInsert(6200, 0, new Object[][]{{"MSFT", 5000L, 9d}, {"IBM", 150L, 24d}, {"YAH", 10000L, 1d}, {"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}, {"YAH", 10500L, 1d}});
        expected.addResultInsert(7200, 0, new Object[][]{{"IBM", 155L, 26d}, {"YAH", 11000L, 2d}, {"IBM", 150L, 22d}, {"YAH", 11500L, 3d}, {"YAH", 10500L, 1d}});

        ResultAssertExecution execution = new ResultAssertExecution(stmtText, env, expected);
        execution.execute(false);
    }

    private static void sendTimeEvent(RegressionEnvironment env, int timeIncrement, AtomicLong currentTime) {
        currentTime.addAndGet(timeIncrement);
        env.advanceTime(currentTime.get());
    }

    private static void sendJoinEvents(RegressionEnvironment env, String s) {
        SupportBean event1 = new SupportBean();
        event1.setTheString(s);
        event1.setDoubleBoxed(0.0);
        event1.setIntPrimitive(0);
        event1.setIntBoxed(0);

        SupportBean_A event2 = new SupportBean_A(s);

        env.sendEventBean(event1);
        env.sendEventBean(event2);
    }

    private static void sendMDEvent(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }
}
