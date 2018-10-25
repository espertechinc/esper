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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ViewFirstEvent {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewFirstEvent.ViewFirstEventSceneOne());
        execs.add(new ViewFirstEvent.ViewFirstEventMarketData());
        return execs;
    }

    public static class ViewFirstEventSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@Name('s0') select irstream theString as c0, intPrimitive as c1 from SupportBean#firstevent()";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}});
            sendSupportBean(env, "E2", 2);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}});
            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    public static class ViewFirstEventMarketData implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#firstevent()";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E1"}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2"));
            assertFalse(env.listener("s0").isInvoked());

            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"symbol"}, new Object[][]{{"E1"}});

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("E3"));
            assertFalse(env.listener("s0").isInvoked());
            events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"symbol"}, new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive) {
        env.sendEventBean(new SupportBean(string, intPrimitive));
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }
}
