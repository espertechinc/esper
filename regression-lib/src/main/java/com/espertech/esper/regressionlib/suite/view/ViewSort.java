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
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWithEnum;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

public class ViewSort {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewSortSceneOne());
        execs.add(new ViewSortSceneTwo());
        execs.add(new ViewSortedSingleKeyBuiltin());
        execs.add(new ViewSortedMultikey());
        execs.add(new ViewSortedPrimitiveKey());
        execs.add(new ViewSortedPrev());
        return execs;
    }

    private static class ViewSortSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#sort(3, intPrimitive desc, longPrimitive)";
            env.compileDeployAddListenerMileZero(epl, "s0");
            String[] fields = "theString,intPrimitive,longPrimitive".split(",");

            env.sendEventBean(makeEvent("E1", 100, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 100, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 0L}});

            env.sendEventBean(makeEvent("E2", 99, 5L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 99, 5L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 100, 0L}, {"E2", 99, 5L}});

            env.sendEventBean(makeEvent("E3", 100, -1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 100, -1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3", 100, -1L}, {"E1", 100, 0L}, {"E2", 99, 5L}});

            env.sendEventBean(makeEvent("E4", 100, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E4", 100, 1L}, new Object[]{"E2", 99, 5L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3", 100, -1L}, {"E1", 100, 0L}, {"E4", 100, 1L}});

            env.sendEventBean(makeEvent("E5", 101, 10L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E5", 101, 10L}, new Object[]{"E4", 100, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5", 101, 10L}, {"E3", 100, -1L}, {"E1", 100, 0L}});

            env.sendEventBean(makeEvent("E6", 101, 11L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E6", 101, 11L}, new Object[]{"E1", 100, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5", 101, 10L}, {"E6", 101, 11L}, {"E3", 100, -1L}});

            env.sendEventBean(makeEvent("E6", 100, 0L));
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E6", 100, 0L}, new Object[]{"E6", 100, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5", 101, 10L}, {"E6", 101, 11L}, {"E3", 100, -1L}});

            env.undeployAll();
        }
    }

    public static class ViewSortSceneTwo implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive".split(",");

            String epl = "@Name('s0') select irstream * from SupportBean#sort(3, theString)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            sendSupportBean(env, "G", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G", 1});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"G", 1}});
            sendSupportBean(env, "E", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E", 2});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E", 2}, {"G", 1}});
            sendSupportBean(env, "H", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"H", 3});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E", 2}, {"G", 1}, {"H", 3}});
            sendSupportBean(env, "I", 4);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"I", 4}, new Object[]{"I", 4});

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E", 2}, {"G", 1}, {"H", 3}});
            sendSupportBean(env, "A", 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"A", 5}, new Object[]{"H", 3});

            env.milestone(5);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 5}, {"E", 2}, {"G", 1}});
            sendSupportBean(env, "C", 6);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"C", 6}, new Object[]{"G", 1});

            env.milestone(6);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 5}, {"C", 6}, {"E", 2}});
            sendSupportBean(env, "C", 7);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"C", 7}, new Object[]{"E", 2});

            env.milestone(7);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 5}, {"C", 7}, {"C", 6}});
            sendSupportBean(env, "C", 8);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"C", 8}, new Object[]{"C", 6});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 5}, {"C", 8}, {"C", 7}});

            env.undeployAll();
        }
    }

    public static class ViewSortedSingleKeyBuiltin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from  SupportMarketDataBean#sort(3, symbol)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("B1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "B1"}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("D1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "D1"}}, null);

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("C1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "C1"}}, null);

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("A1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "A1"}}, new Object[][]{{"symbol", "D1"}});

            env.milestone(4);

            env.sendEventBean(makeMarketDataEvent("F1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "F1"}}, new Object[][]{{"symbol", "F1"}});

            env.milestone(5);

            env.sendEventBean(makeMarketDataEvent("B2"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "B2"}}, new Object[][]{{"symbol", "C1"}});

            env.milestone(6);

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"symbol"}, new Object[][]{{"A1"}, {"B1"}, {"B2"}});

            env.undeployAll();
        }
    }

    public static class ViewSortedMultikey implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportBeanWithEnum#sort(1, theString, supportEnum)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(new SupportBeanWithEnum("E1", SupportEnum.ENUM_VALUE_1));
            env.listener("s0").assertNewOldData(new Object[][]{{"theString", "E1"}, {"supportEnum", SupportEnum.ENUM_VALUE_1}}, null);

            env.milestone(1);

            env.sendEventBean(new SupportBeanWithEnum("E2", SupportEnum.ENUM_VALUE_2));
            env.listener("s0").assertNewOldData(new Object[][]{{"theString", "E2"}}, new Object[][]{{"theString", "E2"}});

            env.milestone(2);

            env.sendEventBean(new SupportBeanWithEnum("E0", SupportEnum.ENUM_VALUE_1));
            env.listener("s0").assertNewOldData(new Object[][]{{"theString", "E0"}}, new Object[][]{{"theString", "E1"}});

            env.undeployAll();
        }
    }

    public static class ViewSortedPrimitiveKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#sort(1, price)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent(10.5));
            env.listener("s0").assertNewOldData(new Object[][]{{"price", 10.5}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent(10));
            env.listener("s0").assertNewOldData(new Object[][]{{"price", 10.0}}, new Object[][]{{"price", 10.5}});

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent(11));
            env.listener("s0").assertNewOldData(new Object[][]{{"price", 11.0}}, new Object[][]{{"price", 11.0}});

            env.undeployAll();
        }
    }

    public static class ViewSortedPrev implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol, " +
                "prev(1, symbol) as prev1," +
                "prevtail(symbol) as prevtail, " +
                "prevcount(symbol) as prevCountSym, " +
                "prevwindow(symbol) as prevWindowSym " +
                "from SupportMarketDataBean#sort(3, symbol)";
            env.compileDeploy(text).addListener("s0");
            String[] fields = new String[]{"symbol", "prev1", "prevtail", "prevCountSym", "prevWindowSym"};

            env.sendEventBean(makeMarketDataEvent("B1"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
                fields, new Object[][]{{"B1", null, "B1", 1L, new Object[]{"B1"}}});
            env.listener("s0").reset();

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent("D1"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
                fields, new Object[][]{{"D1", "D1", "D1", 2L, new Object[]{"B1", "D1"}}});
            env.listener("s0").reset();

            env.sendEventBean(makeMarketDataEvent("C1"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
                fields, new Object[][]{{"C1", "C1", "D1", 3L, new Object[]{"B1", "C1", "D1"}}});
            env.listener("s0").reset();

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("A1"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
                fields, new Object[][]{{"A1", "B1", "C1", 3L, new Object[]{"A1", "B1", "C1"}}});
            env.listener("s0").reset();

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("F1"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
                fields, new Object[][]{{"F1", "B1", "C1", 3L, new Object[]{"A1", "B1", "C1"}}});
            env.listener("s0").reset();

            env.milestone(3);

            env.undeployAll();
        }
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static SupportMarketDataBean makeMarketDataEvent(double price) {
        return new SupportMarketDataBean("IBM", price, 0L, null);
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}
