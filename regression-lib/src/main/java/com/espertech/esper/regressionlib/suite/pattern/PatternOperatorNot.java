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
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.patternassert.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class PatternOperatorNot {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternOperatorNotWHarness());
        execs.add(new PatternOp());
        execs.add(new PatternUniformEvents());
        execs.add(new PatternNotFollowedBy());
        execs.add(new PatternNotTimeInterval());
        execs.add(new PatternNotWithEvery());
        return execs;
    }

    private static class PatternOperatorNotWHarness implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("b=SupportBean_B and not d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            String text = "select * from pattern [every b=SupportBean_B and not g=SupportBean_G]";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            PatternExpr pattern = Patterns.and()
                .add(Patterns.everyFilter("SupportBean_B", "b"))
                .add(Patterns.notFilter("SupportBean_G", "g"));
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model = (EPStatementObjectModel) SerializableObjectCopier.copyMayFail(model);
            assertEquals(text, model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B and not g=SupportBean_G");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B and not d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B and not a=SupportBean_A(id='A1')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B and not a2=SupportBean_A(id='A2')");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B and not b3=SupportBean_B(id='B3'))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B or not SupportBean_D())");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (every b=SupportBean_B and not SupportBean_B(id='B2'))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B and not SupportBean_B(id='B2'))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_A");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_G");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_G");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_G(id='x')");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("b=SupportBean_B and not d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            String text = "select * from pattern [every b=SupportBean_B and not g=SupportBean_G]";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            PatternExpr pattern = Patterns.and()
                .add(Patterns.everyFilter("SupportBean_B", "b"))
                .add(Patterns.notFilter("SupportBean_G", "g"));
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model = SerializableObjectCopier.copyMayFail(model);
            Assert.assertEquals(text, model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B and not g=SupportBean_G");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every b=SupportBean_B and not d=SupportBean_D");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B and not a=SupportBean_A(id='A1')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B and not a2=SupportBean_A(id='A2')");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B and not b3=SupportBean_B(id='B3'))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B2", "b", events.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B or not SupportBean_D())");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (every b=SupportBean_B and not SupportBean_B(id='B2'))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B and not SupportBean_B(id='B2'))");
            testCase.add("B1", "b", events.getEvent("B1"));
            testCase.add("B3", "b", events.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_A");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("(b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_G");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_G");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every (b=SupportBean_B -> d=SupportBean_D) and " +
                " not SupportBean_G(id='x')");
            testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternUniformEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getSetTwoExternalClock(0, 1000);
            CaseList results = new CaseList();

            EventExpressionCase desc = new EventExpressionCase("every a=SupportBean_A() and not a1=SupportBean_A(id='A4')");
            desc.add("B1", "a", events.getEvent("B1"));
            desc.add("B2", "a", events.getEvent("B2"));
            desc.add("B3", "a", events.getEvent("B3"));
            results.addTest(desc);

            PatternTestHarness util = new PatternTestHarness(events, results, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternNotTimeInterval implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String text = "@name('s0') select A.theString as theString from pattern " +
                "[every A=SupportBean(intPrimitive=123) -> (timer:interval(30 seconds) and not SupportMarketDataBean(volume=123, symbol=A.theString))]";
            env.compileDeploy(text);

            env.addListener("s0");

            sendTimer(0, env);
            env.sendEventBean(new SupportBean("E1", 123));

            sendTimer(10000, env);
            env.sendEventBean(new SupportBean("E2", 123));

            sendTimer(20000, env);
            env.sendEventBean(new SupportMarketDataBean("E1", 0, 123L, ""));

            sendTimer(30000, env);
            env.sendEventBean(new SupportBean("E3", 123));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(40000, env);
            String[] fields = new String[]{"theString"};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class PatternNotFollowedBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtText = "@name('s0') select * from pattern [ every( SupportBean(intPrimitive>0) -> (SupportMarketDataBean and not SupportBean(intPrimitive=0) ) ) ]";
            env.compileDeploy(stmtText);

            env.addListener("s0");

            // A(a=1) A(a=2) A(a=0) A(a=3) ...
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 0));
            env.sendEventBean(new SupportBean("E4", 1));
            env.sendEventBean(new SupportMarketDataBean("E5", "M1", 1d));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    public static class PatternNotWithEvery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");

            String epl = "@Name('s0') select a.theString as c0 from pattern [(every a=SupportBean(intPrimitive>=0)) and not SupportBean(intPrimitive<0)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            sendSupportBean(env, "E2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(1);

            sendSupportBean(env, "E3", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            sendSupportBean(env, "E4", -1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            sendSupportBean(env, "E5", 3);
            sendSupportBean(env, "E6", -1);
            Assert.assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }
}
