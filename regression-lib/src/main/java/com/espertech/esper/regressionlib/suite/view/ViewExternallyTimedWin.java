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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ViewExternallyTimedWin {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewExternallyTimedWindowSceneOne());
        execs.add(new ViewExternallyTimedBatchSceneTwo());
        execs.add(new ViewExternallyTimedWinSceneShort());
        execs.add(new ViewExternallyTimedTimedMonthScoped());
        execs.add(new ViewExternallyTimedWindowPrev());
        return execs;
    }

    public static class ViewExternallyTimedWindowSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "symbol".split(",");
            String text = "@name('s0') select irstream * from  SupportMarketDataBean#ext_timed(volume, 1 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("E1", 500));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E1"}}, null);
            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2", 600));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E2"}}, null);
            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("E3", 1500));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E3"}}, new Object[][]{{"symbol", "E1"}});

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E4", 1600));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E4"}}, new Object[][]{{"symbol", "E2"}});

            env.milestone(4);

            env.sendEventBean(makeMarketDataEvent("E5", 1700));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E5"}}, null);

            env.milestone(5);

            env.sendEventBean(makeMarketDataEvent("E6", 1800));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E6"}}, null);

            env.milestone(6);

            env.sendEventBean(makeMarketDataEvent("E7", 1900));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E7"}}, null);

            env.milestone(7);

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}, {"E6"}, {"E7"}});

            env.sendEventBean(makeMarketDataEvent("E8", 2700));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getOldDataListFlattened(), fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields, new Object[][]{{"E8"}});
            env.listener("s0").reset();

            env.milestone(8);

            env.sendEventBean(makeMarketDataEvent("E9", 3700));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getOldDataListFlattened(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields, new Object[][]{{"E9"}});
            env.listener("s0").reset();

            env.milestone(9);

            env.undeployAll();
        }
    }

    public static class ViewExternallyTimedBatchSceneTwo implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");
            String epl = "@Name('s0') select irstream theString as c0 from SupportBean#ext_timed(longPrimitive, 10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBeanWLong(env, "E1", 1000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}});
            sendSupportBeanWLong(env, "E2", 5000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            sendSupportBeanWLong(env, "E3", 11000);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E3"}, new Object[]{"E1"});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}});
            sendSupportBeanWLong(env, "E4", 14000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            env.milestone(4);
            env.milestone(5);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}});
            sendSupportBeanWLong(env, "E5", 21000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastOldData(), fields, new Object[][]{{"E2"}, {"E3"}});
            sendSupportBeanWLong(env, "E6", 24000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E6"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastOldData(), fields, new Object[][]{{"E4"}});

            env.undeployAll();
        }
    }

    private static class ViewExternallyTimedWinSceneShort implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#ext_timed(longPrimitive, 10 minutes)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendExtTimeEvent(env, 0);

            sendExtTimeEvent(env, 10 * 60 * 1000 - 1);
            assertNull(env.listener("s0").getOldDataList().get(0));
            env.listener("s0").reset();

            sendExtTimeEvent(env, 10 * 60 * 1000 + 1);
            assertEquals(1, env.listener("s0").getOldDataList().get(0).length);

            env.undeployAll();
        }
    }

    private static class ViewExternallyTimedTimedMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select rstream * from SupportBean#ext_timed(longPrimitive, 1 month)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendExtTimeEvent(env, DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"), "E1");
            sendExtTimeEvent(env, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000") - 1, "E2");
            assertFalse(env.listener("s0").isInvoked());

            sendExtTimeEvent(env, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000"), "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

            env.undeployAll();
        }
    }

    public static class ViewExternallyTimedWindowPrev implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol," +
                "prev(1, symbol) as prev1, " +
                "prevtail(0, symbol) as prevTail0, " +
                "prevtail(1, symbol) as prevTail1, " +
                "prevcount(symbol) as prevCountSym, " +
                "prevwindow(symbol) as prevWindowSym " +
                "from SupportMarketDataBean#ext_timed(volume, 1 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");
            String[] fields = new String[]{"symbol", "prev1", "prevTail0", "prevTail1", "prevCountSym", "prevWindowSym"};

            env.sendEventBean(makeMarketDataEvent("E1", 500));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields,
                new Object[][]{{"E1", null, "E1", null, 1L, new Object[]{"E1"}}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2", 600));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields,
                new Object[][]{{"E2", "E1", "E1", "E2", 2L, new Object[]{"E2", "E1"}}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("E3", 1500));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields,
                new Object[][]{{"E3", "E2", "E2", "E3", 2L, new Object[]{"E3", "E2"}}});
            env.listener("s0").reset();

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E4", 1600));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), fields,
                new Object[][]{{"E4", "E3", "E3", "E4", 2L, new Object[]{"E4", "E3"}}});
            env.listener("s0").reset();

            env.milestone(4);

            env.undeployAll();
        }
    }

    private static void sendExtTimeEvent(RegressionEnvironment env, long longPrimitive) {
        SupportBean theEvent = new SupportBean(null, 0);
        theEvent.setLongPrimitive(longPrimitive);
        env.sendEventBean(theEvent);
    }

    private static void sendExtTimeEvent(RegressionEnvironment env, long longPrimitive, String name) {
        SupportBean theEvent = new SupportBean(name, 0);
        theEvent.setLongPrimitive(longPrimitive);
        env.sendEventBean(theEvent);
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, long volume) {
        return new SupportMarketDataBean(symbol, 0, volume, null);
    }

    private static void sendSupportBeanWLong(RegressionEnvironment env, String string, long longPrimitive) {
        SupportBean sb = new SupportBean(string, 0);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBean(sb);
    }
}
