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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ViewTimeAccum {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewTimeAccumSceneOne());
        execs.add(new ViewTimeAccumSceneTwo());
        execs.add(new ViewTimeAccumSceneThree());
        execs.add(new ViewTimeAccumRStream());
        execs.add(new ViewTimeAccumPreviousAndPriorSceneOne());
        execs.add(new ViewTimeAccumPreviousAndPriorSceneTwo());
        execs.add(new ViewTimeAccumMonthScoped());
        execs.add(new ViewTimeAccumSum());
        execs.add(new ViewTimeAccumGroupedWindow());
        return execs;
    }

    private static class ViewTimeAccumMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            String epl = "@name('s0') select rstream * from SupportBean#time_accum(1 month)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));

            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.assertListenerNotInvoked("s0");

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            env.assertPropsPerRowLastNew("s0", "theString".split(","), new Object[][]{{"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    private static class ViewTimeAccumSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select irstream * from SupportMarketDataBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendTimer(env, startTime + 10000);
            env.assertListenerNotInvoked("s0");

            // 1st at 10 sec
            env.sendEventBean(events[0]);
            assertUnderlying(env, events[0]);

            // 2nd event at 14 sec
            sendTimer(env, startTime + 14000);
            env.sendEventBean(events[1]);
            assertUnderlying(env, events[1]);

            // 3nd event at 14 sec
            sendTimer(env, startTime + 14000);
            env.sendEventBean(events[2]);
            assertUnderlying(env, events[2]);

            // 3rd event at 23 sec
            sendTimer(env, startTime + 23000);
            env.sendEventBean(events[3]);
            assertUnderlying(env, events[3]);

            // no event till 33 sec
            sendTimer(env, startTime + 32999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, startTime + 33000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getOldDataList().size());
                assertEquals(4, listener.getLastOldData().length);
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2], events[3]}, listener.getOldDataListFlattened());
                listener.reset();
            });

            // no events till 50 sec
            sendTimer(env, startTime + 50000);
            env.assertListenerNotInvoked("s0");

            // next two events at 55 sec
            sendTimer(env, startTime + 55000);
            env.sendEventBean(events[4]);
            assertUnderlying(env, events[4]);
            env.sendEventBean(events[5]);
            assertUnderlying(env, events[5]);

            // no event till 65 sec
            sendTimer(env, startTime + 64999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, startTime + 65000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getOldDataList().size());
                assertEquals(2, listener.getLastOldData().length);
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[4], events[5]}, listener.getOldDataListFlattened());
                listener.reset();
            });

            // next window
            env.sendEventBean(events[6]);
            assertUnderlying(env, events[6]);

            sendTimer(env, startTime + 74999);
            env.sendEventBean(events[7]);
            assertUnderlying(env, events[7]);

            sendTimer(env, startTime + 74999 + 10000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getOldDataList().size());
                assertEquals(2, listener.getLastOldData().length);
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6], events[7]}, listener.getOldDataListFlattened());
                listener.reset();
            });

            env.undeployAll();
        }
    }

    public static class ViewTimeAccumSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);
            String[] fields = "symbol".split(",");

            String text = "@name('s0') select irstream * from SupportMarketDataBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");
            env.assertPropsPerRowIterator("s0", fields, null);

            // 1st event
            sendTimer(env, 1000);
            sendEvent(env, "E1");
            env.assertEqualsNew("s0", "symbol", "E1");

            env.milestone(1);

            // 2nd event
            sendTimer(env, 5000);
            sendEvent(env, "E2");
            env.assertEqualsNew("s0", "symbol", "E2");

            env.milestone(2);

            sendTimer(env, 14999);
            env.assertListenerNotInvoked("s0");

            // Window pushes out events
            sendTimer(env, 15000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(3);

            // No events for a while
            sendTimer(env, 30000);
            env.assertListenerNotInvoked("s0");

            env.milestone(4);

            // 3rd and 4th event
            sendTimer(env, 31000);
            sendEvent(env, "E3");
            env.assertEqualsNew("s0", "symbol", "E3");

            env.milestone(5);

            sendTimer(env, 31000);
            sendEvent(env, "E4");
            env.assertEqualsNew("s0", "symbol", "E4");

            // Window pushes out events
            env.milestone(6);

            sendTimer(env, 40999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 41000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E3"}, {"E4"}});

            // 5th event
            sendEvent(env, "E5");
            env.assertEqualsNew("s0", "symbol", "E5");

            env.milestone(7);

            // 6th and 7th event
            sendTimer(env, 41000);
            sendEvent(env, "E6");
            env.assertEqualsNew("s0", "symbol", "E6");

            sendTimer(env, 49000);
            sendEvent(env, "E7");
            env.assertEqualsNew("s0", "symbol", "E7");

            env.milestone(8);

            sendTimer(env, 59000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E5"}, {"E6"}, {"E7"}});

            env.undeployAll();
        }
    }

    public static class ViewTimeAccumSceneThree implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            sendTimer(env, 1000);
            String epl = "@Name('s0') select irstream * from SupportBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.assertPropsPerRowIterator("s0", fields, null);

            sendSupportBean(env, "E1");
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(1);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}});

            sendTimer(env, 5000);
            sendSupportBean(env, "E2");
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(2);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}, {"E2"}});
            sendTimer(env, 14999);
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            sendTimer(env, 15000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(4);

            sendTimer(env, 18000);
            sendSupportBean(env, "E3");
            env.assertPropsNew("s0", fields, new Object[]{"E3"});
            sendSupportBean(env, "E4");
            env.assertPropsNew("s0", fields, new Object[]{"E4"});

            env.milestone(5);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E3"}, {"E4"}});
            sendTimer(env, 19000);
            sendSupportBean(env, "E5");
            env.assertPropsNew("s0", fields, new Object[]{"E5"});

            env.milestone(6);

            sendTimer(env, 28999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 29000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E3"}, {"E4"}, {"E5"}});

            env.milestone(7);

            sendTimer(env, 39000);
            sendTimer(env, 99000);
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewTimeAccumRStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select rstream * from SupportMarketDataBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendTimer(env, startTime + 10000);
            env.assertListenerNotInvoked("s0");

            // some events at 10 sec
            env.sendEventBean(events[0]);
            env.sendEventBean(events[1]);
            env.sendEventBean(events[2]);
            env.assertListenerNotInvoked("s0");

            // flush out of the window
            sendTimer(env, startTime + 20000);
            env.assertListener("s0", listener -> {
                assertEquals(1, listener.getNewDataList().size());
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, listener.getNewDataListFlattened());
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class ViewTimeAccumPreviousAndPriorSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select irstream price, prev(1, price) as prevPrice, prior(1, price) as priorPrice " +
                "from SupportMarketDataBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // 1st event
            sendTimer(env, startTime + 20000);
            env.sendEventBean(events[5]);
            env.assertEventNew("s0", event -> assertData(event, 5d, null, null));

            // 2nd event
            sendTimer(env, startTime + 25000);
            env.sendEventBean(events[6]);
            env.assertEventNew("s0", event -> assertData(event, 6d, 5d, 5d));

            // 3nd event
            sendTimer(env, startTime + 34000);
            env.sendEventBean(events[7]);
            env.assertEventNew("s0", event -> assertData(event, 7d, 6d, 6d));

            sendTimer(env, startTime + 43999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, startTime + 44000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getOldDataList().size());
                assertEquals(3, listener.getLastOldData().length);
                assertData(listener.getLastOldData()[0], 5d, null, null);
                assertData(listener.getLastOldData()[1], 6d, null, 5d);
                assertData(listener.getLastOldData()[2], 7d, null, 6d);
                listener.reset();
            });
            
            env.undeployAll();
        }
    }

    public static class ViewTimeAccumPreviousAndPriorSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String text = "@name('s0') select irstream price, " +
                "prev(1, price) as prevPrice, " +
                "prior(1, price) as priorPrice, " +
                "prevtail(price) as prevtailPrice, " +
                "prevcount(price) as prevCountPrice, " +
                "prevwindow(price) as prevWindowPrice " +
                "from SupportMarketDataBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");

            // 1st event S1 group
            sendTimer(env, 1000);
            sendEvent(env, "S1", 10);
            env.assertEventNew("s0", event -> assertData(event, 10d, null, null, 10d, 1L, new Object[]{10d}));

            env.milestone(1);

            // 2nd event S1 group
            sendTimer(env, 5000);
            sendEvent(env, "S1", 20);
            env.assertEventNew("s0", event -> assertData(event, 20d, 10d, 10d, 10d, 2L, new Object[]{20d, 10d}));

            env.milestone(2);

            // 1st event S2 group
            sendTimer(env, 10000);
            sendEvent(env, "S2", 30);
            env.assertEventNew("s0", event -> assertData(event, 30d, 20d, 20d, 10d, 3L, new Object[]{30d, 20d, 10d}));

            env.milestone(3);

            sendTimer(env, 20000);
            env.assertListener("s0", listener -> {
                assertNull(null, listener.getLastNewData());
                EventBean[] oldData = listener.getLastOldData();
                assertData(oldData[0], 10d, null, null, null, null, null);
                assertData(oldData[1], 20d, null, 10d, null, null, null);
                assertData(oldData[2], 30d, null, 20d, null, null, null);
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class ViewTimeAccumSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select irstream sum(price) as sumPrice from SupportMarketDataBean#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // 1st event
            sendTimer(env, startTime + 20000);
            env.sendEventBean(events[5]);
            env.assertListener("s0", listener -> {
                assertData(listener.getLastNewData()[0], 5d);
                assertData(listener.getLastOldData()[0], null);
                listener.reset();
            });

            // 2nd event
            sendTimer(env, startTime + 25000);
            env.sendEventBean(events[6]);
            env.assertListener("s0", listener -> {
                assertData(listener.getLastNewData()[0], 11d);
                assertData(listener.getLastOldData()[0], 5d);
                listener.reset();
            });

            sendTimer(env, startTime + 34999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, startTime + 35000);
            env.assertListener("s0", listener -> {
                assertData(listener.getLastNewData()[0], null);
                assertData(listener.getLastOldData()[0], 11d);
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class ViewTimeAccumGroupedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select irstream * from SupportMarketDataBean#groupwin(symbol)#time_accum(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // 1st S1 event
            sendTimer(env, startTime + 10000);
            env.sendEventBean(events[1]);
            assertUnderlying(env, events[1]);

            // 1st S2 event
            sendTimer(env, startTime + 12000);
            env.sendEventBean(events[2]);
            assertUnderlying(env, events[2]);

            // 2nd S1 event
            sendTimer(env, startTime + 15000);
            env.sendEventBean(events[11]);
            assertUnderlying(env, events[11]);

            // 2nd S2 event
            sendTimer(env, startTime + 18000);
            env.sendEventBean(events[12]);
            assertUnderlying(env, events[12]);

            // 3rd S1 event
            sendTimer(env, startTime + 21000);
            env.sendEventBean(events[21]);
            assertUnderlying(env, events[21]);

            sendTimer(env, startTime + 28000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getOldDataList().size());
                assertEquals(2, listener.getLastOldData().length);
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[2], events[12]}, listener.getOldDataListFlattened());
                listener.reset();
            });

            // 3rd S2 event
            sendTimer(env, startTime + 29000);
            env.sendEventBean(events[32]);
            assertUnderlying(env, events[32]);

            sendTimer(env, startTime + 31000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getOldDataList().size());
                assertEquals(3, listener.getLastOldData().length);
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[1], events[11], events[21]}, listener.getOldDataListFlattened());
                listener.reset();
            });

            sendTimer(env, startTime + 39000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                assertEquals(1, listener.getLastOldData().length);
                EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[32]}, listener.getOldDataListFlattened());
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void assertData(EventBean theEvent, double price, Double prevPrice, Double priorPrice) {
        assertEquals(price, theEvent.get("price"));
        assertEquals(prevPrice, theEvent.get("prevPrice"));
        assertEquals(priorPrice, theEvent.get("priorPrice"));
    }

    private static void assertData(EventBean theEvent, Double sumPrice) {
        assertEquals(sumPrice, theEvent.get("sumPrice"));
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static SupportMarketDataBean[] get100Events() {
        SupportMarketDataBean[] events = new SupportMarketDataBean[100];
        for (int i = 0; i < events.length; i++) {
            int group = i % 10;
            events[i] = new SupportMarketDataBean("S" + Integer.toString(group), "id_" + Integer.toString(i), i);
        }
        return events;
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }

    private static SupportMarketDataBean sendEvent(RegressionEnvironment env, String symbol) {
        return sendEvent(env, symbol, 0);
    }

    private static SupportMarketDataBean sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
        return bean;
    }

    private static void assertData(EventBean event, double price, Double prevPrice, Double priorPrice, Double prevtailPrice, Long prevCountPrice, Object[] prevWindowPrice) {
        assertEquals(price, event.get("price"));
        assertEquals(prevPrice, event.get("prevPrice"));
        assertEquals(priorPrice, event.get("priorPrice"));
        assertEquals(prevtailPrice, event.get("prevtailPrice"));
        assertEquals(prevCountPrice, event.get("prevCountPrice"));
        EPAssertionUtil.assertEqualsExactOrder(prevWindowPrice, (Object[]) event.get("prevWindowPrice"));
    }

    private static void assertUnderlying(RegressionEnvironment env, Object underlying) {
        env.assertListener("s0", listener -> {
            assertEquals(listener.assertOneGetNewAndReset().getUnderlying(), underlying);
        });
    }
}
