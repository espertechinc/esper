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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.DeploymentOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class PatternOperatorMatchUntil {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternMatchUntilSimple());
        execs.add(new PatternOp());
        execs.add(new PatternSelectArray());
        execs.add(new PatternUseFilter());
        execs.add(new PatternRepeatUseTags());
        execs.add(new PatternArrayFunctionRepeat());
        execs.add(new PatternExpressionBounds());
        execs.add(new PatternBoundRepeatWithNot());
        execs.add(new PatternInvalid());
        return execs;
    }

    public static class PatternMatchUntilSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0') select a[0].theString as c0, a[1].theString as c1, b.theString as c2 from pattern [a=SupportBean(intPrimitive=0) until b=SupportBean(intPrimitive=1)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "A1", 0);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBean(env, "A2", 0);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            sendSupportBean(env, "B1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", "B1"});

            env.milestone(3);

            sendSupportBean(env, "A1", 0);
            sendSupportBean(env, "B1", 1);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }

        private void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
        }
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("a=SupportBean_A(id='A2') until SupportBean_D");
            testCase.add("D1", "a[0]", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A until SupportBean_D");
            testCase.add("D1", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B until a=SupportBean_A");
            testCase.add("A1", "b[0]", null, "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B until SupportBean_D(id='D3')");
            testCase.add("D3", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(a=SupportBean_A or b=SupportBean_B) until d=SupportBean_D(id='D3')");
            testCase.add("D3", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
                {"b[0]", events.getEvent("B1")},
                {"b[1]", events.getEvent("B2")},
                {"b[2]", events.getEvent("B3")},
                {"d", events.getEvent("D3")}});
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(a=SupportBean_A or b=SupportBean_B) until (g=SupportBean_G or d=SupportBean_D)");
            testCase.add("D1", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
                {"b[0]", events.getEvent("B1")},
                {"b[1]", events.getEvent("B2")},
                {"d", events.getEvent("D1")}});
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(d=SupportBean_D) until a=SupportBean_A(id='A1')");
            testCase.add("A1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A until SupportBean_G(id='GX')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2] a=SupportBean_A");
            testCase.add("A2", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2:2] a=SupportBean_A");
            testCase.add("A2", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1] a=SupportBean_A");
            testCase.add("A1", "a[0]", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:1] a=SupportBean_A");
            testCase.add("A1", "a[0]", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[3] a=SupportBean_A");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[3] b=SupportBean_B");
            testCase.add("B3", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[4] (a=SupportBean_A or b=SupportBean_B)");
            testCase.add("A2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            // the until ends the matching returning permanently false
            testCase = new EventExpressionCase("[2] b=SupportBean_B until a=SupportBean_A(id='A1')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2] b=SupportBean_B until c=SupportBean_C");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2:2] b=SupportBean_B until g=SupportBean_G(id='G1')");
            testCase.add("B2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[:4] b=SupportBean_B until g=SupportBean_G(id='G1')");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "g", events.getEvent("G1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[:3] b=SupportBean_B until g=SupportBean_G(id='G1')");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "g", events.getEvent("G1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[:2] b=SupportBean_B until g=SupportBean_G(id='G1')");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "g", events.getEvent("G1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[:1] b=SupportBean_B until g=SupportBean_G(id='G1')");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "g", events.getEvent("G1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[:1] b=SupportBean_B until a=SupportBean_A(id='A1')");
            testCase.add("A1", "b[0]", null, "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:] b=SupportBean_B until g=SupportBean_G(id='G1')");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "g", events.getEvent("G1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:] b=SupportBean_B until a=SupportBean_A");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2:] b=SupportBean_B until a=SupportBean_A(id='A2')");
            testCase.add("A2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "a", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2:] b=SupportBean_B until c=SupportBean_C");
            testCaseList.addTest(testCase);

            // same event triggering both clauses, until always wins, match does not count
            testCase = new EventExpressionCase("[2:] b=SupportBean_B until e=SupportBean_B(id='B2')");
            testCaseList.addTest(testCase);

            // same event triggering both clauses, until always wins, match does not count
            testCase = new EventExpressionCase("[1:] b=SupportBean_B until e=SupportBean_B(id='B1')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:2] b=SupportBean_B until a=SupportBean_A(id='A2')");
            testCase.add("A2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", null, "a", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:3] b=SupportBean_B until SupportBean_G");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "b[3]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:2] b=SupportBean_B until SupportBean_G");
            testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:10] b=SupportBean_B until SupportBean_F");
            testCase.add("F1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[1:10] b=SupportBean_B until SupportBean_C");
            testCase.add("C1", "b[0]", events.getEvent("B1"), "b[1]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[0:1] b=SupportBean_B until SupportBean_C");
            testCase.add("C1", "b[0]", events.getEvent("B1"), "b[1]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("c=SupportBean_C -> [2] b=SupportBean_B -> d=SupportBean_D");
            testCase.add("D3", "c", events.getEvent("C1"), "b[0]", events.getEvent("B2"), "b[1]", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[3] d=SupportBean_D or [3] b=SupportBean_B");
            testCase.add("B3", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[3] d=SupportBean_D or [4] b=SupportBean_B");
            testCase.add("D3", "d[0]", events.getEvent("D1"), "d[1]", events.getEvent("D2"), "d[2]", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2] d=SupportBean_D and [2] b=SupportBean_B");
            testCase.add("D2", "d[0]", events.getEvent("D1"), "d[1]", events.getEvent("D2"), "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("d=SupportBean_D until timer:interval(7 sec)");
            testCase.add("E1", "d[0]", events.getEvent("D1"), "d[1]", null, "d[2]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (d=SupportBean_D until b=SupportBean_B)");
            testCase.add("B1", "d[0]", null, "b", events.getEvent("B1"));
            testCase.add("B2", "d[0]", null, "b", events.getEvent("B2"));
            testCase.add("B3", "d[0]", events.getEvent("D1"), "d[1]", events.getEvent("D2"), "d[2]", null, "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            // note precendence: every is higher then until
            testCase = new EventExpressionCase("every d=SupportBean_D until b=SupportBean_B");
            testCase.add("B1", "d[0]", null, "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every d=SupportBean_D) until b=SupportBean_B");
            testCase.add("B1", "d[0]", null, "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A until (every (timer:interval(6 sec) and not SupportBean_A))");
            testCase.add("G1", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("SupportBean_A until (every (timer:interval(7 sec) and not SupportBean_A))");
            testCase.add("D3");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[2] (a=SupportBean_A or b=SupportBean_B)");
            testCase.add("B1", "a[0]", events.getEvent("A1"), "b[0]", events.getEvent("B1"), "b[1]", null);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every [2] a=SupportBean_A");
            testCase.add("A2", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
            });
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every [2] a=SupportBean_A until d=SupportBean_D");  // every has precedence; ESPER-339
            testCase.add("D1", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
                {"d", events.getEvent("D1")},
            });
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("[3] (a=SupportBean_A or b=SupportBean_B)");
            testCase.add("B2", "a[0]", events.getEvent("A1"), "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(a=SupportBean_A until b=SupportBean_B) until c=SupportBean_C");
            testCase.add("C1", "a[0]", events.getEvent("A1"), "b[0]", events.getEvent("B1"), "c", events.getEvent("C1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(a=SupportBean_A until b=SupportBean_B) until g=SupportBean_G");
            testCase.add("G1", new Object[][]{{"a[0]", events.getEvent("A1")}, {"b[0]", events.getEvent("B1")},
                {"a[1]", events.getEvent("A2")}, {"b[1]", events.getEvent("B2")},
                {"b[2]", events.getEvent("B3")},
                {"g", events.getEvent("G1")}
            });
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("SupportBean_B until not SupportBean_B");
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternSelectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select a, b, a[0] as a0, a[0].id as a0Id, a[1] as a1, a[1].id as a1Id, a[2] as a2, a[2].id as a2Id from pattern [a=SupportBean_A until b=SupportBean_B]";
            env.compileDeploy(stmt).addListener("s0");

            env.milestone(0);

            Object eventA1 = new SupportBean_A("A1");
            env.sendEventBean(eventA1);

            env.milestone(1);

            Object eventA2 = new SupportBean_A("A2");
            env.sendEventBean(eventA2);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            Object eventB1 = new SupportBean_B("B1");
            env.sendEventBean(eventB1);

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("a"), new Object[]{eventA1, eventA2});
            assertEquals(eventA1, theEvent.get("a0"));
            assertEquals(eventA2, theEvent.get("a1"));
            assertNull(theEvent.get("a2"));
            assertEquals("A1", theEvent.get("a0Id"));
            assertEquals("A2", theEvent.get("a1Id"));
            assertNull(null, theEvent.get("a2Id"));
            assertEquals(eventB1, theEvent.get("b"));

            env.undeployModuleContaining("s0");

            // try wildcard
            stmt = "@name('s0') select * from pattern [a=SupportBean_A until b=SupportBean_B]";
            env.compileDeploy(stmt).addListener("s0");

            env.sendEventBean(eventA1);
            env.sendEventBean(eventA2);
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(eventB1);

            theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("a"), new Object[]{eventA1, eventA2});
            assertSame(eventA1, theEvent.get("a[0]"));
            assertSame(eventA2, theEvent.get("a[1]"));
            assertNull(theEvent.get("a[2]"));
            assertEquals("A1", theEvent.get("a[0].id"));
            assertEquals("A2", theEvent.get("a[1].id"));
            assertNull(null, theEvent.get("a[2].id"));
            assertSame(eventB1, theEvent.get("b"));

            env.undeployAll();
        }
    }

    private static class PatternUseFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt;
            EventBean theEvent;

            stmt = "@name('s0') select * from pattern [a=SupportBean_A until b=SupportBean_B -> c=SupportBean_C(id = ('C' || a[0].id || a[1].id || b.id))]";
            env.compileDeploy(stmt).addListener("s0");

            Object eventA1 = new SupportBean_A("A1");
            env.sendEventBean(eventA1);

            env.milestone(0);

            Object eventA2 = new SupportBean_A("A2");
            env.sendEventBean(eventA2);

            env.milestone(1);

            Object eventB1 = new SupportBean_B("B1");
            env.sendEventBean(eventB1);

            env.milestone(2);

            env.sendEventBean(new SupportBean_C("C1"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            Object eventC1 = new SupportBean_C("CA1A2B1");
            env.sendEventBean(eventC1);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(eventA1, theEvent.get("a[0]"));
            assertEquals(eventA2, theEvent.get("a[1]"));
            assertNull(theEvent.get("a[2]"));
            assertEquals(eventB1, theEvent.get("b"));
            assertEquals(eventC1, theEvent.get("c"));
            env.undeployAll();

            // Test equals-optimization with array event
            stmt = "@name('s0') select * from pattern [a=SupportBean_A until b=SupportBean_B -> c=SupportBean(theString = a[1].id)]";
            env.compileDeploy(stmt).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_B("B1"));

            env.sendEventBean(new SupportBean("A3", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("A2", 10));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(10, theEvent.get("c.intPrimitive"));
            env.undeployAll();

            // Test in-optimization
            stmt = "@name('s0') select * from pattern [a=SupportBean_A until b=SupportBean_B -> c=SupportBean(theString in(a[2].id))]";
            env.compileDeploy(stmt).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_A("A3"));
            env.sendEventBean(new SupportBean_B("B1"));

            env.sendEventBean(new SupportBean("A2", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("A3", 5));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(5, theEvent.get("c.intPrimitive"));
            env.undeployAll();

            // Test not-in-optimization
            stmt = "@name('s0') select * from pattern [a=SupportBean_A until b=SupportBean_B -> c=SupportBean(theString!=a[0].id and theString!=a[1].id and theString!=a[2].id)]";
            env.compileDeploy(stmt).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_A("A3"));
            env.sendEventBean(new SupportBean_B("B1"));

            env.sendEventBean(new SupportBean("A2", 20));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("A1", 20));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("A6", 5));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(5, theEvent.get("c.intPrimitive"));
            env.undeployAll();

            // Test range-optimization
            stmt = "@name('s0') select * from pattern [a=SupportBean(theString like 'A%') until b=SupportBean(theString like 'B%') -> c=SupportBean(intPrimitive between a[0].intPrimitive and a[1].intPrimitive)]";
            env.compileDeploy(stmt).addListener("s0");

            env.sendEventBean(new SupportBean("A1", 5));
            env.sendEventBean(new SupportBean("A2", 8));
            env.sendEventBean(new SupportBean("B1", -1));

            env.sendEventBean(new SupportBean("E1", 20));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("E2", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E3", 5));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(5, theEvent.get("c.intPrimitive"));

            env.undeployAll();
        }
    }

    private static class PatternRepeatUseTags implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select * from pattern [every [2] (a=SupportBean_A() -> b=SupportBean_B(id=a.id))]";

            env.compileDeploy(stmt);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_B("A2"));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();

            // test with timer:interval
            env.advanceTime(0);
            String query = "@name('s0') select * from pattern [every ([2:]e1=SupportBean(theString='2') until timer:interval(5))->([2:]e2=SupportBean(theString='3') until timer:interval(2))]";
            env.compileDeploy(query).addListener("s0");

            env.sendEventBean(new SupportBean("2", 0));
            env.sendEventBean(new SupportBean("2", 0));
            env.advanceTime(5000);

            env.sendEventBean(new SupportBean("3", 0));
            env.sendEventBean(new SupportBean("3", 0));
            env.sendEventBean(new SupportBean("3", 0));
            env.sendEventBean(new SupportBean("3", 0));
            env.advanceTime(10000);

            env.sendEventBean(new SupportBean("2", 0));
            env.sendEventBean(new SupportBean("2", 0));
            env.advanceTime(15000);

            // test followed by 3 streams
            env.undeployAll();

            String epl = "@name('s0') select * from pattern [ every [2] A=SupportBean(theString='1') " +
                "-> [2] B=SupportBean(theString='2' and intPrimitive=A[0].intPrimitive)" +
                "-> [2] C=SupportBean(theString='3' and intPrimitive=A[0].intPrimitive)]";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("1", 10));
            env.sendEventBean(new SupportBean("1", 20));
            env.sendEventBean(new SupportBean("2", 10));
            env.sendEventBean(new SupportBean("2", 10));
            env.sendEventBean(new SupportBean("3", 10));
            env.sendEventBean(new SupportBean("3", 10));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternArrayFunctionRepeat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select SupportStaticMethodLib.arrayLength(a) as length, java.lang.reflect.Array.getLength(a) as l2 from pattern [[1:] a=SupportBean_A until SupportBean_B]";

            env.compileDeploy(stmt);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_A("A3"));
            env.sendEventBean(new SupportBean_B("A2"));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(3, theEvent.get("length"));
            assertEquals(3, theEvent.get("l2"));

            env.undeployAll();
        }
    }

    private static class PatternExpressionBounds implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test variables - closed bounds
            env.runtime().getVariableService().setVariableValue(null, "lower", 2);
            env.runtime().getVariableService().setVariableValue(null, "upper", 3);
            String stmtOne = "[lower:upper] a=SupportBean (theString = 'A') until b=SupportBean (theString = 'B')";
            validateStmt(env, stmtOne, 0, false, null);
            validateStmt(env, stmtOne, 1, false, null);
            validateStmt(env, stmtOne, 2, true, 2);
            validateStmt(env, stmtOne, 3, true, 3);
            validateStmt(env, stmtOne, 4, true, 3);
            validateStmt(env, stmtOne, 5, true, 3);

            // test variables - half open
            env.runtime().getVariableService().setVariableValue(null, "lower", 3);
            env.runtime().getVariableService().setVariableValue(null, "upper", null);
            String stmtTwo = "[lower:] a=SupportBean (theString = 'A') until b=SupportBean (theString = 'B')";
            validateStmt(env, stmtTwo, 0, false, null);
            validateStmt(env, stmtTwo, 1, false, null);
            validateStmt(env, stmtTwo, 2, false, null);
            validateStmt(env, stmtTwo, 3, true, 3);
            validateStmt(env, stmtTwo, 4, true, 4);
            validateStmt(env, stmtTwo, 5, true, 5);

            // test variables - half closed
            env.runtime().getVariableService().setVariableValue(null, "lower", null);
            env.runtime().getVariableService().setVariableValue(null, "upper", 2);
            String stmtThree = "[:upper] a=SupportBean (theString = 'A') until b=SupportBean (theString = 'B')";
            validateStmt(env, stmtThree, 0, true, null);
            validateStmt(env, stmtThree, 1, true, 1);
            validateStmt(env, stmtThree, 2, true, 2);
            validateStmt(env, stmtThree, 3, true, 2);
            validateStmt(env, stmtThree, 4, true, 2);
            validateStmt(env, stmtThree, 5, true, 2);

            // test followed-by - bounded
            env.compileDeploy("@Name('s0') select * from pattern [s0=SupportBean_S0 -> [s0.id] b=SupportBean]").addListener("s0");
            env.sendEventBean(new SupportBean_S0(2));
            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "b[0].theString,b[1].theString".split(","), new Object[]{"E1", "E2"});

            env.undeployAll();

            // test substitution parameter
            String epl = "select * from pattern[[?::int] SupportBean]";
            EPCompiled compiled = env.compile(epl);
            env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> prepared.setObject(1, 2)));
            env.undeployAll();

            // test exactly-1
            env.advanceTime(0);
            String eplExact1 = "@name('s0') select * from pattern [a=SupportBean_A -> [1] every (timer:interval(10) and not SupportBean_B)]";
            env.compileDeploy(eplExact1).addListener("s0");

            env.advanceTime(5000);
            env.sendEventBean(new SupportBean_A("A1"));

            env.advanceTime(6000);
            env.sendEventBean(new SupportBean_B("B1"));
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(15999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(16000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.id".split(","), new Object[]{"A1"});

            env.advanceTime(999999);
            assertFalse(env.listener("s0").isInvoked());
            env.undeployAll();

            // test until
            env.advanceTime(1000000);
            String eplUntilOne = "@name('s0') select * from pattern [a=SupportBean_A -> b=SupportBean_B until ([1] every (timer:interval(10) and not SupportBean_C))]";
            env.compileDeploy(eplUntilOne).addListener("s0");

            env.advanceTime(1005000);
            env.sendEventBean(new SupportBean_A("A1"));

            env.advanceTime(1006000);
            env.sendEventBean(new SupportBean_B("B1"));
            env.advanceTime(1014999);
            env.sendEventBean(new SupportBean_B("B2"));
            env.sendEventBean(new SupportBean_C("C1"));
            env.advanceTime(1015000);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(1024998);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(1024999);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.id,b[0].id,b[1].id".split(","), new Object[]{"A1", "B1", "B2"});

            env.advanceTime(1999999);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternBoundRepeatWithNot implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "e[0].intPrimitive,e[1].intPrimitive".split(",");
            String epl = "@name('s0') select * from pattern [every [2] (e = SupportBean(theString='A') and not SupportBean(theString='B'))]";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("A", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean("A", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 2});

            env.sendEventBean(new SupportBean("A", 3));

            env.milestone(1);

            env.sendEventBean(new SupportBean("B", 4));

            env.milestone(2);

            env.sendEventBean(new SupportBean("A", 5));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.sendEventBean(new SupportBean("A", 6));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, 6});

            env.undeployAll();
        }
    }

    private static class PatternInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidPattern(env, "[:0] SupportBean_A until SupportBean_B", "Incorrect range specification, a bounds value of zero or negative value is not allowed ");
            tryInvalidPattern(env, "[10:4] SupportBean_A", "Incorrect range specification, lower bounds value '10' is higher then higher bounds '4'");
            tryInvalidPattern(env, "[-1] SupportBean_A", "Incorrect range specification, a bounds value of zero or negative value is not allowed");
            tryInvalidPattern(env, "[4:6] SupportBean_A", "Variable bounds repeat operator requires an until-expression");
            tryInvalidPattern(env, "[0:0] SupportBean_A", "Incorrect range specification, a bounds value of zero or negative value is not allowed");
            tryInvalidPattern(env, "[0] SupportBean_A", "Incorrect range specification, a bounds value of zero or negative value is not allowed");
            tryInvalidPattern(env, "[1] a=SupportBean_A(a[0].id='a')", "Failed to validate filter expression 'a[0].id=\"a\"': Property named 'a[0].id' is not valid in any stream");
            tryInvalidPattern(env, "a=SupportBean_A -> SupportBean_B(a[0].id='a')", "Failed to validate filter expression 'a[0].id=\"a\"': Property named 'a[0].id' is not valid in any stream");
            tryInvalidPattern(env, "(a=SupportBean_A until c=SupportBean_B) -> c=SupportBean_C", "Tag 'c' for event 'SupportBean_C' has already been declared for events of type " + SupportBean_B.class.getName());
            tryInvalidPattern(env, "((a=SupportBean_A until b=SupportBean_B) until a=SupportBean_A)", "Tag 'a' for event 'SupportBean_A' used in the repeat-until operator cannot also appear in other filter expressions");
            tryInvalidPattern(env, "a=SupportBean -> [a.theString] b=SupportBean", "Match-until bounds value expressions must return a numeric value");
            tryInvalidPattern(env, "a=SupportBean -> [:a.theString] b=SupportBean", "Match-until bounds value expressions must return a numeric value");
            tryInvalidPattern(env, "a=SupportBean -> [a.theString:1] b=SupportBean", "Match-until bounds value expressions must return a numeric value");
        }
    }

    private static void validateStmt(RegressionEnvironment env, String stmtText, int numEventsA, boolean match, Integer matchCount) {

        env.compileDeploy("@name('s0') select * from pattern[" + stmtText + "]").addListener("s0");

        for (int i = 0; i < numEventsA; i++) {
            env.sendEventBean(new SupportBean("A", i));
        }
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportBean("B", -1));

        assertEquals(match, env.listener("s0").isInvoked());
        if (!match) {
            env.undeployAll();
            return;
        }

        Object valueATag = env.listener("s0").assertOneGetNewAndReset().get("a");
        if (matchCount == null) {
            assertNull(valueATag);
        } else {
            assertEquals((int) matchCount, Array.getLength(valueATag));
        }

        env.undeployAll();
    }

    private static void tryInvalidPattern(RegressionEnvironment env, String epl, String message) {
        tryInvalidCompile(env, "select * from pattern[" + epl + "]", message);
    }
}
