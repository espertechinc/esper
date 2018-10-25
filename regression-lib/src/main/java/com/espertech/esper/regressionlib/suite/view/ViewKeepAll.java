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
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertFalse;

public class ViewKeepAll {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewKeepAllSimple());
        execs.add(new ViewKeepAllIterator());
        execs.add(new ViewKeepAllWindowStats());
        return execs;
    }

    public static class ViewKeepAllSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");

            String epl = "@Name('s0') select irstream theString as c0 from SupportBean#keepall()";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}});
            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            sendSupportBean(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            sendSupportBean(env, "E4");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

            env.milestone(4);

            env.undeployAll();
        }
    }


    private static class ViewKeepAllIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select symbol, price from SupportMarketDataBean#keepall";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "ABC", 20);
            sendEvent(env, "DEF", 100);

            // check iterator results
            Iterator<EventBean> events = env.statement("s0").iterator();
            EventBean theEvent = events.next();
            Assert.assertEquals("ABC", theEvent.get("symbol"));
            Assert.assertEquals(20d, theEvent.get("price"));

            theEvent = events.next();
            Assert.assertEquals("DEF", theEvent.get("symbol"));
            Assert.assertEquals(100d, theEvent.get("price"));
            assertFalse(events.hasNext());

            sendEvent(env, "EFG", 50);

            // check iterator results
            events = env.statement("s0").iterator();
            theEvent = events.next();
            Assert.assertEquals("ABC", theEvent.get("symbol"));
            Assert.assertEquals(20d, theEvent.get("price"));

            theEvent = events.next();
            Assert.assertEquals("DEF", theEvent.get("symbol"));
            Assert.assertEquals(100d, theEvent.get("price"));

            theEvent = events.next();
            Assert.assertEquals("EFG", theEvent.get("symbol"));
            Assert.assertEquals(50d, theEvent.get("price"));

            env.undeployAll();
        }
    }

    private static class ViewKeepAllWindowStats implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, count(*) as cnt, sum(price) as mysum from SupportMarketDataBean#keepall group by symbol";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "S1", 100);
            String[] fields = new String[]{"symbol", "cnt", "mysum"};
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"S1", 1L, 100d});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"S1", 0L, null});
            env.listener("s0").reset();

            sendEvent(env, "S2", 50);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"S2", 1L, 50d});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"S2", 0L, null});
            env.listener("s0").reset();

            sendEvent(env, "S1", 5);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"S1", 2L, 105d});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"S1", 1L, 100d});
            env.listener("s0").reset();

            sendEvent(env, "S2", -1);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"S2", 2L, 49d});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"S2", 1L, 50d});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String string) {
        env.sendEventBean(new SupportBean(string, 0));
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, "");
        env.sendEventBean(theEvent);
    }
}
