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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportEventIdWithTimestamp;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class ViewExternallyTimedBatched {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewExternallyTimedBatchSceneOne());
        execs.add(new ViewExternallyTimedBatchedNoReference());
        execs.add(new ViewExternallyTimedBatchedWithRefTime());
        execs.add(new ViewExternallyTimedBatchRefWithPrev());
        execs.add(new ViewExternallyTimedBatchMonthScoped());
        return execs;
    }

    public static class ViewExternallyTimedBatchSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");

            String epl = "@Name('s0') select irstream theString as c0 from SupportBean#ext_timed_batch(longPrimitive, 10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBeanWLong(env, "E1", 1000);   // reference point is 1000, every batch is 11000/21000/31000...
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBeanWLong(env, "E2", 5000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            sendSupportBeanWLong(env, "E3", 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E1"}, {"E2"}}, null);

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E3"}});
            sendSupportBeanWLong(env, "E4", 0);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E3"}, {"E4"}});
            sendSupportBeanWLong(env, "E5", 21000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E3"}, {"E4"}}, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(5);
            env.milestone(6);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E5"}});
            sendSupportBeanWLong(env, "E6", 31000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E5"}}, new Object[][]{{"E3"}, {"E4"}});
            sendSupportBeanWLong(env, "E7", 41000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E6"}}, new Object[][]{{"E5"}});

            env.undeployAll();
        }
    }

    private static class ViewExternallyTimedBatchedNoReference implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "id".split(",");
            String epl = "@name('s0') select irstream * from SupportEventIdWithTimestamp#ext_timed_batch(mytimestamp, 1 minute)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E1", "8:00:00.000"));
            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E2", "8:00:30.000"));

            env.milestone(1);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E3", "8:00:59.999"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E4", "8:01:00.000"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E1"}, {"E2"}, {"E3"}}, (Object[][]) null);

            env.milestone(3);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E5", "8:01:02.000"));
            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E6", "8:01:05.000"));

            env.milestone(4);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E7", "8:02:00.000"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(5);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E8", "8:03:59.000"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E7"}}, new Object[][]{{"E4"}, {"E5"}, {"E6"}});

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E9", "8:03:59.000"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E10", "8:04:00.000"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E8"}, {"E9"}}, new Object[][]{{"E7"}});

            env.milestone(7);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E11", "8:06:30.000"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E10"}}, new Object[][]{{"E8"}, {"E9"}});

            env.milestone(8);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E12", "8:06:59.999"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(9);

            env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E13", "8:07:00.001"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{{"E11"}, {"E12"}}, new Object[][]{{"E10"}});

            env.undeployAll();
        }
    }

    private static class ViewExternallyTimedBatchedWithRefTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select irstream * from SupportEventIdWithTimestamp#ext_timed_batch(mytimestamp, 1 minute, 5000)";
            tryAssertionWithRefTime(env, epl, milestone);

            epl = "@name('s0') select irstream * from SupportEventIdWithTimestamp#ext_timed_batch(mytimestamp, 1 minute, 65000)";
            tryAssertionWithRefTime(env, epl, milestone);
        }
    }

    public static class ViewExternallyTimedBatchRefWithPrev implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "currSymbol,prev0Symbol,prev0Price,prev1Symbol,prev1Price,prev2Symbol,prev2Price,prevTail0Symbol,prevTail0Price,prevTail1Symbol,prevTail1Price,prevCountPrice,prevWindowPrice".split(",");
            String epl = "@Name('s0') select irstream symbol as currSymbol, " +
                "prev(0, symbol) as prev0Symbol, " +
                "prev(0, price) as prev0Price, " +
                "prev(1, symbol) as prev1Symbol, " +
                "prev(1, price) as prev1Price, " +
                "prev(2, symbol) as prev2Symbol, " +
                "prev(2, price) as prev2Price," +
                "prevtail(0, symbol) as prevTail0Symbol, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, symbol) as prevTail1Symbol, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as prevCountPrice, " +
                "prevwindow(price) as prevWindowPrice " +
                "from SupportMarketDataBean#ext_timed_batch(volume, 10, 0L) ";
            env.compileDeploy(epl).addListener("s0");

            sendMarketEvent(env, "A", 1, 1000);

            env.milestone(0);

            sendMarketEvent(env, "B", 2, 1001);

            env.milestone(1);

            sendMarketEvent(env, "C", 3, 1002);

            env.milestone(2);

            sendMarketEvent(env, "D", 4, 10000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{
                    {"A", "A", 1d, null, null, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")},
                    {"B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")},
                    {"C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")}
                },
                null);

            env.milestone(3);

            sendMarketEvent(env, "E", 5, 20000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
                new Object[][]{
                    {"D", "D", 4d, null, null, null, null, "D", 4d, null, null, 1L, splitDoubles("4d")},
                },
                new Object[][]{
                    {"A", null, null, null, null, null, null, null, null, null, null, null, null},
                    {"B", null, null, null, null, null, null, null, null, null, null, null, null},
                    {"C", null, null, null, null, null, null, null, null, null, null, null, null},
                }
            );

            env.undeployAll();
        }
    }

    private static void tryAssertionWithRefTime(RegressionEnvironment env, String epl, AtomicInteger milestone) {
        String[] fields = "id".split(",");
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E1", "8:00:00.000"));
        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E2", "8:00:04.999"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E3", "8:00:05.000"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
            new Object[][]{{"E1"}, {"E2"}}, null);

        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E4", "8:00:04.000"));
        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E5", "7:00:00.000"));
        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E6", "8:01:04.999"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E7", "8:01:05.000"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
            new Object[][]{{"E3"}, {"E4"}, {"E5"}, {"E6"}}, new Object[][]{{"E1"}, {"E2"}});

        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E8", "8:03:55.000"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
            new Object[][]{{"E7"}}, new Object[][]{{"E3"}, {"E4"}, {"E5"}, {"E6"}});

        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E9", "0:00:00.000"));
        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E10", "8:04:04.999"));
        env.sendEventBean(SupportEventIdWithTimestamp.makeTime("E11", "8:04:05.000"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), fields,
            new Object[][]{{"E8"}, {"E9"}, {"E10"}}, new Object[][]{{"E7"}});

        env.undeployAll();
    }

    private static class ViewExternallyTimedBatchMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean#ext_timed_batch(longPrimitive, 1 month)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendExtTimeEvent(env, DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"), "E1");
            sendExtTimeEvent(env, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000") - 1, "E2");
            assertFalse(env.listener("s0").isInvoked());

            sendExtTimeEvent(env, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000"), "E3");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    private static void sendMarketEvent(RegressionEnvironment env, String symbol, double price, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }

    private static Object[] splitDoubles(String doubleList) {
        String[] doubles = doubleList.split(",");
        Object[] result = new Object[doubles.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Double.parseDouble(doubles[i]);
        }
        return result;
    }

    private static void sendSupportBeanWLong(RegressionEnvironment env, String string, long longPrimitive) {
        SupportBean sb = new SupportBean(string, 0);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBean(sb);
    }

    private static void sendExtTimeEvent(RegressionEnvironment env, long longPrimitive, String theString) {
        SupportBean theEvent = new SupportBean(theString, 0);
        theEvent.setLongPrimitive(longPrimitive);
        env.sendEventBean(theEvent);
    }
}