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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class PatternOperatorEvery {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternEverySimple());
        execs.add(new PatternOp());
        execs.add(new PatternEveryWithAnd());
        execs.add(new PatternEveryFollowedByWithin());
        execs.add(new PatternEveryAndNot());
        execs.add(new PatternEveryFollowedBy());
        return execs;
    }

    public static class PatternEverySimple implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");

            String epl = "@Name('s0') select a.theString as c0 from pattern [every a=SupportBean]";
            env.compileDeploy(epl).addListener("s0");

            sendSupportBean(env, "E1", 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            sendSupportBean(env, "E2", 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            sendSupportBean(env, "E3", 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.milestone(3);

            sendSupportBean(env, "E4", 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            SupportListener listener = env.listener("s0");
            env.undeployModuleContaining("s0");

            sendSupportBean(env, "E5", 0);
            Assert.assertFalse(listener.isInvoked());

            env.milestone(4);

            sendSupportBean(env, "E6", 0);
            Assert.assertFalse(listener.isInvoked());

            env.undeployAll();
        }
    }

    public static class PatternEveryFollowedBy implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0') select a.theString as c0, a.intPrimitive as c1, b.intPrimitive as c2 " +
                "from pattern [every a=SupportBean -> b=SupportBean(theString=a.theString)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBean(env, "E2", 10);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            sendSupportBean(env, "E1", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, 2});

            env.milestone(3);

            env.undeployAll();
        }
    }

    public static class PatternEveryFollowedByWithin implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");

            env.advanceTime(0);
            String epl = "@Name('s0') select a.theString as c0, a.intPrimitive as c1, b.intPrimitive as c2 " +
                "from pattern [every a=SupportBean -> b=SupportBean(theString=a.theString) where timer:within(10 sec)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.advanceTime(5000);
            sendSupportBean(env, "E1", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.advanceTime(8000);
            sendSupportBean(env, "E2", 10);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.advanceTime(15000);   // expires E1 subexpression
            env.milestone(3);

            sendSupportBean(env, "E1", 2);
            Assert.assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "E2", 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 10, 11});

            env.undeployAll();
        }
    }

    public static class PatternEveryWithAnd implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");

            String epl = "@Name('s0') select a.theString as c0, b.theString as c1 from pattern [every (a=SupportBean(intPrimitive>0) and b=SupportBean(intPrimitive<0))]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBean(env, "E2", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E3", -1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3"});

            env.milestone(2);

            sendSupportBean(env, "E4", -2);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendSupportBean(env, "E5", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", "E4"});

            env.undeployAll();
        }
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("every b=SupportBean_B");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (every (every b=SupportBean_B))");
            testCase.add("B1", "b", events.getEvent("B1"));
            for (int i = 0; i < 3; i++) {
                testCase.add("B2", "b", events.getEvent("B2"));
            }
            for (int i = 0; i < 9; i++) {
                testCase.add("B3", "b", events.getEvent("B3"));
            }
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (every b=SupportBean_B())");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B2", "b", events.getEvent("B2"));
            for (int i = 0; i < 4; i++) {
                testCase.add("B3", "b", events.getEvent("B3"));
            }
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every( every (every (every b=SupportBean_B())))");
            testCase.add("B1", "b", events.getEvent("B1"));
            for (int i = 0; i < 4; i++) {
                testCase.add("B2", "b", events.getEvent("B2"));
            }
            for (int i = 0; i < 16; i++) {
                testCase.add("B3", "b", events.getEvent("B3"));
            }
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternEveryAndNot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String expression = "@name('s0') select 'No event within 6 seconds' as alert\n" +
                "from pattern [ every (timer:interval(6) and not SupportBean)]";
            env.compileDeploy(expression).addListener("s0");

            sendTimer(env, 2000);
            env.sendEventBean(new SupportBean());

            sendTimer(env, 6000);
            sendTimer(env, 7000);
            sendTimer(env, 7999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 8000);
            Assert.assertEquals("No event within 6 seconds", env.listener("s0").assertOneGetNewAndReset().get("alert"));

            sendTimer(env, 12000);
            env.sendEventBean(new SupportBean());
            sendTimer(env, 13000);
            env.sendEventBean(new SupportBean());

            sendTimer(env, 18999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 19000);
            Assert.assertEquals("No event within 6 seconds", env.listener("s0").assertOneGetNewAndReset().get("alert"));

            env.undeployAll();
        }

        private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
            env.advanceTime(timeInMSec);
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}