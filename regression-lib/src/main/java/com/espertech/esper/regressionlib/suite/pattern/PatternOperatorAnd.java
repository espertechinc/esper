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
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.bean.SupportBean_C;
import com.espertech.esper.regressionlib.support.patternassert.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class PatternOperatorAnd {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOperatorAndSimple());
        execs.add(new PatternOperatorAndWHarness());
        execs.add(new PatternOperatorAndWithEveryAndTerminationOptimization());
        execs.add(new PatternOperatorAndNotDefaultTrue());
        return execs;
    }

    public static class PatternOperatorAndSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");

            String epl = "@Name('s0') select a.theString as c0, b.theString as c1 from pattern [a=SupportBean(intPrimitive=0) and b=SupportBean(intPrimitive=1)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "EB", 1);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBean(env, "EA", 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EA", "EB"});

            env.milestone(2);
            sendSupportBean(env, "EB", 1);
            sendSupportBean(env, "EA", 0);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);
            sendSupportBean(env, "EB", 1);
            sendSupportBean(env, "EA", 0);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternOperatorAndNotDefaultTrue implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // ESPER-402
            String pattern =
                "@name('s0') insert into NumberOfWaitingCalls(calls) " +
                    " select count(*)" +
                    " from pattern[every call=SupportBean_A ->" +
                    " (not SupportBean_B(id=call.id) and" +
                    " not SupportBean_C(id=call.id))]";
            env.compileDeploy(pattern).addListener("s0");
            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            env.sendEventBean(new SupportBean_C("C1"));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternOperatorAndWithEveryAndTerminationOptimization implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // When all other sub-expressions to an AND are gone,
            // then there is no need to retain events of the subexpression still active

            String epl = "@name('s0') select * from pattern [a=SupportBean_A and every b=SupportBean_B]";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBean_A("A1"));
            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean_B("B" + i));
            }

            env.addListener("s0");
            env.sendEventBean(new SupportBean_B("B_last"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[]{"A1", "B_last"});

            env.undeployAll();
        }
    }

    public static class PatternOperatorAndWHarness implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("b=SupportBean_B and d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B and every d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B and d=SupportBean_D");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every(b=SupportBean_B and d=SupportBean_D" + ")");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCaseList.addTest(testCase);

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            PatternExpr pattern = Patterns.every(Patterns.and(Patterns.filter("SupportBean_B", "b"), Patterns.filter("SupportBean_D", "d")));
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model = (EPStatementObjectModel) SerializableObjectCopier.copyMayFail(model);
            assertEquals("select * from pattern [every (b=SupportBean_B and d=SupportBean_D" + ")]", model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every( b=SupportBean_B and every d=SupportBean_D" + ")");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B and every d=SupportBean_D");
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

            testCase = new EventExpressionCase("every( every b=SupportBean_B and d=SupportBean_D" + ")");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every a=SupportBean_A and d=SupportBean_D" + " and b=SupportBean_B");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every( every b=SupportBean_B and every d=SupportBean_D" + ")");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
            testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
            for (int i = 0; i < 3; i++) {
                testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
            }
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
            testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
            for (int i = 0; i < 5; i++) {
                testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            }
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A and d=SupportBean_D" + " and b=SupportBean_B");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every a=SupportBean_A and every d=SupportBean_D" + " and b=SupportBean_B");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B and b=SupportBean_B");
            testCase.add("B1", "b", events.getEvent("B1"), "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every a=SupportBean_A and every d=SupportBean_D" + " and every b=SupportBean_B");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
            testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
            testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
            testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
            testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (a=SupportBean_A and every d=SupportBean_D" + " and b=SupportBean_B)");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
            testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
            testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B and b=SupportBean_B)");
            testCase.add("B1", "b", events.getEvent("B1"), "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"), "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"), "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}