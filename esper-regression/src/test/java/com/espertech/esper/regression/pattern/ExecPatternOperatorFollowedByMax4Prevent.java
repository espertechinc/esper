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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.ConditionHandlerContext;
import com.espertech.esper.client.hook.ConditionHandlerFactoryContext;
import com.espertech.esper.client.hook.ConditionPatternEngineSubexpressionMax;
import com.espertech.esper.client.hook.ConditionPatternSubexpressionMax;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.client.SupportConditionHandlerFactory;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecPatternOperatorFollowedByMax4Prevent implements RegressionExecution, SupportBeanConstants {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_A", SupportBean_A.class);
        configuration.addEventType("SupportBean_B", SupportBean_B.class);
        configuration.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        configuration.getEngineDefaults().getPatterns().setMaxSubexpressions(4L);
        configuration.getEngineDefaults().getPatterns().setMaxSubexpressionPreventStart(true);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecPatternOperatorFollowedByMax4Prevent.class)) {
            return;
        }
        ConditionHandlerFactoryContext context = SupportConditionHandlerFactory.getFactoryContexts().get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

        runAssertionFollowedWithMax(epService, handler);
        runAssertionTwoStatementsAndStopDestroy(epService, handler);
    }

    private void runAssertionFollowedWithMax(EPServiceProvider epService, SupportConditionHandlerFactory.SupportConditionHandler handler) {
        String expressionOne = "@Name('S1') select * from pattern [every a=SupportBean(theString like 'A%') -[2]> b=SupportBean_A(id=a.theString)]";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(expressionOne);

        String expressionTwo = "@Name('S2') select * from pattern [every a=SupportBean(theString like 'B%') -> b=SupportBean_B(id=a.theString)]";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(expressionTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 0));
        assertContextStatement(epService, stmtOne, handler.getAndResetContexts(), 2);

        epService.getEPRuntime().sendEvent(new SupportBean("B2", 0));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("B3", 0));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 2, "S2", 2));

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean("B4", 0));   // now A1, B1, B2, B4
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 0));
        assertContextEnginePool(epService, stmtOne, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 1, "S2", 3));

        stmtOne.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("B4", 0));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("B5", 0));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, getExpectedCountMap("S2", 4));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTwoStatementsAndStopDestroy(EPServiceProvider epService, SupportConditionHandlerFactory.SupportConditionHandler handler) {
        String expressionOne = "@Name('S1') select * from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean_A(id=a.theString)]";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(expressionOne);

        String expressionTwo = "@Name('S2') select * from pattern [every a=SupportBean(theString like 'B%') -> b=SupportBean_B(id=a.theString)]";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(expressionTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("B2", 0));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 3, "S2", 1));

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 0));
        assertContextEnginePool(epService, stmtOne, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 3, "S2", 1));

        stmtOne.stop();

        epService.getEPRuntime().sendEvent(new SupportBean("B3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B4", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B5", 0));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("B6", 0));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 0, "S2", 4));

        stmtOne.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("B7", 0));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, getExpectedCountMap("S2", 4));
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

    protected static void assertContextEnginePool(EPServiceProvider epService, EPStatement stmt, List<ConditionHandlerContext> contexts, int max, Map<String, Long> counts) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        assertEquals(stmt.getText(), context.getEpl());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionPatternEngineSubexpressionMax condition = (ConditionPatternEngineSubexpressionMax) context.getEngineCondition();
        assertEquals(max, condition.getMax());
        assertEquals(counts.size(), condition.getCounts().size());
        for (Map.Entry<String, Long> expected : counts.entrySet()) {
            assertEquals("failed for key " + expected.getKey(), expected.getValue(), condition.getCounts().get(expected.getKey()));
        }
        contexts.clear();
    }

    protected static void assertContextStatement(EPServiceProvider epService, EPStatement stmt, List<ConditionHandlerContext> contexts, int max) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        assertEquals(stmt.getText(), context.getEpl());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionPatternSubexpressionMax condition = (ConditionPatternSubexpressionMax) context.getEngineCondition();
        assertEquals(max, condition.getMax());
        contexts.clear();
    }
}
