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

import com.espertech.esper.common.client.hook.condition.ConditionHandlerContext;
import com.espertech.esper.common.client.hook.condition.ConditionHandlerFactoryContext;
import com.espertech.esper.common.client.hook.condition.ConditionPatternSubexpressionMax;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.bean.SupportBean_C;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class PatternOperatorFollowedByMax {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternMultiple());
        execs.add(new PatternMixed());
        execs.add(new PatternSinglePermFalseAndQuit());
        execs.add(new PatternSingleMaxSimple());
        execs.add(new PatternOperatorFollowedByMaxInvalid());
        return execs;
    }

    private static class PatternOperatorFollowedByMaxInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern[a=SupportBean_A -[a.intPrimitive]> SupportBean_B]",
                "Invalid maximum expression in followed-by, event properties are not allowed within the expression [select * from pattern[a=SupportBean_A -[a.intPrimitive]> SupportBean_B]]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from pattern[a=SupportBean_A -[false]> SupportBean_B]",
                "Invalid maximum expression in followed-by, the expression must return an integer value [select * from pattern[a=SupportBean_A -[false]> SupportBean_B]]");
        }
    }

    private static class PatternMultiple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

            String expression = "@name('s0') select a.id as a, b.id as b, c.id as c from pattern [" +
                "every a=SupportBean_A -[2]> b=SupportBean_B -[3]> c=SupportBean_C]";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = new String[]{"a", "b", "c"};

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_B("B1"));
            env.sendEventBean(new SupportBean_A("A3"));
            env.sendEventBean(new SupportBean_A("A4"));
            assertTrue(handler.getContexts().isEmpty());

            env.sendEventBean(new SupportBean_B("B2"));
            assertContext(env, handler.getContexts(), 3);

            env.sendEventBean(new SupportBean_C("C1"));
            assertTrue(handler.getContexts().isEmpty());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1", "C1"}, {"A2", "B1", "C1"}, {"A3", "B2", "C1"}});

            env.undeployAll();
        }
    }

    private static class PatternMixed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

            String expression = "@name('s0') select a.id as a, b.id as b, c.id as c from pattern [" +
                "every a=SupportBean_A -> b=SupportBean_B -[2]> c=SupportBean_C]";
            env.compileDeploy(expression).addListener("s0");

            tryAssertionMixed(env, handler);

            // test SODA
            env.undeployAll();

            env.eplToModelCompileDeploy(expression).addListener("s0");

            tryAssertionMixed(env, handler);

            env.undeployAll();
        }

        private void tryAssertionMixed(RegressionEnvironment env, SupportConditionHandlerFactory.SupportConditionHandler handler) {

            String[] fields = new String[]{"a", "b", "c"};

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_A("A3"));

            handler.getContexts().clear();
            env.sendEventBean(new SupportBean_B("B1"));
            assertContext(env, handler.getContexts(), 2);

            env.sendEventBean(new SupportBean_C("C1"));
            assertTrue(handler.getContexts().isEmpty());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1", "C1"}, {"A2", "B1", "C1"}});
        }
    }

    private static class PatternSinglePermFalseAndQuit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            ConditionHandlerFactoryContext context = SupportConditionHandlerFactory.getFactoryContexts().get(0);
            Assert.assertEquals("default", context.getRuntimeURI());
            SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();
            handler.getAndResetContexts();

            // not-operator
            String expression = "@name('s0') select a.id as a, b.id as b from pattern [every a=SupportBean_A -[2]> (b=SupportBean_B and not SupportBean_C)]";
            env.compileDeploy(expression).addListener("s0");
            String[] fields = new String[]{"a", "b"};

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.sendEventBean(new SupportBean_C("C1"));

            env.sendEventBean(new SupportBean_A("A3"));
            env.sendEventBean(new SupportBean_A("A4"));
            env.sendEventBean(new SupportBean_B("B1"));
            assertTrue(handler.getContexts().isEmpty());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A3", "B1"}, {"A4", "B1"}});

            env.sendEventBean(new SupportBean_A("A5"));
            env.sendEventBean(new SupportBean_A("A6"));
            env.sendEventBean(new SupportBean_A("A7"));
            assertContext(env, handler.getContexts(), 2);
            env.undeployAll();

            // guard
            String expressionTwo = "@name('s0') select a.id as a, b.id as b from pattern [every a=SupportBean_A -[2]> (b=SupportBean_B where timer:within(1))]";
            env.compileDeploy(expressionTwo).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));
            env.advanceTime(2000); // expires sub-expressions
            assertTrue(handler.getContexts().isEmpty());

            env.sendEventBean(new SupportBean_A("A3"));
            env.sendEventBean(new SupportBean_A("A4"));
            env.sendEventBean(new SupportBean_B("B1"));
            assertTrue(handler.getContexts().isEmpty());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A3", "B1"}, {"A4", "B1"}});

            env.sendEventBean(new SupportBean_A("A5"));
            env.sendEventBean(new SupportBean_A("A6"));
            env.sendEventBean(new SupportBean_A("A7"));
            assertContext(env, handler.getContexts(), 2);

            env.undeployAll();

            // every-operator
            String expressionThree = "@name('s0') select a.id as a, b.id as b from pattern [every a=SupportBean_A -[2]> (every b=SupportBean_B(id=a.id) and not SupportBean_C(id=a.id))]";
            env.compileDeploy(expressionThree).addListener("s0");

            env.sendEventBean(new SupportBean_A("1"));
            env.sendEventBean(new SupportBean_A("2"));

            env.sendEventBean(new SupportBean_B("1"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"1", "1"}});

            env.sendEventBean(new SupportBean_B("2"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"2", "2"}});

            env.sendEventBean(new SupportBean_C("1"));

            env.sendEventBean(new SupportBean_A("3"));
            env.sendEventBean(new SupportBean_B("3"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"3", "3"}});

            env.undeployAll();
        }
    }

    private static class PatternSingleMaxSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            ConditionHandlerFactoryContext context = SupportConditionHandlerFactory.getFactoryContexts().get(0);
            Assert.assertEquals(env.runtimeURI(), context.getRuntimeURI());
            SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

            String expression = "@name('s0') select a.id as a, b.id as b from pattern [every a=SupportBean_A -[2]> b=SupportBean_B]";
            env.compileDeploy(expression).addListener("s0");
            runAssertionSingleMaxSimple(env, handler);
            env.undeployAll();

            // test SODA
            env.eplToModelCompileDeploy(expression).addListener("s0");
            runAssertionSingleMaxSimple(env, handler);
            env.undeployAll();

            // test variable
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int myvar=3", path);
            expression = "@name('s0') select a.id as a, b.id as b from pattern [every a=SupportBean_A -[myvar-1]> b=SupportBean_B]";
            env.compileDeploy(expression, path).addListener("s0");
            runAssertionSingleMaxSimple(env, handler);

            env.undeployAll();
        }
    }

    private static void runAssertionSingleMaxSimple(RegressionEnvironment env, SupportConditionHandlerFactory.SupportConditionHandler handler) {

        String[] fields = new String[]{"a", "b"};

        env.sendEventBean(new SupportBean_A("A1"));
        env.sendEventBean(new SupportBean_A("A2"));

        handler.getContexts().clear();
        env.sendEventBean(new SupportBean_A("A3"));
        assertContext(env, handler.getContexts(), 2);

        env.sendEventBean(new SupportBean_B("B1"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}});

        env.sendEventBean(new SupportBean_A("A4"));
        env.sendEventBean(new SupportBean_B("B2"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A4", "B2"}});
        assertTrue(handler.getContexts().isEmpty());

        for (int i = 5; i < 9; i++) {
            env.sendEventBean(new SupportBean_A("A" + i));
            if (i >= 7) {
                assertContext(env, handler.getContexts(), 2);
            }
        }

        env.sendEventBean(new SupportBean_B("B3"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A5", "B3"}, {"A6", "B3"}});

        env.sendEventBean(new SupportBean_B("B4"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_A("A20"));
        env.sendEventBean(new SupportBean_A("A21"));
        env.sendEventBean(new SupportBean_B("B5"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A20", "B5"}, {"A21", "B5"}});
        assertTrue(handler.getContexts().isEmpty());
    }

    private static void assertContext(RegressionEnvironment env, List<ConditionHandlerContext> contexts, int max) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        Assert.assertEquals("default", context.getRuntimeURI());
        Assert.assertEquals(env.statement("s0").getDeploymentId(), context.getDeploymentId());
        Assert.assertEquals("s0", context.getStatementName());
        ConditionPatternSubexpressionMax condition = (ConditionPatternSubexpressionMax) context.getCondition();
        Assert.assertEquals(max, condition.getMax());
        contexts.clear();
    }

    private final static Logger log = LoggerFactory.getLogger(PatternOperatorFollowedByMax.class);
}