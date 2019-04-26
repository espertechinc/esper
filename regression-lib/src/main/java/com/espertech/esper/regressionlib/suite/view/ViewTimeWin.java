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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPStatement;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.schedule.SupportDateTimeUtil.timePlusMonth;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

public class ViewTimeWin {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewTimeWindowSceneOne());
        execs.add(new ViewTimeWindowSceneTwo());
        execs.add(new ViewTimeJustSelectStar());
        execs.add(new ViewTimeSum());
        execs.add(new ViewTimeSumGroupBy());
        execs.add(new ViewTimeSumWFilter());
        execs.add(new ViewTimeWindowMonthScoped());
        execs.add(new ViewTimeWindowWPrev());
        execs.add(new ViewTimeWindowPreparedStmt());
        execs.add(new ViewTimeWindowVariableStmt());
        execs.add(new ViewTimeWindowTimePeriod());
        execs.add(new ViewTimeWindowVariableTimePeriodStmt());
        execs.add(new ViewTimeWindowTimePeriodParams());
        execs.add(new ViewTimeWindowFlipTimer(0, "1", 1000));
        execs.add(new ViewTimeWindowFlipTimer(123456789, "10", 123456789 + 10 * 1000));
        execs.add(new ViewTimeWindowFlipTimer(0, "1 months 10 milliseconds", timePlusMonth(0, 1) + 10));

        long currentTime = DateTime.parseDefaultMSec("2002-05-1T08:00:01.999");
        execs.add(new ViewTimeWindowFlipTimer(currentTime, "1 months 50 milliseconds", timePlusMonth(currentTime, 1) + 50));

        return execs;
    }

    public static class ViewTimeWindowSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            env.advanceTime(0);
            String epl = "@Name('s0') select irstream * from SupportBean#time(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);
            env.advanceTime(1000);
            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}});
            env.advanceTime(2000);
            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            env.advanceTime(3000);
            sendSupportBean(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            env.advanceTime(10999);
            Assert.assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.advanceTime(11000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E1"});

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}});
            env.advanceTime(12000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E2"});

            env.milestone(5);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E3"}});
            sendSupportBean(env, "E4");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            env.milestone(6);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E3"}, {"E4"}});
            sendSupportBean(env, "E5");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            env.milestone(7);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
            env.advanceTime(13000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E3"});

            env.milestone(8);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E4"}, {"E5"}});
            env.advanceTime(22000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastOldData(), fields, new Object[][]{{"E4"}, {"E5"}});

            env.milestone(9);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{});

            env.milestone(10);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{});

            env.undeployAll();
        }
    }

    public static class ViewTimeWindowSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            env.advanceTime(1000);
            String epl = "@Name('s0') select irstream * from SupportBean#time(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendSupportBean(env, "E1");
            sendSupportBean(env, "E2");

            env.milestone(1);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});

            sendSupportBean(env, "E3");
            sendSupportBean(env, "E4");

            env.milestone(2);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

            env.advanceTime(2000);
            sendSupportBean(env, "E5");

            env.milestone(3);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});

            env.advanceTime(10999);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});

            env.milestone(4);

            env.advanceTime(11000);

            env.milestone(5);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E5"}});

            env.milestone(6);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E5"}});

            env.undeployAll();
        }
    }

    private static class ViewTimeWindowMonthScoped implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            String epl = "@name('s0') select rstream * from SupportBean#time(1 month)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            sendCurrentTime(env, "2002-02-15T09:00:00.000");
            env.sendEventBean(new SupportBean("E2", 2));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

            sendCurrentTimeWithMinus(env, "2002-03-15T09:00:00.000", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            sendCurrentTime(env, "2002-03-15T09:00:00.000");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class ViewTimeSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum from SupportMarketDataBean#time(30)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            assertSelectResultType(env.statement("s0"));

            sendEvent(env, SYMBOL_DELL, 10000, 51);
            assertEvents(env, SYMBOL_DELL, 10000, 51, false);

            sendEvent(env, SYMBOL_IBM, 20000, 52);
            assertEvents(env, SYMBOL_IBM, 20000, 103, false);

            sendEvent(env, SYMBOL_DELL, 40000, 45);
            assertEvents(env, SYMBOL_DELL, 40000, 148, false);

            env.advanceTime(35000);

            //These events are out of the window and new sums are generated

            sendEvent(env, SYMBOL_IBM, 30000, 70);
            assertEvents(env, SYMBOL_IBM, 30000, 70, false);

            sendEvent(env, SYMBOL_DELL, 10000, 20);
            assertEvents(env, SYMBOL_DELL, 10000, 90, false);

            env.undeployAll();
        }
    }

    private static class ViewTimeSumGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum " +
                "from SupportMarketDataBean#time(30) group by symbol";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryGroupByAssertions(env);

            env.undeployAll();
        }
    }

    private static class ViewTimeSumWFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select symbol, volume, sum(price) as mySum from SupportMarketDataBean(symbol = 'IBM')#time(30)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            trySingleAssertion(env);

            env.undeployAll();
        }
    }

    private static class ViewTimeJustSelectStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String text = "@name('s0') select irstream * from SupportMarketDataBean#time(1 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");
            String[] fields = new String[]{"symbol"};

            env.advanceTime(500);
            env.sendEventBean(makeMarketDataEvent("E1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E1"}}, null);

            env.milestone(1);

            env.advanceTime(600);
            env.sendEventBean(makeMarketDataEvent("E2"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E2"}}, null);

            env.milestone(2);

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.statement("s0").iterator());
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});

            env.advanceTime(1500);
            env.listener("s0").assertNewOldData(null, new Object[][]{{"symbol", "E1"}}); // olddata

            env.milestone(3);

            env.advanceTime(1600);
            env.listener("s0").assertNewOldData(null, new Object[][]{{"symbol", "E2"}}); // olddata

            env.milestone(4);

            env.advanceTime(2000);

            env.milestone(5);

            env.sendEventBean(makeMarketDataEvent("E3"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E3"}}, null);

            env.milestone(6);

            env.undeployAll();
        }
    }

    private static void tryGroupByAssertions(RegressionEnvironment env) {
        assertSelectResultType(env.statement("s0"));

        env.advanceTime(0);

        sendEvent(env, SYMBOL_DELL, 10000, 51);
        assertEvents(env, SYMBOL_DELL, 10000, 51, false);

        sendEvent(env, SYMBOL_IBM, 30000, 70);
        assertEvents(env, SYMBOL_IBM, 30000, 70, false);

        sendEvent(env, SYMBOL_DELL, 20000, 52);
        assertEvents(env, SYMBOL_DELL, 20000, 103, false);

        sendEvent(env, SYMBOL_IBM, 30000, 70);
        assertEvents(env, SYMBOL_IBM, 30000, 140, false);

        env.advanceTime(35000);

        //These events are out of the window and new sums are generated
        sendEvent(env, SYMBOL_DELL, 10000, 90);
        assertEvents(env, SYMBOL_DELL, 10000, 90, false);

        sendEvent(env, SYMBOL_IBM, 30000, 120);
        assertEvents(env, SYMBOL_IBM, 30000, 120, false);

        sendEvent(env, SYMBOL_DELL, 20000, 90);
        assertEvents(env, SYMBOL_DELL, 20000, 180, false);

        sendEvent(env, SYMBOL_IBM, 30000, 120);
        assertEvents(env, SYMBOL_IBM, 30000, 240, false);
    }

    private static void trySingleAssertion(RegressionEnvironment env) {
        assertSelectResultType(env.statement("s0"));

        env.advanceTime(0);

        sendEvent(env, SYMBOL_IBM, 20000, 52);
        assertEvents(env, SYMBOL_IBM, 20000, 52, false);

        sendEvent(env, SYMBOL_IBM, 20000, 100);
        assertEvents(env, SYMBOL_IBM, 20000, 152, false);

        env.advanceTime(35000);

        //These events are out of the window and new sums are generated
        sendEvent(env, SYMBOL_IBM, 20000, 252);
        assertEvents(env, SYMBOL_IBM, 20000, 252, false);

        sendEvent(env, SYMBOL_IBM, 20000, 100);
        assertEvents(env, SYMBOL_IBM, 20000, 352, false);
    }

    public static class ViewTimeWindowWPrev implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String text = "@name('s0') select irstream symbol, " +
                "prev(1, symbol) as prev1, " +
                "prevtail(symbol) as prevtail, " +
                "prevcount(symbol) as prevCountSym, " +
                "prevwindow(symbol) as prevWindowSym " +
                "from SupportMarketDataBean#time(1 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");
            String[] fields = new String[]{"symbol", "prev1", "prevtail", "prevCountSym", "prevWindowSym"};

            env.advanceTime(500);
            env.sendEventBean(makeMarketDataEvent("E1"));
            env.sendEventBean(makeMarketDataEvent("E2"));
            env.sendEventBean(makeMarketDataEvent("E3"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields,
                new Object[][]{{"E1", null, "E1", 1L, new Object[]{"E1"}}, {"E2", "E1", "E1", 2L, new Object[]{"E2", "E1"}}, {"E3", "E2", "E1", 3L, new Object[]{"E3", "E2", "E1"}}});
            env.listener("s0").reset();

            env.milestone(1);

            env.advanceTime(1200);
            env.sendEventBean(makeMarketDataEvent("E4"));
            env.sendEventBean(makeMarketDataEvent("E5"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields,
                new Object[][]{
                    {"E4", "E3", "E1", 4L, new Object[]{"E4", "E3", "E2", "E1"}},
                    {"E5", "E4", "E1", 5L, new Object[]{"E5", "E4", "E3", "E2", "E1"}}});
            env.listener("s0").reset();

            env.milestone(2);

            env.advanceTime(1600);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getOldDataListFlattened(), "symbol".split(","), new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            env.listener("s0").reset();

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E6"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E6"}}, null);

            env.milestone(4);

            env.undeployAll();
        }
    }

    private static class ViewTimeWindowPreparedStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String text = "select rstream theString from SupportBean#time(?::int)";
            EPCompiled compiled = env.compile(text);

            env.deploy(compiled, new DeploymentOptions()
                .setStatementSubstitutionParameter(prepared -> prepared.setObject(1, 4))
                .setStatementNameRuntime(ctx -> "s0"));
            env.deploy(compiled, new DeploymentOptions()
                .setStatementSubstitutionParameter(prepared -> prepared.setObject(1, 3))
                .setStatementNameRuntime(ctx -> "s1"));
            env.addListener("s0").addListener("s1");

            runAssertion(env);

            env.undeployAll();
        }
    }

    private static class ViewTimeWindowVariableStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String text = "select rstream theString from SupportBean#time(TIME_WIN_ONE)";
            env.compileDeploy("@name('s0') " + text).addListener("s0");

            env.runtime().getVariableService().setVariableValue(null, "TIME_WIN_ONE", 3);

            env.compileDeploy("@name('s1') " + text).addListener("s1");

            runAssertion(env);

            env.undeployAll();
        }
    }

    private static class ViewTimeWindowTimePeriod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String text = "@name('s0') select rstream theString from SupportBean#time(4 sec)";
            env.compileDeploy(text).addListener("s0");

            text = "@name('s1') select rstream theString from SupportBean#time(3000 milliseconds)";
            env.compileDeploy(text).addListener("s1").milestone(0);

            runAssertion(env);

            env.undeployAll();
        }
    }

    private static class ViewTimeWindowVariableTimePeriodStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String text = "select rstream theString from SupportBean#time(TIME_WIN_TWO milliseconds)";
            env.compileDeploy("@name('s0')" + text).addListener("s0");

            text = "select rstream theString from SupportBean#time(TIME_WIN_TWO minutes)";
            env.runtime().getVariableService().setVariableValue(null, "TIME_WIN_TWO", 0.05);
            env.compileDeploy("@name('s1')" + text).addListener("s1");

            runAssertion(env);

            env.undeployAll();
        }
    }

    private static class ViewTimeWindowTimePeriodParams implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryTimeWindow(env, "30000");
            tryTimeWindow(env, "30E6 milliseconds");
            tryTimeWindow(env, "30000 seconds");
            tryTimeWindow(env, "500 minutes");
            tryTimeWindow(env, "8.33333333333333333333 hours");
            tryTimeWindow(env, "0.34722222222222222222222222222222 days");
            tryTimeWindow(env, "0.1 hour 490 min 240 sec");
        }
    }

    public static class ViewTimeWindowFlipTimer implements RegressionExecution {

        private final long startTime;
        private final String size;
        private final long flipTime;

        public ViewTimeWindowFlipTimer(long startTime, String size, long flipTime) {
            this.startTime = startTime;
            this.size = size;
            this.flipTime = flipTime;
        }

        public void run(RegressionEnvironment env) {
            env.advanceTime(startTime);

            String[] fields = "theString".split(",");
            String epl = "@name('s0') select * from SupportBean#time(" + size + ")";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.advanceTime(flipTime - 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.advanceTime(flipTime);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);

            env.undeployAll();
        }
    }

    private static void tryTimeWindow(RegressionEnvironment env, String intervalSpec) {
        env.advanceTime(0);
        String epl = "@name('s0') select irstream * from SupportBean#time(" + intervalSpec + ")";
        env.compileDeploy(epl).addListener("s0");

        sendEvent(env, "E1");
        env.listener("s0").reset();

        sendTimerAssertNotInvoked(env, 29999 * 1000);
        sendTimerAssertInvoked(env, 30000 * 1000);

        env.undeployAll();
    }

    private static void assertEvents(RegressionEnvironment env, String symbol, long volume, double sum, boolean unique) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        if (!unique)
            assertNull(oldData);

        assertEquals(1, newData.length);

        Assert.assertEquals(symbol, newData[0].get("symbol"));
        Assert.assertEquals(volume, newData[0].get("volume"));
        Assert.assertEquals(sum, newData[0].get("mySum"));

        env.listener("s0").reset();
    }

    private static void assertSelectResultType(EPStatement stmt) {
        Assert.assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        Assert.assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));
        Assert.assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static void runAssertion(RegressionEnvironment env) {
        env.advanceTime(1000);
        sendEvent(env, "E1");

        env.advanceTime(2000);
        sendEvent(env, "E2");

        env.advanceTime(3000);
        sendEvent(env, "E3");

        assertFalse(env.listener("s0").isInvoked());
        assertFalse(env.listener("s1").isInvoked());

        env.advanceTime(4000);
        assertEquals("E1", env.listener("s1").assertOneGetNewAndReset().get("theString"));
        assertFalse(env.listener("s0").isInvoked());

        env.advanceTime(5000);
        assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("theString"));
    }

    private static void sendEvent(RegressionEnvironment env, String theString) {
        SupportBean theEvent = new SupportBean(theString, 1);
        env.sendEventBean(theEvent);
    }

    private static void sendTimerAssertNotInvoked(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
        Assert.assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendTimerAssertInvoked(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
        assertTrue(env.listener("s0").isInvoked());
        env.listener("s0").reset();
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }
}
