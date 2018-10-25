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

public class ViewLastEvent {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewLastEventSceneOne());
        execs.add(new ViewLastEventMarketData());
        return execs;
    }

    public static class ViewLastEventSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");

            String epl = "@Name('s0') select irstream theString as c0, intPrimitive as c1 from SupportBean#lastevent()";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}});
            sendSupportBean(env, "E2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E2", 2}, new Object[]{"E1", 1});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2", 2}});
            sendSupportBean(env, "E3", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E3", 3}, new Object[]{"E2", 2});

            env.milestone(4);
            env.milestone(5);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E3", 3}});
            sendSupportBean(env, "E4", 4);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E4", 4}, new Object[]{"E3", 3});

            env.undeployAll();
        }
    }

    public static class ViewLastEventMarketData implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from  SupportMarketDataBean#lastevent()";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E1"}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E2"}}, new Object[][]{{"symbol", "E1"}});

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"symbol"}, new Object[][]{{"E2"}});

            env.milestone(2);

            for (int i = 3; i < 10; i++) {
                env.sendEventBean(makeMarketDataEvent("E" + i));
                env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E" + i}}, // new data
                    new Object[][]{{"symbol", "E" + (i - 1)}} //  old data
                );

                env.milestone(i);
            }

            env.undeployAll();
        }
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive) {
        env.sendEventBean(new SupportBean(string, intPrimitive));
    }
}
