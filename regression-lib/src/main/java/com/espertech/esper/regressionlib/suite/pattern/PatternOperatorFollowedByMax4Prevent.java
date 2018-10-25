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
import com.espertech.esper.common.client.hook.condition.ConditionPatternRuntimeSubexpressionMax;
import com.espertech.esper.common.client.hook.condition.ConditionPatternSubexpressionMax;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatternOperatorFollowedByMax4Prevent implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        ConditionHandlerFactoryContext context = SupportConditionHandlerFactory.getFactoryContexts().get(0);
        assertEquals(env.runtimeURI(), context.getRuntimeURI());
        SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

        runAssertionFollowedWithMax(env, handler);
        runAssertionTwoStatementsAndStopDestroy(env, handler);
    }

    private static void runAssertionFollowedWithMax(RegressionEnvironment env, SupportConditionHandlerFactory.SupportConditionHandler handler) {
        String expressionOne = "@Name('S1') select * from pattern [every a=SupportBean(theString like 'A%') -[2]> b=SupportBean_A(id=a.theString)]";
        env.compileDeploy(expressionOne).addListener("S1");

        String expressionTwo = "@Name('S2') select * from pattern [every a=SupportBean(theString like 'B%') -> b=SupportBean_B(id=a.theString)]";
        env.compileDeploy(expressionTwo).addListener("S2");

        env.sendEventBean(new SupportBean("A1", 0));
        env.sendEventBean(new SupportBean("A2", 0));
        env.sendEventBean(new SupportBean("B1", 0));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("A3", 0));
        assertContextStatement(env, env.statement("S1"), handler.getAndResetContexts(), 2);

        env.sendEventBean(new SupportBean("B2", 0));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("B3", 0));
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 2, "S2", 2));

        env.sendEventBean(new SupportBean_A("A2"));
        env.sendEventBean(new SupportBean("B4", 0));   // now A1, B1, B2, B4
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("A3", 0));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 1, "S2", 3));

        env.undeployModuleContaining("S1");

        env.sendEventBean(new SupportBean("B4", 0));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("B5", 0));
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 4, getExpectedCountMap("S2", 4));

        env.undeployAll();
    }

    private static void runAssertionTwoStatementsAndStopDestroy(RegressionEnvironment env, SupportConditionHandlerFactory.SupportConditionHandler handler) {
        String expressionOne = "@Name('S1') select * from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean_A(id=a.theString)]";
        env.compileDeploy(expressionOne).addListener("S1");

        String expressionTwo = "@Name('S2') select * from pattern [every a=SupportBean(theString like 'B%') -> b=SupportBean_B(id=a.theString)]";
        env.compileDeploy(expressionTwo).addListener("S2");

        env.sendEventBean(new SupportBean("A1", 0));
        env.sendEventBean(new SupportBean("A2", 0));
        env.sendEventBean(new SupportBean("A3", 0));
        env.sendEventBean(new SupportBean("B1", 0));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("B2", 0));
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 3, "S2", 1));

        handler = SupportConditionHandlerFactory.getLastHandler();

        env.sendEventBean(new SupportBean("A4", 0));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 3, "S2", 1));

        env.undeployModuleContaining("S1");

        env.sendEventBean(new SupportBean("B3", 0));
        env.sendEventBean(new SupportBean("B4", 0));
        env.sendEventBean(new SupportBean("B5", 0));
        assertTrue(handler.getContexts().isEmpty());

        handler = SupportConditionHandlerFactory.getLastHandler();

        env.sendEventBean(new SupportBean("B6", 0));
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 4, getExpectedCountMap("S2", 4));

        env.sendEventBean(new SupportBean("B7", 0));
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 4, getExpectedCountMap("S2", 4));

        env.undeployAll();
    }

    protected static Map<String, Long> getExpectedCountMap(String statementName, long count) {
        Map<String, Long> result = new HashMap<>();
        result.put(statementName, count);
        return result;
    }

    protected static Map<String, Long> getExpectedCountMap(String stmtOne, long countOne, String stmtTwo, long countTwo) {
        Map<String, Long> result = new HashMap<>();
        result.put(stmtOne, countOne);
        result.put(stmtTwo, countTwo);
        return result;
    }

    protected static void assertContextEnginePool(RegressionEnvironment env, EPStatement stmt, List<ConditionHandlerContext> contexts, int max, Map<String, Long> counts) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(env.runtimeURI(), context.getRuntimeURI());
        assertEquals(stmt.getDeploymentId(), context.getDeploymentId());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionPatternRuntimeSubexpressionMax condition = (ConditionPatternRuntimeSubexpressionMax) context.getCondition();
        assertEquals(max, condition.getMax());
        assertEquals(counts.size(), condition.getCounts().size());
        for (Map.Entry<String, Long> expected : counts.entrySet()) {
            assertEquals("failed for key " + expected.getKey(), expected.getValue(), condition.getCounts().get(expected.getKey()));
        }
        contexts.clear();
    }

    protected static void assertContextStatement(RegressionEnvironment env, EPStatement stmt, List<ConditionHandlerContext> contexts, int max) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(env.runtimeURI(), context.getRuntimeURI());
        assertEquals(stmt.getDeploymentId(), context.getDeploymentId());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionPatternSubexpressionMax condition = (ConditionPatternSubexpressionMax) context.getCondition();
        assertEquals(max, condition.getMax());
        contexts.clear();
    }
}
