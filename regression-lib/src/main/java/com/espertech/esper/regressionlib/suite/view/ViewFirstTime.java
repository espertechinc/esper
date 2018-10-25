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
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ViewFirstTime {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewFirstTimeSimple());
        execs.add(new ViewFirstTimeSceneOne());
        execs.add(new ViewFirstTimeSceneTwo());
        return execs;
    }

    public static class ViewFirstTimeSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");
            env.advanceTime(0);
            String epl = "@Name('s0') select irstream theString as c0, intPrimitive as c1 from SupportBean#firsttime(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}});
            env.advanceTime(2000);
            sendSupportBean(env, "E2", 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

            env.milestone(3);

            env.advanceTime(9999);

            env.milestone(4);

            env.advanceTime(10000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}, {"E2", 20}});
            sendSupportBean(env, "E3", 30);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            sendSupportBean(env, "E4", 40);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}, {"E2", 20}});

            env.undeployAll();
        }
    }

    public static class ViewFirstTimeSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String text = "@name('s0') select irstream * from SupportMarketDataBean#firsttime(1 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.advanceTime(500);
            env.sendEventBean(makeMarketDataEvent("E1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E1"}}, null);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"symbol"}, new Object[][]{{"E1"}});

            env.milestone(1);

            env.advanceTime(600);
            env.sendEventBean(makeMarketDataEvent("E2"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E2"}}, null);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(2);

            env.advanceTime(1500);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.advanceTime(1600);
            env.sendEventBean(makeMarketDataEvent("E3"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            env.advanceTime(2000);
            env.sendEventBean(makeMarketDataEvent("E4"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            env.undeployAll();
        }
    }

    private static class ViewFirstTimeSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            env.compileDeployAddListenerMileZero("@name('s0') select * from SupportBean#firsttime(1 month)", "s0");

            sendCurrentTime(env, "2002-02-15T09:00:00.000");
            env.sendEventBean(new SupportBean("E1", 1));

            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.sendEventBean(new SupportBean("E2", 2));

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            env.sendEventBean(new SupportBean("E3", 3));

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}
