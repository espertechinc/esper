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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResultSetQueryTypeRowForAll {
    private final static String JOIN_KEY = "KEY";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeRowForAllSimple());
        execs.add(new ResultSetQueryTypeRowForAllSumMinMax());
        execs.add(new ResultSetQueryTypeRowForAllWWindowAgg());
        execs.add(new ResultSetQueryTypeRowForAllMinMaxWindowed());
        execs.add(new ResultSetQueryTypeSumOneView());
        execs.add(new ResultSetQueryTypeSumJoin());
        execs.add(new ResultSetQueryTypeAvgPerSym());
        execs.add(new ResultSetQueryTypeSelectStarStdGroupBy());
        execs.add(new ResultSetQueryTypeSelectExprGroupWin());
        execs.add(new ResultSetQueryTypeSelectAvgExprStdGroupBy());
        execs.add(new ResultSetQueryTypeSelectAvgStdGroupByUni());
        execs.add(new ResultSetQueryTypeRowForAllNamedWindowWindow());
        return execs;
    }

    public static class ResultSetQueryTypeRowForAllSumMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3".split(",");

            env.milestone(0);
            env.advanceTime(0);
            String epl = "@Name('s0') select theString as c0, sum(intPrimitive) as c1," +
                "min(intPrimitive) as c2, max(intPrimitive) as c3 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(1);

            sendEventSB(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 10, 10});

            env.milestone(2);

            sendEventSB(env, "E2", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 10 + 100, 10, 100});

            env.milestone(3);

            sendEventSB(env, "E3", 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 10 + 100 + 11, 10, 100});

            env.milestone(4);

            env.milestone(5);

            sendEventSB(env, "E4", 9);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", 10 + 100 + 11 + 9, 9, 100});

            sendEventSB(env, "E5", 120);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", 10 + 100 + 11 + 9 + 120, 9, 120});

            sendEventSB(env, "E6", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6", 10 + 100 + 11 + 9 + 120 + 100, 9, 120});

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowForAllWWindowAgg implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0') select irstream theString as c0, sum(intPrimitive) as c1," +
                "window(*) as c2 from SupportBean.win:length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            Object e1 = sendSupportBean(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, new Object[]{e1}});

            env.milestone(1);

            Object e2 = sendSupportBean(env, "E2", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 10 + 100, new Object[]{e1, e2}});

            env.milestone(2);

            Object e3 = sendSupportBean(env, "E3", 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E3", 100 + 11, new Object[]{e2, e3}}, new Object[]{"E1", 100 + 11, new Object[]{e2, e3}});

            env.milestone(3);

            env.milestone(4);

            Object e4 = sendSupportBean(env, "E4", 9);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E4", 11 + 9, new Object[]{e3, e4}}, new Object[]{"E2", 11 + 9, new Object[]{e3, e4}});

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowForAllSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream " +
                "avg(price) as avgPrice," +
                "sum(price) as sumPrice," +
                "min(price) as minPrice," +
                "max(price) as maxPrice," +
                "median(price) as medianPrice," +
                "stddev(price) as stddevPrice," +
                "avedev(price) as avedevPrice," +
                "count(*) as datacount, " +
                "count(distinct price) as countDistinctPrice " +
                "from SupportMarketDataBean";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent(100));

            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"avgPrice", 100d},
                    {"sumPrice", 100d},
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                    {"medianPrice", 100d},
                    {"stddevPrice", null},
                    {"avedevPrice", 0.0},
                    {"datacount", 1L},
                    {"countDistinctPrice", 1L},
                }, // new data
                new Object[][]{
                    {"avgPrice", null},
                    {"sumPrice", null},
                    {"minPrice", null},
                    {"maxPrice", null},
                    {"medianPrice", null},
                    {"stddevPrice", null},
                    {"avedevPrice", null},
                    {"datacount", 0L},
                    {"countDistinctPrice", 0L},
                } // old data
            );

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent(200));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"avgPrice", (100 + 200) / 2.0},
                    {"sumPrice", 100 + 200d},
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                    {"medianPrice", 150d},
                    {"stddevPrice", 70.71067811865476},
                    {"avedevPrice", 50d},
                    {"datacount", 2L},
                    {"countDistinctPrice", 2L},
                }, // new data
                new Object[][]{
                    {"avgPrice", 100d},
                    {"sumPrice", 100d},
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                    {"medianPrice", 100d},
                    {"stddevPrice", null},
                    {"avedevPrice", 0.0},
                    {"datacount", 1L},
                    {"countDistinctPrice", 1L},
                } // old data
            );

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent(150));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"avgPrice", (150 + 100 + 200) / 3.0},
                    {"sumPrice", 150 + 100 + 200d},
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                    {"medianPrice", 150d},
                    {"stddevPrice", 50d},
                    {"avedevPrice", 33 + 1 / 3d},
                    {"datacount", 3L},
                    {"countDistinctPrice", 3L},
                }, // new data
                new Object[][]{
                    {"avgPrice", (100 + 200) / 2.0},
                    {"sumPrice", 100 + 200d},
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                    {"medianPrice", 150d},
                    {"stddevPrice", 70.71067811865476},
                    {"avedevPrice", 50d},
                    {"datacount", 2L},
                    {"countDistinctPrice", 2L},
                } // old data
            );

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowForAllMinMaxWindowed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream " +
                "min(price) as minPrice," +
                "max(price) as maxPrice " +
                "from  SupportMarketDataBean#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent(100));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                }, // new data
                new Object[][]{
                    {"minPrice", null},
                    {"maxPrice", null},
                } // old data
            );

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent(200));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                }, // new data
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                } // old data
            );

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent(150));
            env.listener("s0").assertNewOldData(
                new Object[][]{
                    {"minPrice", 150d},
                    {"maxPrice", 200d},
                }, // new data
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                } // old data
            );

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBean#time(10 sec)";
            env.compileDeploy(epl).addListener("s0");

            sendTimerEvent(env, 0);

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBeanString#keepall as one, " +
                "SupportBean#time(10 sec) as two " +
                "where one.theString = two.theString";
            env.compileDeploy(epl).addListener("s0");

            sendTimerEvent(env, 0);

            env.sendEventBean(new SupportBeanString(JOIN_KEY));

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAvgPerSym implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream avg(price) as avgp, sym " +
                "from SupportPriceEvent#groupwin(sym)#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportPriceEvent(1, "A"));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("A", theEvent.get("sym"));
            Assert.assertEquals(1.0, theEvent.get("avgp"));

            env.sendEventBean(new SupportPriceEvent(2, "B"));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("B", theEvent.get("sym"));
            Assert.assertEquals(1.5, theEvent.get("avgp"));

            env.milestone(0);

            env.sendEventBean(new SupportPriceEvent(9, "A"));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("A", theEvent.get("sym"));
            Assert.assertEquals((1 + 2 + 9) / 3.0, theEvent.get("avgp"));

            env.sendEventBean(new SupportPriceEvent(18, "B"));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("B", theEvent.get("sym"));
            Assert.assertEquals((1 + 2 + 9 + 18) / 4.0, theEvent.get("avgp"));

            env.sendEventBean(new SupportPriceEvent(5, "A"));
            theEvent = env.listener("s0").getLastNewData()[0];
            Assert.assertEquals("A", theEvent.get("sym"));
            Assert.assertEquals((2 + 9 + 18 + 5) / 4.0, theEvent.get("avgp"));
            theEvent = env.listener("s0").getLastOldData()[0];
            Assert.assertEquals("A", theEvent.get("sym"));
            Assert.assertEquals((5 + 2 + 9 + 18) / 4.0, theEvent.get("avgp"));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSelectStarStdGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream * from SupportMarketDataBean#groupwin(symbol)#length(2)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1.0, env.listener("s0").getLastNewData()[0].get("price"));
            assertTrue(env.listener("s0").getLastNewData()[0].getUnderlying() instanceof SupportMarketDataBean);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSelectExprGroupWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream price from SupportMarketDataBean#groupwin(symbol)#length(2)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1.0, env.listener("s0").getLastNewData()[0].get("price"));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSelectAvgExprStdGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream avg(price) as aprice from SupportMarketDataBean"
                + "#groupwin(symbol)#length(2)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1.0, env.listener("s0").getLastNewData()[0].get("aprice"));

            env.milestone(0);

            sendEvent(env, "B", 3);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(2.0, env.listener("s0").getLastNewData()[0].get("aprice"));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSelectAvgStdGroupByUni implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream average as aprice from SupportMarketDataBean"
                + "#groupwin(symbol)#length(2)#uni(price)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals(1.0, env.listener("s0").getLastNewData()[0].get("aprice"));

            env.milestone(0);

            sendEvent(env, "B", 3);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals(3.0, env.listener("s0").getLastNewData()[0].get("aprice"));
            sendEvent(env, "A", 3);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals(2.0, env.listener("s0").getLastNewData()[0].get("aprice"));

            env.milestone(1);

            sendEvent(env, "A", 10);
            sendEvent(env, "A", 20);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals(15.0, env.listener("s0").getLastNewData()[0].get("aprice"));

            env.undeployAll();
        }
    }

    private static void tryAssert(RegressionEnvironment env) {
        // assert select result type
        Assert.assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{null}});

        sendTimerEvent(env, 0);
        sendEvent(env, 10);
        Assert.assertEquals(10L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(env, 5000);
        sendEvent(env, 15);
        Assert.assertEquals(25L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{25L}});

        sendTimerEvent(env, 8000);
        sendEvent(env, -5);
        Assert.assertEquals(20L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        assertNull(env.listener("s0").getLastOldData());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{20L}});

        sendTimerEvent(env, 10000);
        Assert.assertEquals(20L, env.listener("s0").getLastOldData()[0].get("mySum"));
        Assert.assertEquals(10L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(env, 15000);
        Assert.assertEquals(10L, env.listener("s0").getLastOldData()[0].get("mySum"));
        Assert.assertEquals(-5L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{-5L}});

        sendTimerEvent(env, 18000);
        Assert.assertEquals(-5L, env.listener("s0").getLastOldData()[0].get("mySum"));
        assertNull(env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), new String[]{"mySum"}, new Object[][]{{null}});
    }

    public static class ResultSetQueryTypeRowForAllNamedWindowWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");
            String epl = "create window ABCWin.win:keepall() as SupportBean;\n" +
                "insert into ABCWin select * from SupportBean;\n" +
                "on SupportBean_A delete from ABCWin where theString = id;\n" +
                "@Name('s0') select irstream theString as c0, window(intPrimitive) as c1 from ABCWin;\n";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", new int[]{10}});

            env.milestone(1);

            sendSupportBean(env, "E2", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", new int[]{10, 100}});

            env.milestone(2);

            sendSupportBean_A(env, "E2");    // delete E2
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E2", new int[]{10}});

            env.milestone(3);

            sendSupportBean(env, "E3", 50);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", new int[]{10, 50}});

            env.milestone(4);

            env.milestone(5);  // no change

            sendSupportBean_A(env, "E1");    // delete E1
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E1", new int[]{50}});

            env.milestone(6);

            sendSupportBean(env, "E4", -1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", new int[]{50, -1}});

            env.undeployAll();
        }
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        env.sendEventBean(new SupportBean_A(id));
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        env.sendEventBean(sb);
        return sb;
    }

    private static Object sendEvent(RegressionEnvironment env, String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, null, null);
        env.sendEventBean(theEvent);
        return theEvent;
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

    private static SupportMarketDataBean makeMarketDataEvent(double price) {
        return new SupportMarketDataBean("DELL", price, 0L, null);
    }

    private static void sendEventSB(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendTimerEvent(RegressionEnvironment env, long msec) {
        env.advanceTime(msec);
    }
}
