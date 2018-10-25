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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ViewUnique {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewLastUniqueSceneOne(null));
        execs.add(new ViewLastUniqueSceneTwo(null));
        execs.add(new ViewLastUniqueWithAnnotationPrefix(null));
        execs.add(new ViewUniqueExpressionParameter());
        execs.add(new ViewUniqueTwoWindows());
        return execs;
    }

    public static class ViewLastUniqueSceneOne implements RegressionExecution {

        private final String optionalAnnotation;

        public ViewLastUniqueSceneOne(String optionalAnnotation) {
            this.optionalAnnotation = optionalAnnotation;
        }

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol, price from SupportMarketDataBean#unique(symbol) order by symbol";
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
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S1"}, {"price", 101.0}},
                new Object[][]{{"symbol", "S1"}, {"price", 100.0}});

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("S1", 102));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S1"}, {"price", 102.0}},
                new Object[][]{{"symbol", "S1"}, {"price", 101.0}});

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"price"}, new Object[][]{{102.0}, {5.0}});

            env.milestone(4);

            env.sendEventBean(makeMarketDataEvent("S2", 6));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S2"}, {"price", 6.0}},
                new Object[][]{{"symbol", "S2"}, {"price", 5.0}});

            env.undeployAll();
        }
    }

    public static class ViewLastUniqueSceneTwo implements RegressionExecution {
        private final String optionalAnnotation;

        public ViewLastUniqueSceneTwo(String optionalAnnotation) {
            this.optionalAnnotation = optionalAnnotation;
        }

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol, feed, price from  SupportMarketDataBean#unique(symbol, feed) order by symbol, feed";
            if (optionalAnnotation != null) {
                text = optionalAnnotation + text;
            }
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("S1", "F1", 100));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S1"}, {"feed", "F1"}, {"price", 100.0}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("S2", "F1", 5));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S2"}, {"feed", "F1"}, {"price", 5.0}}, null);

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("S1", "F1", 101));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S1"}, {"feed", "F1"}, {"price", 101.0}},
                new Object[][]{{"symbol", "S1"}, {"feed", "F1"}, {"price", 100.0}});

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("S2", "F1", 102));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S2"}, {"feed", "F1"}, {"price", 102.0}},
                new Object[][]{{"symbol", "S2"}, {"feed", "F1"}, {"price", 5.0}});

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(events, new String[]{"price"}, new Object[][]{{101.0}, {102.0}});

            env.milestone(4);

            env.sendEventBean(makeMarketDataEvent("S1", "F2", 6));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "S1"}, {"feed", "F2"}, {"price", 6.0}}, null);

            // test iterator
            events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(events, new String[]{"price"}, new Object[][]{{101.0}, {6.0}, {102.0}});

            env.undeployAll();
        }
    }

    public static class ViewLastUniqueWithAnnotationPrefix implements RegressionExecution {
        private final String optionalAnnotations;

        public ViewLastUniqueWithAnnotationPrefix(String optionalAnnotations) {
            this.optionalAnnotations = optionalAnnotations;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@Name('s0') select irstream theString as c0, intPrimitive as c1 from SupportBean#unique(theString)";
            if (optionalAnnotations != null) {
                epl = optionalAnnotations + epl;
            }
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.milestone(1);

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
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E1", 2}, new Object[]{"E1", 1});

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 2}, {"E2", 20}});
            sendSupportBean(env, "E2", 21);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E2", 21}, new Object[]{"E2", 20});

            env.milestone(5);
            env.milestone(6);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 2}, {"E2", 21}});
            sendSupportBean(env, "E2", 22);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E2", 22}, new Object[]{"E2", 21});
            sendSupportBean(env, "E1", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E1", 3}, new Object[]{"E1", 2});

            sendSupportBean(env, "E3", 30);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 30});

            env.undeployAll();
        }
    }

    private static class ViewUniqueExpressionParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean#unique(Math.abs(intPrimitive))";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendSupportBean(env, "E1", 10);
            sendSupportBean(env, "E2", -10);
            sendSupportBean(env, "E3", -5);
            sendSupportBean(env, "E4", 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), "theString".split(","), new Object[][]{{"E2"}, {"E4"}});

            env.undeployAll();
        }
    }

    private static class ViewUniqueTwoWindows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#unique(intBoxed)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportBean beanOne = new SupportBean("E1", 1);
            env.sendEventBean(beanOne);
            env.listener("s0").assertOneGetNewAndReset();

            String eplTwo = "@name('s1') select irstream * from SupportBean#unique(intBoxed)";
            env.compileDeployAddListenerMile(eplTwo, "s1", 1);

            SupportBean beanTwo = new SupportBean("E2", 2);
            env.sendEventBean(beanTwo);

            assertEquals(beanTwo, env.listener("s0").getLastNewData()[0].getUnderlying());
            assertEquals(beanOne, env.listener("s0").getLastOldData()[0].getUnderlying());
            assertEquals(beanTwo, env.listener("s1").getLastNewData()[0].getUnderlying());
            assertNull(env.listener("s1").getLastOldData());

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, String feed, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, feed);
        return bean;
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, "");
        return bean;
    }
}
