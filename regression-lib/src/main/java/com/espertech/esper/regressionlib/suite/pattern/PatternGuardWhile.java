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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.patternassert.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class PatternGuardWhile {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternGuardWhileSimple());
        execs.add(new PatternOp());
        execs.add(new PatternVariable());
        execs.add(new PatternInvalid());
        return execs;
    }

    public static class PatternGuardWhileSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");

            String epl = "@Name('s0') select a.theString as c0 from pattern [(every a=SupportBean) while (a.theString like 'E%')]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            sendSupportBean(env, "X");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            sendSupportBean(env, "E3");
            sendSupportBean(env, "X");
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase = null;

            testCase = new EventExpressionCase("a=SupportBean_A -> (every b=SupportBean_B) while(b.id != 'B2')");
            testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("a=SupportBean_A -> (every b=SupportBean_B) while(b.id != 'B3')");
            testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
            testCase.add("B2", "a", events.getEvent("A1"), "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) while(b.id != 'B3')");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            String text = "select * from pattern [(every b=SupportBean_B) while (b.id!=\"B3\")]";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            model = SerializableObjectCopier.copyMayFail(model);
            Expression guardExpr = Expressions.neq("b.id", "B3");
            PatternExpr every = Patterns.every(Patterns.filter(Filter.create("SupportBean_B"), "b"));
            PatternExpr patternGuarded = Patterns.whileGuard(every, guardExpr);
            model.setFromClause(FromClause.create(PatternStream.create(patternGuarded)));
            Assert.assertEquals(text, model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(every b=SupportBean_B) while(b.id != 'B1')");
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('var') create variable boolean myVariable = true", path);

            String expression = "@name('s0') select * from pattern [every a=SupportBean(theString like 'A%') -> (every b=SupportBean(theString like 'B%')) while (myVariable)]";
            env.compileDeploy(expression, path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("A2", 2));
            env.sendEventBean(new SupportBean("B1", 100));
            Assert.assertEquals(2, env.listener("s0").getAndResetLastNewData().length);

            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "myVariable", false);

            env.sendEventBean(new SupportBean("A3", 3));
            env.sendEventBean(new SupportBean("A4", 4));
            env.sendEventBean(new SupportBean("B2", 200));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class PatternInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [every SupportBean while ('abc')]",
                "Invalid parameter for pattern guard 'SupportBean while (\"abc\")': Expression pattern guard requires a single expression as a parameter returning a true or false (boolean) value [select * from pattern [every SupportBean while ('abc')]]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern [every SupportBean while (abc)]",
                "Failed to validate pattern guard expression 'abc': Property named 'abc' is not valid in any stream [select * from pattern [every SupportBean while (abc)]]");
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }
}