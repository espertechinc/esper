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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.DeploymentOptions;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternGuardTimerWithin {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOp());
        execs.add(new PatternInterval10Min());
        execs.add(new PatternInterval10MinVariable());
        execs.add(new PatternIntervalPrepared());
        execs.add(new PatternWithinFromExpression());
        execs.add(new PatternPatternNotFollowedBy());
        execs.add(new PatternWithinMayMaxMonthScoped());
        return execs;
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase = null;

            testCase = new EventExpressionCase("b=SupportBean_B(id='B1') where timer:within(2 sec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B1') where timer:within(2001 msec)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B1') where timer:within(1999 msec)");
            testCaseList.addTest(testCase);

            String text = "select * from pattern [b=SupportBean_B(id=\"B3\") where timer:within(10.001d)]";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            model = SerializableObjectCopier.copyMayFail(model);
            Expression filter = Expressions.eq("id", "B3");
            PatternExpr pattern = Patterns.timerWithin(10.001, Patterns.filter(Filter.create("SupportBean_B", filter), "b"));
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            Assert.assertEquals(text, model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B3') where timer:within(10001 msec)");
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B3') where timer:within(10 sec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B3') where timer:within(9.999)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:within(2.001)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:within(4.001)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B where timer:within(2.001)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B where timer:within(2001 msec))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every ((every b=SupportBean_B) where timer:within(2.001))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every ((every b=SupportBean_B) where timer:within(6.001))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:within(11.001)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:within(4001 milliseconds)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B) where timer:within(6.001)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> d=SupportBean_D where timer:within(4001 milliseconds)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() -> d=SupportBean_D() where timer:within(4 sec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B() where timer:within (4.001) and d=SupportBean_D() where timer:within(6.001))");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() where timer:within (2001 msec) and d=SupportBean_D() where timer:within(6001 msec)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() where timer:within (2001 msec) and d=SupportBean_D() where timer:within(6000 msec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() where timer:within (2000 msec) and d=SupportBean_D() where timer:within(6001 msec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B -> d=SupportBean_D where timer:within(4000 msec)");
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B() -> every d=SupportBean_D where timer:within(4000 msec)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() -> d=SupportBean_D() where timer:within(3999 msec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B() -> (every d=SupportBean_D) where timer:within(2001 msec)");
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B() -> d=SupportBean_D()) where timer:within(6001 msec)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() where timer:within (2000 msec) or d=SupportBean_D() where timer:within(6000 msec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(b=SupportBean_B() where timer:within (2000 msec) or d=SupportBean_D() where timer:within(6000 msec)) where timer:within (1999 msec)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B() where timer:within (2001 msec) and d=SupportBean_D() where timer:within(6001 msec))");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() where timer:within (2001 msec) or d=SupportBean_D() where timer:within(6001 msec)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() where timer:within (2000 msec) or d=SupportBean_D() where timer:within(6001 msec)");
            testCase.add("D1", "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B() where timer:within (2001 msec) and every d=SupportBean_D() where timer:within(6001 msec)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:within (2000 msec) and every d=SupportBean_D() where timer:within(6001 msec)");
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternInterval10Min implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);
            Assert.assertEquals(0, env.eventService().getCurrentTime());

            // Set up a timer:within
            env.compileDeploy("@name('s0') select * from pattern [(every SupportBean) where timer:within(1 days 2 hours 3 minutes 4 seconds 5 milliseconds)]");
            env.addListener("s0");

            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class PatternInterval10MinVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            String stmtText = "@name('s0') select * from pattern [(every SupportBean) where timer:within(D days H hours M minutes S seconds MS milliseconds)]";
            env.compileDeploy(stmtText).addListener("s0");

            tryAssertion(env);

            EPStatementObjectModel model = env.eplToModel(stmtText);
            Assert.assertEquals(stmtText, model.toEPL());

            env.undeployAll();
        }
    }

    private static class PatternIntervalPrepared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            EPCompiled compiled = env.compile("@name('s0') select * from pattern [(every SupportBean) where timer:within(?::int days ?::int hours ?::int minutes ?::int seconds ?::int milliseconds)]");
            env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                prepared.setObject(1, 1);
                prepared.setObject(2, 2);
                prepared.setObject(3, 3);
                prepared.setObject(4, 4);
                prepared.setObject(5, 5);
            }));
            env.addListener("s0");

            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class PatternWithinFromExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            env.compileDeploy("@name('s0') select b.theString as id from pattern[a=SupportBean -> (every b=SupportBean) where timer:within(a.intPrimitive seconds)]");
            env.addListener("s0");

            // seed
            env.sendEventBean(new SupportBean("E1", 3));

            sendTimer(2000, env);
            env.sendEventBean(new SupportBean("E2", -1));
            Assert.assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("id"));

            sendTimer(2999, env);
            env.sendEventBean(new SupportBean("E3", -1));
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("id"));

            sendTimer(3000, env);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternPatternNotFollowedBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);

            String stmtText = "@name('s0') select * from pattern [ every(SupportBean -> (SupportMarketDataBean where timer:within(5 sec))) ]";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            sendTimer(6000, env);

            env.sendEventBean(new SupportBean("E4", 1));

            env.milestone(1);

            env.sendEventBean(new SupportMarketDataBean("E5", "M1", 1d));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternWithinMayMaxMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionWithinMayMaxMonthScoped(env, false);
            tryAssertionWithinMayMaxMonthScoped(env, true);
        }
    }

    private static void tryAssertionWithinMayMaxMonthScoped(RegressionEnvironment env, boolean hasMax) {
        sendCurrentTime(env, "2002-02-01T09:00:00.000");

        String epl = "@name('s0') select * from pattern [(every SupportBean) where " +
            (hasMax ? "timer:withinmax(1 month, 10)]" : "timer:within(1 month)]");
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 0));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
        env.sendEventBean(new SupportBean("E2", 0));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        sendCurrentTime(env, "2002-03-01T09:00:00.000");
        env.sendEventBean(new SupportBean("E3", 0));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void tryAssertion(RegressionEnvironment env) {
        sendEvent(env);
        env.listener("s0").assertOneGetNewAndReset();

        long time = 24 * 60 * 60 * 1000 + 2 * 60 * 60 * 1000 + 3 * 60 * 1000 + 4 * 1000 + 5;
        sendTimer(time - 1, env);
        Assert.assertEquals(time - 1, env.eventService().getCurrentTime());
        sendEvent(env);
        env.listener("s0").assertOneGetNewAndReset();

        sendTimer(time, env);
        sendEvent(env);
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }

    private static void sendEvent(RegressionEnvironment env) {
        SupportBean theEvent = new SupportBean();
        env.sendEventBean(theEvent);
    }
}
