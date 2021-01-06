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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

public class ViewFirstLength {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewFirstLengthSceneOne());
        execs.add(new ViewFirstLengthMarketData());
        return execs;
    }

    public static class ViewFirstLengthSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.milestone(0);

            String[] fields = "c0".split(",");
            String epl = "@Name('s0') select irstream theString as c0 from SupportBean#firstlength(2)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(1);

            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);
            sendSupportBean(env, "E1");
            env.assertPropsListenerNew("s0", fields, new Object[]{"E1"});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}});
            sendSupportBean(env, "E2");
            env.assertPropsListenerNew("s0", fields, new Object[]{"E2"});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            sendSupportBean(env, "E3");
            env.assertListenerNotInvoked("s0");

            env.milestone(4);

            sendSupportBean(env, "E4");
            env.assertListenerNotInvoked("s0");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    public static class ViewFirstLengthMarketData implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from  SupportMarketDataBean#firstlength(3)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.assertNVListener("s0", new Object[][]{{"symbol", "E1"}}, null);
            env.assertPropsPerRowIterator("s0", "symbol".split(","), new Object[][]{{"E1"}});

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2"));
            env.assertNVListener("s0", new Object[][]{{"symbol", "E2"}}, null);
            env.assertPropsPerRowIterator("s0", "symbol".split(","), new Object[][]{{"E1"}, {"E2"}});

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("E3"));
            env.assertNVListener("s0", new Object[][]{{"symbol", "E3"}}, null);
            env.assertPropsPerRowIterator("s0", "symbol".split(","), new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E4"));
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("s0", "symbol".split(","), new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.undeployAll();
        }
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static void sendSupportBean(RegressionEnvironment env, String string) {
        env.sendEventBean(new SupportBean(string, 0));
    }
}
