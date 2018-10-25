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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSetOutputLimitFirstHaving {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetHavingNoAvgOutputFirstEvents());
        execs.add(new ResultSetHavingNoAvgOutputFirstMinutes());
        execs.add(new ResultSetHavingAvgOutputFirstEveryTwoMinutes());
        return execs;
    }

    private static class ResultSetHavingNoAvgOutputFirstEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String query = "@name('s0') select doublePrimitive from SupportBean having doublePrimitive > 1 output first every 2 events";
            env.compileDeploy(query).addListener("s0");

            tryAssertion2Events(env);
            env.undeployAll();

            // test joined
            query = "@name('s0') select doublePrimitive from SupportBean#lastevent,SupportBean_ST0#lastevent st0 having doublePrimitive > 1 output first every 2 events";
            env.compileDeploy(query).addListener("s0");
            env.sendEventBean(new SupportBean_ST0("ID", 1));
            tryAssertion2Events(env);

            env.undeployAll();
        }
    }

    private static class ResultSetHavingNoAvgOutputFirstMinutes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String[] fields = "val0".split(",");
            String query = "@name('s0') select sum(doublePrimitive) as val0 from SupportBean#length(5) having sum(doublePrimitive) > 100 output first every 2 seconds";
            env.compileDeploy(query).addListener("s0");

            sendBeanEvent(env, 10);
            sendBeanEvent(env, 80);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(1000);
            sendBeanEvent(env, 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{101d});

            sendBeanEvent(env, 1);

            env.advanceTime(2999);
            sendBeanEvent(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(3000);
            sendBeanEvent(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanEvent(env, 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{114d});

            env.advanceTime(4999);
            sendBeanEvent(env, 0);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(5000);
            sendBeanEvent(env, 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{102d});

            env.undeployAll();
        }
    }

    private static class ResultSetHavingAvgOutputFirstEveryTwoMinutes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String query = "@name('s0') select doublePrimitive, avg(doublePrimitive) from SupportBean having doublePrimitive > 2*avg(doublePrimitive) output first every 2 minutes";
            env.compileDeploy(query).addListener("s0");

            sendBeanEvent(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanEvent(env, 2);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanEvent(env, 9);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void tryAssertion2Events(RegressionEnvironment env) {

        sendBeanEvent(env, 1);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 2);
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 9);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 1);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 1);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 2);
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 1);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 2);
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 2);
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendBeanEvent(env, 2);
        assertTrue(env.listener("s0").getAndClearIsInvoked());
    }

    private static void sendBeanEvent(RegressionEnvironment env, double doublePrimitive) {
        SupportBean b = new SupportBean();
        b.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(b);
    }
}

