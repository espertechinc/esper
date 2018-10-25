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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PatternOperatorOr {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOrSimple());
        execs.add(new PatternOrAndNotAndZeroStart());
        execs.add(new PatternOperatorOrWHarness());
        return execs;
    }

    public static class PatternOrSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");

            String epl = "@Name('s0') select a.theString as c0, b.theString as c1 from pattern [a=SupportBean(intPrimitive=0) or b=SupportBean(intPrimitive=1)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "EB", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, "EB"});

            env.milestone(1);

            sendSupportBean(env, "EA", 0);
            sendSupportBean(env, "EB", 1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            env.undeployAll();
        }
    }

    private static class PatternOrAndNotAndZeroStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryOrAndNot(env, "(a=SupportBean_A -> b=SupportBean_B) or (a=SupportBean_A -> not b=SupportBean_B)");
            tryOrAndNot(env, "a=SupportBean_A -> (b=SupportBean_B or not SupportBean_B)");

            // try zero-time start
            env.advanceTime(0);
            SupportUpdateListener listener = new SupportUpdateListener();
            env.compileDeploy("@name('s0') select * from pattern [timer:interval(0) or every timer:interval(1 min)]").statement("s0").addListenerWithReplay(listener);
            assertTrue(listener.isInvoked());
            env.undeployAll();
        }
    }

    private static class PatternOperatorOrWHarness implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("a=SupportBean_A or a=SupportBean_A");
            testCase.add("A1", "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A or b=SupportBean_B" + " or c=SupportBean_C");
            testCase.add("A1", "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B" + " or every d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("D1", "d", events.getEvent("D1"));
            testCase.add("D2", "d", events.getEvent("D2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("D3", "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A or b=SupportBean_B");
            testCase.add("A1", "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A or every b=SupportBean_B");
            testCase.add("A1", "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every a=SupportBean_A or d=SupportBean_D");
            testCase.add("A1", "a", events.getEvent("A1"));
            testCase.add("A2", "a", events.getEvent("A2"));
            testCase.add("D1", "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (every b=SupportBean_B" + "() or d=SupportBean_D" + "())");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B2", "b", events.getEvent("B2"));
            for (int i = 0; i < 4; i++) {
                testCase.add("D1", "d", events.getEvent("D1"));
            }
            for (int i = 0; i < 4; i++) {
                testCase.add("D2", "d", events.getEvent("D2"));
            }
            for (int i = 0; i < 4; i++) {
                testCase.add("B3", "b", events.getEvent("B3"));
            }
            for (int i = 0; i < 8; i++) {
                testCase.add("D3", "d", events.getEvent("D3"));
            }
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B" + "() or every d=SupportBean_D" + "())");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("D1", "d", events.getEvent("D1"));
            testCase.add("D2", "d", events.getEvent("D2"));
            testCase.add("D2", "d", events.getEvent("D2"));
            for (int i = 0; i < 4; i++) {
                testCase.add("B3", "b", events.getEvent("B3"));
            }
            for (int i = 0; i < 4; i++) {
                testCase.add("D3", "d", events.getEvent("D3"));
            }
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (every d=SupportBean_D" + "() or every b=SupportBean_B" + "())");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B2", "b", events.getEvent("B2"));
            for (int i = 0; i < 4; i++) {
                testCase.add("D1", "d", events.getEvent("D1"));
            }
            for (int i = 0; i < 8; i++) {
                testCase.add("D2", "d", events.getEvent("D2"));
            }
            for (int i = 0; i < 16; i++) {
                testCase.add("B3", "b", events.getEvent("B3"));
            }
            for (int i = 0; i < 32; i++) {
                testCase.add("D3", "d", events.getEvent("D3"));
            }
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static void tryOrAndNot(RegressionEnvironment env, String pattern) {
        String expression = "@name('s0') select * " + "from pattern [" + pattern + "]";
        env.compileDeploy(expression);
        env.addListener("s0");

        Object eventA1 = new SupportBean_A("A1");
        env.sendEventBean(eventA1);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        Assert.assertEquals(eventA1, theEvent.get("a"));
        assertNull(theEvent.get("b"));

        Object eventB1 = new SupportBean_B("B1");
        env.sendEventBean(eventB1);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        Assert.assertEquals(eventA1, theEvent.get("a"));
        Assert.assertEquals(eventB1, theEvent.get("b"));

        env.undeployAll();
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}