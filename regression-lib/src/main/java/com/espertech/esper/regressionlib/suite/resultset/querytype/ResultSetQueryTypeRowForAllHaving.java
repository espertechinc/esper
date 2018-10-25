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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ResultSetQueryTypeRowForAllHaving {
    private final static String JOIN_KEY = "KEY";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeSumOneView());
        execs.add(new ResultSetQueryTypeSumJoin());
        execs.add(new ResultSetQueryTypeAvgGroupWindow());
        return execs;
    }

    private static class ResultSetQueryTypeSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBean#time(10 seconds) " +
                "having sum(longBoxed) > 10";
            env.compileDeploy(epl).addListener("s0");

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBeanString#time(10 seconds) as one, " +
                "SupportBean#time(10 seconds) as two " +
                "where one.theString = two.theString " +
                "having sum(longBoxed) > 10";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBeanString(JOIN_KEY));

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAvgGroupWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream avg(price) as aprice from SupportMarketDataBean#unique(symbol) having avg(price) <= 0";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", -1);
            Assert.assertEquals(-1.0d, env.listener("s0").getLastNewData()[0].get("aprice"));
            env.listener("s0").reset();

            sendEvent(env, "A", 5);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "B", -6);
            Assert.assertEquals(-.5d, env.listener("s0").getLastNewData()[0].get("aprice"));
            env.listener("s0").reset();

            env.milestone(0);

            sendEvent(env, "C", 2);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "C", 3);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendEvent(env, "C", -2);
            Assert.assertEquals(-1d, env.listener("s0").getLastNewData()[0].get("aprice"));
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static void tryAssert(RegressionEnvironment env) {
        // assert select result type
        Assert.assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("mySum"));

        sendTimerEvent(env, 0);
        sendEvent(env, 10);
        assertFalse(env.listener("s0").isInvoked());

        env.milestone(0);

        sendTimerEvent(env, 5000);
        sendEvent(env, 15);
        Assert.assertEquals(25L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));

        sendTimerEvent(env, 8000);
        sendEvent(env, -5);
        Assert.assertEquals(20L, env.listener("s0").getAndResetLastNewData()[0].get("mySum"));
        assertNull(env.listener("s0").getLastOldData());

        env.milestone(1);

        sendTimerEvent(env, 10000);
        Assert.assertEquals(20L, env.listener("s0").getLastOldData()[0].get("mySum"));
        assertNull(env.listener("s0").getAndResetLastNewData());
    }

    private static Object sendEvent(RegressionEnvironment env, String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, null, null);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed) {
        sendEvent(env, longBoxed, 0, (short) 0);
    }

    private static void sendTimerEvent(RegressionEnvironment env, long msec) {
        env.advanceTime(msec);
    }
}
