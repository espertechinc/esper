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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.DeploymentOptions;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternObserverTimerInterval {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOp());
        execs.add(new PatternIntervalSpec());
        execs.add(new PatternIntervalSpecVariables());
        execs.add(new PatternIntervalSpecExpression());
        execs.add(new PatternIntervalSpecExpressionWithProperty());
        execs.add(new PatternIntervalSpecPreparedStmt());
        execs.add(new PatternMonthScoped());
        execs.add(new PatternIntervalSpecExpressionWithPropertyArray());
        return execs;
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            // The wait is done when 2 seconds passed
            testCase = new EventExpressionCase("timer:interval(1999 msec)");
            testCase.add("B1");
            testCaseList.addTest(testCase);

            String text = "select * from pattern [timer:interval(1.999d)]";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            PatternExpr pattern = Patterns.timerInterval(1.999d);
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model = SerializableObjectCopier.copyMayFail(model);
            Assert.assertEquals(text, model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("B1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(2 sec)");
            testCase.add("B1");
            testCaseList.addTest(testCase);

            // 3 seconds (>2001 microseconds) passed
            testCase = new EventExpressionCase("timer:interval(2.001)");
            testCase.add("C1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(2999 milliseconds)");
            testCase.add("C1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(3 seconds)");
            testCase.add("C1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(3.001 seconds)");
            testCase.add("B2");
            testCaseList.addTest(testCase);

            // Try with an all ... repeated timer every 3 seconds
            testCase = new EventExpressionCase("every timer:interval(3.001 sec)");
            testCase.add("B2");
            testCase.add("F1");
            testCase.add("D3");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:interval(5000 msec)");
            testCase.add("A2");
            testCase.add("B3");
            testCaseList.addTest(testCase);


            testCase = new EventExpressionCase("timer:interval(3.999 second) -> b=SupportBean_B");
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(4 sec) -> b=SupportBean_B");
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(4.001 sec) -> b=SupportBean_B");
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(0) -> b=SupportBean_B");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            // Try with an followed-by as a second argument
            testCase = new EventExpressionCase("b=SupportBean_B -> timer:interval(0.001)");
            testCase.add("C1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> timer:interval(0)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> timer:interval(1 sec)");
            testCase.add("C1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B -> timer:interval(1.001)");
            testCase.add("B2", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            // Try in a 3-way followed by
            testCase = new EventExpressionCase("b=SupportBean_B() -> timer:interval(6.000) -> d=SupportBean_D");
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B() -> timer:interval(2.001) -> d=SupportBean_D())");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B() -> timer:interval(2.000) -> d=SupportBean_D())");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            // Try with an "or"
            testCase = new EventExpressionCase("b=SupportBean_B() or timer:interval(1.001)");
            testCase.add("B1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() or timer:interval(2.001)");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B3') or timer:interval(8.500)");
            testCase.add("D2");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(8.500) or timer:interval(7.500)");
            testCase.add("F1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(999999 msec) or g=SupportBean_G");
            testCase.add("G1", "g", events.getEvent("G1"));
            testCaseList.addTest(testCase);

            // Try with an "and"
            testCase = new EventExpressionCase("b=SupportBean_B() and timer:interval(4000 msec)");
            testCase.add("B2", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B() and timer:interval(4001 msec)");
            testCase.add("A2", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(9999999 msec) and b=SupportBean_B");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(1 msec) and b=SupportBean_B(id='B2')");
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            // Try with an "within"
            testCase = new EventExpressionCase("timer:interval(3.000) where timer:within(2.000)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:interval(3.000) where timer:within (3.000)");
            testCaseList.addTest(testCase);

            // Run all tests
            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }

        /**
         * As of release 1.6 this no longer updates listeners when the statement is started.
         * The reason is that the dispatch view only gets attached after a pattern started, therefore
         * ZeroDepthEventStream looses the event.
         * There should be no use case requiring this
         * <p>
         * testCase = new EventExpressionCase("not timer:interval(5000 millisecond)");
         * testCase.add(EventCollection.ON_START_EVENT_ID);
         * testCaseList.addTest(testCase);
         *
         * @param runtime
         */
    }

    private static class PatternIntervalSpec implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            env.compileDeploy("@name('s0') select * from pattern [timer:interval(1 minute 2 seconds)]");
            env.addListener("s0");

            sendTimer(62 * 1000 - 1, env);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(62 * 1000, env);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternIntervalSpecVariables implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable double M_isv=1", path);
            env.compileDeploy("create variable double S_isv=2", path);
            env.compileDeploy("@name('s0') select * from pattern [timer:interval(M_isv minute S_isv seconds)]", path);
            env.addListener("s0");

            sendTimer(62 * 1000 - 1, env);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(62 * 1000, env);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternIntervalSpecExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable double MOne=1", path);
            env.compileDeploy("create variable double SOne=2", path);
            env.compileDeploy("@name('s0') select * from pattern [timer:interval(MOne*60+SOne seconds)]", path);
            env.addListener("s0");

            sendTimer(62 * 1000 - 1, env);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(62 * 1000, env);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternIntervalSpecExpressionWithProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            env.compileDeploy("@name('s0') select a.theString as id from pattern [every a=SupportBean -> timer:interval(intPrimitive seconds)]");
            env.addListener("s0");

            sendTimer(10000, env);
            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E2", 2));

            sendTimer(11999, env);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(12000, env);
            Assert.assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("id"));

            sendTimer(12999, env);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(13000, env);
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class PatternIntervalSpecExpressionWithPropertyArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            env.compileDeploy("@name('s0') select a[0].theString as a0id, a[1].theString as a1id from pattern [ [2] a=SupportBean -> timer:interval(a[0].intPrimitive+a[1].intPrimitive seconds)]");
            env.addListener("s0");

            sendTimer(10000, env);
            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E2", 2));

            sendTimer(14999, env);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(15000, env);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a0id,a1id".split(","), "E1,E2".split(","));

            env.undeployAll();
        }
    }

    private static class PatternIntervalSpecPreparedStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // External clocking
            sendTimer(0, env);

            // Set up a timer:within
            EPCompiled compiled = env.compile("@name('s0') select * from pattern [timer:interval(?::int minute ?::int seconds)]");
            env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                prepared.setObject(1, 1);
                prepared.setObject(2, 2);
            }));
            env.addListener("s0");

            sendTimer(62 * 1000 - 1, env);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(62 * 1000, env);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            env.compileDeploy("@name('s0') select * from pattern [timer:interval(1 month)]").addListener("s0");

            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            assertFalse(env.listener("s0").isInvoked());

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }
}