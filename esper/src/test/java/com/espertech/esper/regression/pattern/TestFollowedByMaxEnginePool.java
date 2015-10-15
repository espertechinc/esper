/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.ConditionHandlerContext;
import com.espertech.esper.client.hook.ConditionHandlerFactoryContext;
import com.espertech.esper.client.hook.ConditionPatternEngineSubexpressionMax;
import com.espertech.esper.client.hook.ConditionPatternSubexpressionMax;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFollowedByMaxEnginePool extends TestCase implements SupportBeanConstants
{
    private EPServiceProvider epService;
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    protected void tearDown() throws Exception {
        handler = null;
    }

    public void testFollowedWithMax()
    {
        if (SupportConfigFactory.skipTest(TestFollowedByMaxEnginePool.class)) {
            return;
        }

        initService(4L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

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

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testTwoStatementsAndStopDestroy()
    {
        if (SupportConfigFactory.skipTest(TestFollowedByMaxEnginePool.class)) {
            return;
        }

        initService(4, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

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

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSingleNoOperatorMax()
    {
        if (SupportConfigFactory.skipTest(TestFollowedByMaxEnginePool.class)) {
            return;
        }

        initService(2L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String expression = "@Name('A') select a.id as a, b.id as b from pattern [every a=SupportBean_A -> b=SupportBean_B]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 2, getExpectedCountMap("A", 2));

        String fields[] = new String[] {"a", "b"};
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("A4"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A4", "B2"}});
        assertTrue(handler.getContexts().isEmpty());

        for (int i = 5; i < 9; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_A("A" + i));
            if (i >= 7) {
                assertContextEnginePool(epService, stmt, handler.getContexts(), 2, getExpectedCountMap("A", 2));
            }
        }

        epService.getEPRuntime().sendEvent(new SupportBean_B("B3"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A5", "B3"}, {"A6", "B3"}});

        epService.getEPRuntime().sendEvent(new SupportBean_B("B4"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_A("A20"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A21"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B5"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A20", "B5"}, {"A21", "B5"}});
        assertTrue(handler.getContexts().isEmpty());

        stmt.destroy();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNoPreventRuntimeConfig()
    {
        if (SupportConfigFactory.skipTest(TestFollowedByMaxEnginePool.class)) {
            return;
        }

        initService(2L, false);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String expression = "@Name('A') select a.id as a, b.id as b from pattern [every a=SupportBean_A -> b=SupportBean_B]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 2, getExpectedCountMap("A", 2));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A4"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 2, getExpectedCountMap("A", 3));

        String fields[] = new String[] {"a", "b"};
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}, {"A3", "B1"}, {"A4", "B1"}});

        // set new max
        epService.getEPAdministrator().getConfiguration().setPatternMaxSubexpressions(1L);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A5"));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A6"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 1, getExpectedCountMap("A", 1));

        stmt.destroy();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void initService(long max, boolean preventStart) {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportBean_A", SupportBean_A.class);
        config.addEventType("SupportBean_B", SupportBean_B.class);
        config.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        config.getEngineDefaults().getPatterns().setMaxSubexpressions(max);
        config.getEngineDefaults().getPatterns().setMaxSubexpressionPreventStart(preventStart);
        config.getEngineDefaults().getLogging().setEnableExecutionDebug(true);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        ConditionHandlerFactoryContext context = SupportConditionHandlerFactory.getFactoryContexts().get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        handler = SupportConditionHandlerFactory.getLastHandler();
    }

    private static Map<String, Long> getExpectedCountMap(String statementName, long count) {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put(statementName, count);
        return result;
    }

    private static Map<String, Long> getExpectedCountMap(String stmtOne, long countOne, String stmtTwo, long countTwo) {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put(stmtOne, countOne);
        result.put(stmtTwo, countTwo);
        return result;
    }

    private static void assertContextEnginePool(EPServiceProvider epService, EPStatement stmt, List<ConditionHandlerContext> contexts, int max, Map<String, Long> counts) {
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

    private static void assertContextStatement(EPServiceProvider epService, EPStatement stmt, List<ConditionHandlerContext> contexts, int max) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        assertEquals(stmt.getText(), context.getEpl());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionPatternSubexpressionMax condition = (ConditionPatternSubexpressionMax) context.getEngineCondition();
        assertEquals(max, condition.getMax());
        contexts.clear();
    }

    private static Log log = LogFactory.getLog(TestFollowedByMaxEnginePool.class);
}
