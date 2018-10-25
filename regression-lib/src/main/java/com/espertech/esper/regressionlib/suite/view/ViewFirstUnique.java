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

public class ViewFirstUnique {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewFirstUniqueSimple(null));
        execs.add(new ViewFirstUniqueSceneOne(null));
        return execs;
    }

    public static class ViewFirstUniqueSimple implements RegressionExecution {

        private final String optionalAnnotations;

        public ViewFirstUniqueSimple(String optionalAnnotations) {
            this.optionalAnnotations = optionalAnnotations;
        }

        public void run(RegressionEnvironment env) {

            env.milestone(0);

            String[] fields = "c0,c1".split(",");
            String epl = "@Name('s0') select irstream theString as c0, intPrimitive as c1 from SupportBean#firstunique(theString)";
            if (optionalAnnotations != null) {
                epl = optionalAnnotations + epl;
            }
            env.compileDeployAddListenerMile(epl, "s0", 1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}});
            sendSupportBean(env, "E2", 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}, {"E2", 20}});
            sendSupportBean(env, "E1", 2);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}, {"E2", 20}});
            sendSupportBean(env, "E2", 21);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);
            env.milestone(6);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 1}, {"E2", 20}});
            sendSupportBean(env, "E2", 22);
            sendSupportBean(env, "E1", 3);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "E3", 30);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 30});

            env.undeployAll();
        }
    }

    public static class ViewFirstUniqueSceneOne implements RegressionExecution {

        private final String optionalAnnotation;

        public ViewFirstUniqueSceneOne(String optionalAnnotation) {
            this.optionalAnnotation = optionalAnnotation;
        }

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol, price from SupportMarketDataBean#firstunique(symbol) order by symbol";
            if (optionalAnnotation != null) {
                text = optionalAnnotation + text;
            }
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("S1", 100));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S1"}, {"price", 100.0}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("S2", 5));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S2"}, {"price", 5.0}}, null);

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("S1", 101));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("S1", 102));
            assertFalse(env.listener("s0").isInvoked());

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"price"}, new Object[][]{{100.0}, {5.0}});

            env.milestone(4);

            env.sendEventBean(makeMarketDataEvent("S3", 6));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S3"}, {"price", 6.0}}, null);

            env.undeployAll();
        }
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, double price) {
        return new SupportMarketDataBean(symbol, price, 0L, "");
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}