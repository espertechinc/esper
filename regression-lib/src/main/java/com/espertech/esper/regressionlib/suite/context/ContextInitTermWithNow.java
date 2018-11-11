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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ContextInitTermWithNow {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextStartStopWNow());
        execs.add(new ContextInitTermWithPattern());
        execs.add(new ContextInitTermWNowInvalid());
        execs.add(new ContextInitTermWNowNoEnd());
        return execs;
    }

    private static class ContextInitTermWNowNoEnd implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context StartNowAndNeverEnd start @now;\n" +
                "@name('s0') context StartNowAndNeverEnd select count(*) as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            assertSendCount(env, 1);
            assertSendCount(env, 2);

            env.milestone(1);

            assertSendCount(env, 3);

            env.milestone(2);

            assertSendCount(env, 4);

            env.undeployAll();
        }

        private void assertSendCount(RegressionEnvironment env, long expected) {
            env.sendEventBean(new SupportBean());
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    private static class ContextStartStopWNow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            RegressionPath path = new RegressionPath();
            String contextExpr = "create context MyContext " +
                "as start @now end after 10 seconds";
            env.compileDeploy(contextExpr, path);

            String[] fields = new String[]{"cnt"};
            String streamExpr = "@name('s0') context MyContext " +
                "select count(*) as cnt from SupportBean output last when terminated";
            env.compileDeploy(streamExpr, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            env.advanceTime(8000);

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 3));
            env.advanceTime(10000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3L});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 4));
            env.advanceTime(19999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(20000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});

            env.milestone(3);

            env.advanceTime(30000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0L});

            env.eplToModelCompileDeploy(streamExpr, path);

            env.undeployAll();

            env.eplToModelCompileDeploy(contextExpr);
            env.undeployAll();
        }
    }

    private static class ContextInitTermWithPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            RegressionPath path = new RegressionPath();
            String contextExpr = "create context MyContext " +
                "initiated by @Now and pattern [every timer:interval(10)] terminated after 10 sec";
            env.compileDeploy(contextExpr, path);

            String[] fields = new String[]{"cnt"};
            String streamExpr = "@name('s0') context MyContext " +
                "select count(*) as cnt from SupportBean output last when terminated";
            env.compileDeploy(streamExpr, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));

            env.milestone(0);

            env.advanceTime(8000);
            env.sendEventBean(new SupportBean("E3", 3));

            env.milestone(1);

            env.advanceTime(9999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(10000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3L});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 4));
            env.advanceTime(10100);

            env.milestone(3);

            env.sendEventBean(new SupportBean("E5", 5));
            env.advanceTime(19999);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(20000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L});

            env.milestone(4);

            env.advanceTime(30000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0L});

            env.milestone(5);

            env.sendEventBean(new SupportBean("E6", 6));

            env.advanceTime(40000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L});

            env.eplToModelCompileDeploy(streamExpr, path);

            env.undeployAll();
        }
    }

    private static class ContextInitTermWNowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // for overlapping contexts, @now without condition is not allowed
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context TimedImmediate initiated @now terminated after 10 seconds",
                "Incorrect syntax near 'terminated' (a reserved keyword) expecting 'and' but found 'terminated' at line 1 column 45 [create context TimedImmediate initiated @now terminated after 10 seconds]");

            // for non-overlapping contexts, @now with condition is not allowed
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context TimedImmediate start @now and after 5 seconds end after 10 seconds",
                "Incorrect syntax near 'and' (a reserved keyword) at line 1 column 41 [create context TimedImmediate start @now and after 5 seconds end after 10 seconds]");

            // for overlapping contexts, @now together with a filter condition is not allowed
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context TimedImmediate initiated @now and SupportBean terminated after 10 seconds",
                "Invalid use of 'now' with initiated-by stream, this combination is not supported [create context TimedImmediate initiated @now and SupportBean terminated after 10 seconds]");
        }
    }
}
