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
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ResultSetQueryTypeRowPerEvent {
    private final static String JOIN_KEY = "KEY";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeRowPerEventSumOneView());
        execs.add(new ResultSetQueryTypeRowPerEventSumJoin());
        execs.add(new ResultSetQueryTypeAggregatedSelectTriggerEvent());
        execs.add(new ResultSetQueryTypeAggregatedSelectUnaggregatedHaving());
        execs.add(new ResultSetQueryTypeSumAvgWithWhere());
        execs.add(new ResultSetQueryTypeRowPerEventDistinct());
        execs.add(new ResultSetQueryTypeRowPerEventDistinctNullable());
        return execs;
    }

    private static class ResultSetQueryTypeRowPerEventSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream longPrimitive, sum(longBoxed) as mySum " +
                "from SupportBean#length(3)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerEventSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream longPrimitive, sum(longBoxed) as mySum " +
                "from SupportBeanString#length(3) as one, SupportBean#length(3) as two " +
                "where one.theString = two.theString";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBeanString(JOIN_KEY));

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregatedSelectTriggerEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select window(s0.*) as rows, sb " +
                "from SupportBean#keepall as sb, SupportBean_S0#keepall as s0 " +
                "where sb.theString = s0.p00";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "K1", "V1"));
            env.sendEventBean(new SupportBean_S0(2, "K1", "V2"));

            env.milestone(0);

            // test SB-direction
            SupportBean b1 = new SupportBean("K1", 0);
            env.sendEventBean(b1);
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            assertEquals(2, events.length);
            for (EventBean event : events) {
                Assert.assertEquals(b1, event.get("sb"));
                assertEquals(2, ((SupportBean_S0[]) event.get("rows")).length);
            }

            // test S0-direction
            env.sendEventBean(new SupportBean_S0(1, "K1", "V3"));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(b1, event.get("sb"));
            assertEquals(3, ((SupportBean_S0[]) event.get("rows")).length);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregatedSelectUnaggregatedHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // ESPER-571
            String epl = "@name('s0') select max(intPrimitive) as val from SupportBean#time(1) having max(intPrimitive) > intBoxed";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 10, 1);
            Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("val"));

            sendEvent(env, "E2", 10, 11);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendEvent(env, "E3", 15, 11);
            Assert.assertEquals(15, env.listener("s0").assertOneGetNewAndReset().get("val"));

            sendEvent(env, "E4", 20, 11);
            Assert.assertEquals(20, env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.milestone(1);

            sendEvent(env, "E5", 25, 25);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumAvgWithWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 'IBM stats' as title, volume, avg(volume) as myAvg, sum(volume) as mySum " +
                "from SupportMarketDataBean#length(3)" +
                "where symbol='IBM'";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendMarketDataEvent(env, "GE", 10L);
            assertFalse(env.listener("s0").isInvoked());

            sendMarketDataEvent(env, "IBM", 20L);
            assertPostedNew(env, 20d, 20L);

            sendMarketDataEvent(env, "XXX", 10000L);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendMarketDataEvent(env, "IBM", 30L);
            assertPostedNew(env, 25d, 50L);

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowPerEventDistinct implements RegressionExecution {
        private final static String SYMBOL_DELL = "DELL";
        private final static String SYMBOL_IBM = "IBM";

        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select irstream symbol, sum(distinct volume) as volSum " +
                "from SupportMarketDataBean#length(3) ";
            env.compileDeploy(epl).addListener("s0");

            // assert select result type
            Assert.assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
            Assert.assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("volSum"));

            sendEvent(env, SYMBOL_DELL, 10000);
            assertEvents(env, SYMBOL_DELL, 10000);

            sendEvent(env, SYMBOL_DELL, 10000);
            assertEvents(env, SYMBOL_DELL, 10000);       // still 10k since summing distinct volumes

            env.milestone(0);

            sendEvent(env, SYMBOL_DELL, 20000);
            assertEvents(env, SYMBOL_DELL, 30000);

            sendEvent(env, SYMBOL_IBM, 1000);
            assertEvents(env, SYMBOL_DELL, 31000, SYMBOL_IBM, 31000);

            sendEvent(env, SYMBOL_IBM, 1000);
            assertEvents(env, SYMBOL_DELL, 21000, SYMBOL_IBM, 21000);

            sendEvent(env, SYMBOL_IBM, 1000);
            assertEvents(env, SYMBOL_DELL, 1000, SYMBOL_IBM, 1000);

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowPerEventDistinctNullable implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream " +
                "avg(distinct volume) as avgVolume, count(distinct symbol) as countDistinctSymbol " +
                "from SupportMarketDataBean";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent(100L, "ONE"));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"avgVolume", 100d},
                    {"countDistinctSymbol", 1L},
                },
                new Object[][]{
                    {"avgVolume", null},
                    {"countDistinctSymbol", 0L},
                });

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent(null, null));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"avgVolume", 100d},
                    {"countDistinctSymbol", 1L},
                },
                new Object[][]{
                    {"avgVolume", 100d},
                    {"countDistinctSymbol", 1L},
                });

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent(null, "Two"));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"avgVolume", 100d},
                    {"countDistinctSymbol", 2L},
                },
                new Object[][]{
                    {"avgVolume", 100d},
                    {"countDistinctSymbol", 1L},
                });

            env.milestone(3);

            env.undeployAll();
        }
    }

    private static SupportMarketDataBean makeMarketDataEvent(Long volume, String symbol) {
        return new SupportMarketDataBean(symbol, 0, volume, null);
    }

    private static void assertPostedNew(RegressionEnvironment env, Double newAvg, Long newSum) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        Assert.assertEquals("IBM stats", newData[0].get("title"));
        Assert.assertEquals(newAvg, newData[0].get("myAvg"));
        Assert.assertEquals(newSum, newData[0].get("mySum"));

        env.listener("s0").reset();
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed, AtomicInteger eventCount) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        bean.setLongPrimitive(eventCount.incrementAndGet());
        env.sendEventBean(bean);
    }

    private static void sendMarketDataEvent(RegressionEnvironment env, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, AtomicInteger eventCount, long longBoxed) {
        sendEvent(env, longBoxed, 0, (short) 0, eventCount);
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, int intBoxed) {
        SupportBean theEvent = new SupportBean(theString, intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        env.sendEventBean(theEvent);
    }

    private static void tryAssert(RegressionEnvironment env) {
        String[] fields = new String[]{"longPrimitive", "mySum"};

        // assert select result type
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);
        AtomicInteger eventCount = new AtomicInteger();

        sendEvent(env, eventCount, 10);
        assertEquals(10L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{1L, 10L}});

        env.milestone(1);

        sendEvent(env, eventCount, 15);
        assertEquals(25L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{1L, 25L}, {2L, 25L}});

        env.milestone(2);

        sendEvent(env, eventCount, -5);
        assertEquals(20L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        assertNull(env.listener("s0").getLastOldData());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{1L, 20L}, {2L, 20L}, {3L, 20L}});

        env.milestone(3);

        sendEvent(env, eventCount, -2);
        assertEquals(8L, env.listener("s0").getLastOldData()[0].get("mySum"));
        assertEquals(8L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{4L, 8L}, {2L, 8L}, {3L, 8L}});

        env.milestone(4);

        sendEvent(env, eventCount, 100);
        assertEquals(93L, env.listener("s0").getLastOldData()[0].get("mySum"));
        assertEquals(93L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{4L, 93L}, {5L, 93L}, {3L, 93L}});

        env.milestone(5);

        sendEvent(env, eventCount, 1000);
        assertEquals(1098L, env.listener("s0").getLastOldData()[0].get("mySum"));
        assertEquals(1098L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{4L, 1098L}, {5L, 1098L}, {6L, 1098L}});
    }

    private static void assertEvents(RegressionEnvironment env, String symbol, long volSum) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        Assert.assertEquals(symbol, newData[0].get("symbol"));
        Assert.assertEquals(volSum, newData[0].get("volSum"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void assertEvents(RegressionEnvironment env, String symbolOld, long volSumOld,
                                     String symbolNew, long volSumNew) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        Assert.assertEquals(symbolOld, oldData[0].get("symbol"));
        Assert.assertEquals(volSumOld, oldData[0].get("volSum"));

        Assert.assertEquals(symbolNew, newData[0].get("symbol"));
        Assert.assertEquals(volSumNew, newData[0].get("volSum"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        env.sendEventBean(bean);
    }
}
