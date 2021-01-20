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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNull;

public class ResultSetQueryTypeRowForAllHaving {
    private final static String JOIN_KEY = "KEY";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeRowForAllWHavingSumOneView());
        execs.add(new ResultSetQueryTypeRowForAllWHavingSumJoin());
        execs.add(new ResultSetQueryTypeAvgRowForAllWHavingGroupWindow());
        return execs;
    }

    private static class ResultSetQueryTypeRowForAllWHavingSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBean#time(10 seconds) " +
                "having sum(longBoxed) > 10";
            env.compileDeploy(epl).addListener("s0");

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllWHavingSumJoin implements RegressionExecution {
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

    private static class ResultSetQueryTypeAvgRowForAllWHavingGroupWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream avg(price) as aprice from SupportMarketDataBean#unique(symbol) having avg(price) <= 0";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", -1);
            env.assertEqualsNew("s0", "aprice", -1.0);

            sendEvent(env, "A", 5);
            env.assertListenerNotInvoked("s0");

            sendEvent(env, "B", -6);
            env.assertEqualsNew("s0", "aprice", -.5d);

            env.milestone(0);

            sendEvent(env, "C", 2);
            env.assertListenerNotInvoked("s0");

            sendEvent(env, "C", 3);
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendEvent(env, "C", -2);
            env.assertEqualsNew("s0", "aprice", -1d);

            env.undeployAll();
        }
    }

    private static void tryAssert(RegressionEnvironment env) {
        // assert select result type
        env.assertStatement("s0", statement -> Assert.assertEquals(Long.class, statement.getEventType().getPropertyType("mySum")));

        sendTimerEvent(env, 0);
        sendEvent(env, 10);
        env.assertListenerNotInvoked("s0");

        env.milestone(0);

        sendTimerEvent(env, 5000);
        sendEvent(env, 15);
        env.assertEqualsNew("s0", "mySum", 25L);

        sendTimerEvent(env, 8000);
        sendEvent(env, -5);
        env.assertListener("s0", listener -> Assert.assertEquals(20L, listener.getAndResetLastNewData()[0].get("mySum")));

        env.milestone(1);

        sendTimerEvent(env, 10000);
        env.assertListener("s0", listener -> {
            Assert.assertEquals(20L, listener.getLastOldData()[0].get("mySum"));
            assertNull(listener.getAndResetLastNewData());
        });
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
