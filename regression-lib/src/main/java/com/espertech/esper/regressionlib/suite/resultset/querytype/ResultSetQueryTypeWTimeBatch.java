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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ResultSetQueryTypeWTimeBatch {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeTimeBatchRowForAllNoJoin());
        execs.add(new ResultSetQueryTypeTimeBatchRowForAllJoin());
        execs.add(new ResultSetQueryTypeTimeBatchRowPerEventNoJoin());
        execs.add(new ResultSetQueryTypeTimeBatchRowPerEventJoin());
        execs.add(new ResultSetQueryTypeTimeBatchRowPerGroupNoJoin());
        execs.add(new ResultSetQueryTypeTimeBatchRowPerGroupJoin());
        execs.add(new ResultSetQueryTypeTimeBatchAggrGroupedNoJoin());
        execs.add(new ResultSetQueryTypeTimeBatchAggrGroupedJoin());
        return execs;
    }

    private static class ResultSetQueryTypeTimeBatchRowForAllNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream sum(price) as sumPrice from SupportMarketDataBean#time_batch(1 sec)";
            env.compileDeploy(stmtText).addListener("s0");

            // send first batch
            sendMDEvent(env, "DELL", 10, 0L);
            sendMDEvent(env, "IBM", 15, 0L);
            sendMDEvent(env, "DELL", 20, 0L);
            sendTimer(env, 1000);

            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], 45d);

            // send second batch
            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);

            newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], 20d);

            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(1, oldEvents.length);
            assertEvent(oldEvents[0], 45d);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchRowForAllJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream sum(price) as sumPrice from SupportMarketDataBean#time_batch(1 sec) as S0, SupportBean#keepall as S1 where S0.symbol = S1.theString";
            env.compileDeploy(stmtText).addListener("s0");

            sendSupportEvent(env, "DELL");
            sendSupportEvent(env, "IBM");

            // send first batch
            sendMDEvent(env, "DELL", 10, 0L);
            sendMDEvent(env, "IBM", 15, 0L);
            sendMDEvent(env, "DELL", 20, 0L);
            sendTimer(env, 1000);

            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], 45d);

            // send second batch
            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);

            newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], 20d);

            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(1, oldEvents.length);
            assertEvent(oldEvents[0], 45d);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchRowPerEventNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream symbol, sum(price) as sumPrice from SupportMarketDataBean#time_batch(1 sec)";
            env.compileDeploy(stmtText).addListener("s0");

            // send first batch
            sendMDEvent(env, "DELL", 10, 0L);
            sendMDEvent(env, "IBM", 15, 0L);
            sendMDEvent(env, "DELL", 20, 0L);
            sendTimer(env, 1000);

            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(3, newEvents.length);
            assertEvent(newEvents[0], "DELL", 45d);
            assertEvent(newEvents[1], "IBM", 45d);
            assertEvent(newEvents[2], "DELL", 45d);

            // send second batch
            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);

            newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], "IBM", 20d);

            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(3, oldEvents.length);
            assertEvent(oldEvents[0], "DELL", 20d);
            assertEvent(oldEvents[1], "IBM", 20d);
            assertEvent(oldEvents[2], "DELL", 20d);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchRowPerEventJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream symbol, sum(price) as sumPrice from SupportMarketDataBean#time_batch(1 sec) as S0, SupportBean#keepall as S1 where S0.symbol = S1.theString";
            env.compileDeploy(stmtText).addListener("s0");

            sendSupportEvent(env, "DELL");
            sendSupportEvent(env, "IBM");

            // send first batch
            sendMDEvent(env, "DELL", 10, 0L);
            sendMDEvent(env, "IBM", 15, 0L);
            sendMDEvent(env, "DELL", 20, 0L);
            sendTimer(env, 1000);

            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(3, newEvents.length);
            assertEvent(newEvents[0], "DELL", 45d);
            assertEvent(newEvents[1], "IBM", 45d);
            assertEvent(newEvents[2], "DELL", 45d);

            // send second batch
            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);

            newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], "IBM", 20d);

            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(3, oldEvents.length);
            assertEvent(oldEvents[0], "DELL", 20d);
            assertEvent(oldEvents[1], "IBM", 20d);
            assertEvent(oldEvents[2], "DELL", 20d);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchRowPerGroupNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream symbol, sum(price) as sumPrice from SupportMarketDataBean#time_batch(1 sec) group by symbol order by symbol asc";
            env.compileDeploy(stmtText).addListener("s0");

            // send first batch
            sendMDEvent(env, "DELL", 10, 0L);
            sendMDEvent(env, "IBM", 15, 0L);
            sendMDEvent(env, "DELL", 20, 0L);
            sendTimer(env, 1000);

            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(2, newEvents.length);
            assertEvent(newEvents[0], "DELL", 30d);
            assertEvent(newEvents[1], "IBM", 15d);

            // send second batch
            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);

            newEvents = env.listener("s0").getLastNewData();
            assertEquals(2, newEvents.length);
            assertEvent(newEvents[0], "DELL", null);
            assertEvent(newEvents[1], "IBM", 20d);

            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(2, oldEvents.length);
            assertEvent(oldEvents[0], "DELL", 30d);
            assertEvent(oldEvents[1], "IBM", 15d);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchRowPerGroupJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream symbol, sum(price) as sumPrice " +
                " from SupportMarketDataBean#time_batch(1 sec) as S0, SupportBean#keepall as S1" +
                " where S0.symbol = S1.theString " +
                " group by symbol";
            env.compileDeploy(stmtText).addListener("s0");

            sendSupportEvent(env, "DELL");
            sendSupportEvent(env, "IBM");

            // send first batch
            sendMDEvent(env, "DELL", 10, 0L);
            sendMDEvent(env, "IBM", 15, 0L);
            sendMDEvent(env, "DELL", 20, 0L);
            sendTimer(env, 1000);

            String[] fields = "symbol,sumPrice".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"DELL", 30d}, {"IBM", 15d}});

            // send second batch
            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getLastNewData(), fields, new Object[][]{{"DELL", null}, {"IBM", 20d}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastOldData(), fields, new Object[][]{{"DELL", 30d}, {"IBM", 15d}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchAggrGroupedNoJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream symbol, sum(price) as sumPrice, volume from SupportMarketDataBean#time_batch(1 sec) group by symbol";
            env.compileDeploy(stmtText).addListener("s0");

            sendMDEvent(env, "DELL", 10, 200L);
            sendMDEvent(env, "IBM", 15, 500L);
            sendMDEvent(env, "DELL", 20, 250L);

            sendTimer(env, 1000);
            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(3, newEvents.length);
            assertEvent(newEvents[0], "DELL", 30d, 200L);
            assertEvent(newEvents[1], "IBM", 15d, 500L);
            assertEvent(newEvents[2], "DELL", 30d, 250L);

            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);
            newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], "IBM", 20d, 600L);
            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(3, oldEvents.length);
            assertEvent(oldEvents[0], "DELL", null, 200L);
            assertEvent(oldEvents[1], "IBM", 20d, 500L);
            assertEvent(oldEvents[2], "DELL", null, 250L);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeTimeBatchAggrGroupedJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select irstream symbol, sum(price) as sumPrice, volume " +
                "from SupportMarketDataBean#time_batch(1 sec) as S0, SupportBean#keepall as S1" +
                " where S0.symbol = S1.theString " +
                " group by symbol";
            env.compileDeploy(stmtText).addListener("s0");

            sendSupportEvent(env, "DELL");
            sendSupportEvent(env, "IBM");

            sendMDEvent(env, "DELL", 10, 200L);
            sendMDEvent(env, "IBM", 15, 500L);
            sendMDEvent(env, "DELL", 20, 250L);

            sendTimer(env, 1000);
            EventBean[] newEvents = env.listener("s0").getLastNewData();
            assertEquals(3, newEvents.length);
            assertEvent(newEvents[0], "DELL", 30d, 200L);
            assertEvent(newEvents[1], "IBM", 15d, 500L);
            assertEvent(newEvents[2], "DELL", 30d, 250L);

            sendMDEvent(env, "IBM", 20, 600L);
            sendTimer(env, 2000);
            newEvents = env.listener("s0").getLastNewData();
            assertEquals(1, newEvents.length);
            assertEvent(newEvents[0], "IBM", 20d, 600L);
            EventBean[] oldEvents = env.listener("s0").getLastOldData();
            assertEquals(3, oldEvents.length);
            assertEvent(oldEvents[0], "DELL", null, 200L);
            assertEvent(oldEvents[1], "IBM", 20d, 500L);
            assertEvent(oldEvents[2], "DELL", null, 250L);

            env.undeployAll();
        }
    }

    private static void sendSupportEvent(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, -1));
    }

    private static void sendMDEvent(RegressionEnvironment env, String symbol, double price, Long volume) {
        env.sendEventBean(new SupportMarketDataBean(symbol, price, volume, null));
    }

    private static void assertEvent(EventBean theEvent, String symbol, Double sumPrice, Long volume) {
        Assert.assertEquals(symbol, theEvent.get("symbol"));
        Assert.assertEquals(sumPrice, theEvent.get("sumPrice"));
        Assert.assertEquals(volume, theEvent.get("volume"));
    }

    private static void assertEvent(EventBean theEvent, String symbol, Double sumPrice) {
        Assert.assertEquals(symbol, theEvent.get("symbol"));
        Assert.assertEquals(sumPrice, theEvent.get("sumPrice"));
    }

    private static void assertEvent(EventBean theEvent, Double sumPrice) {
        Assert.assertEquals(sumPrice, theEvent.get("sumPrice"));
    }

    private static void sendTimer(RegressionEnvironment env, long time) {
        env.advanceTime(time);
    }
}
