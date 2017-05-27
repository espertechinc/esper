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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.ConditionHandlerContext;
import com.espertech.esper.client.hook.ConditionMatchRecognizeStatesMax;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConditionHandlerFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecRowRecogMaxStatesEngineWide3Instance implements RegressionExecution, SupportBeanConstants {
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportBean_S1.class);
        configuration.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        configuration.getEngineDefaults().getMatchRecognize().setMaxStates(3L);
        configuration.getEngineDefaults().getMatchRecognize().setMaxStatesPreventStart(true);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        handler = SupportConditionHandlerFactory.getLastHandler();

        runAssertionTwoStatementNoDelete(epService);
        runAssertionContextPartitionAndOverflow(epService);
        runAssertionNamedWindowInSequenceRemoveEvent(epService);
        runAssertionNamedWindowOutOfSequenceRemoveEvent(epService);
    }

    private void runAssertionTwoStatementNoDelete(EPServiceProvider epService) {
        String[] fields = "c0".split(",");
        String eplOne = "@name('S1') select * from SupportBean(theString='A') " +
                "match_recognize (" +
                "  measures P1.longPrimitive as c0" +
                "  pattern (P1 P2 P3) " +
                "  define " +
                "    P1 as P1.intPrimitive = 1," +
                "    P2 as P2.intPrimitive = 1," +
                "    P3 as P3.intPrimitive = 2 and P3.longPrimitive = P1.longPrimitive" +
                ")";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(eplOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        String eplTwo = "@name('S2') select * from SupportBean(theString='B') " +
                "match_recognize (" +
                "  measures P1.longPrimitive as c0" +
                "  pattern (P1 P2 P3) " +
                "  define " +
                "    P1 as P1.intPrimitive = 1," +
                "    P2 as P2.intPrimitive = 1," +
                "    P3 as P3.intPrimitive = 2 and P3.longPrimitive = P1.longPrimitive" +
                ")";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(makeBean("A", 1, 10)); // A(10):P1->P2
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 11)); // A(10):P1->P2, B(11):P1->P2
        epService.getEPRuntime().sendEvent(makeBean("A", 1, 12)); // A(10):P2->P3, A(12):P1->P2, B(11):P1->P2
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 13)); // would be: A(10):P2->P3, A(12):P1->P2, B(11):P2->P3, B(13):P1->P2
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 2, "S2", 1));

        // terminate B
        epService.getEPRuntime().sendEvent(makeBean("B", 2, 11)); // we have no more B-state
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{11L});

        // should not overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 15));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 16));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 2, "S2", 1));

        // terminate A
        epService.getEPRuntime().sendEvent(makeBean("A", 2, 10)); // we have no more A-state
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{10L});

        // should not overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 17));
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 18));
        epService.getEPRuntime().sendEvent(makeBean("A", 1, 19));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("A", 1, 20));
        assertContextEnginePool(epService, stmtOne, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 1, "S2", 2));

        // terminate B
        epService.getEPRuntime().sendEvent(makeBean("B", 2, 17));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{17L});

        // terminate A
        epService.getEPRuntime().sendEvent(makeBean("A", 2, 19));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{19L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextPartitionAndOverflow(EPServiceProvider epService) {
        String[] fields = "c0".split(",");
        String eplCtx = "create context MyCtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(p10 = s0.p00)";
        epService.getEPAdministrator().createEPL(eplCtx);

        String epl = "@name('S1') context MyCtx select * from SupportBean(theString = context.s0.p00) " +
                "match_recognize (" +
                "  measures P2.theString as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.intPrimitive = 1," +
                "    P2 as P2.intPrimitive = 2" +
                ")";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "C"));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "D"));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("D", 1));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        // terminate a context partition
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "D"));
        epService.getEPRuntime().sendEvent(new SupportBean("D", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E"));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("E", 1));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowInSequenceRemoveEvent(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");

        String namedWindow = "create window MyWindow#keepall as SupportBean";
        epService.getEPAdministrator().createEPL(namedWindow);
        String insert = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(insert);
        String delete = "on SupportBean_S0 delete from MyWindow where theString = p00";
        epService.getEPAdministrator().createEPL(delete);

        String epl = "@name('S1') select * from MyWindow " +
                "match_recognize (" +
                "  partition by theString " +
                "  measures P1.longPrimitive as c0, P2.longPrimitive as c1" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.intPrimitive = 0," +
                "    P2 as P2.intPrimitive = 1" +
                ")";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean("A", 0, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 0, 2));
        epService.getEPRuntime().sendEvent(makeBean("C", 0, 3));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("D", 0, 4));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        // delete A (in-sequence remove)
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(makeBean("D", 0, 5)); // now 3 states: B, C, D
        assertTrue(handler.getContexts().isEmpty());

        // test matching
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 6)); // now 2 states: C, D
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, 6L});

        // no overflows
        epService.getEPRuntime().sendEvent(makeBean("E", 0, 7));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("F", 0, 9));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        // no match expected
        epService.getEPRuntime().sendEvent(makeBean("F", 1, 10));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowOutOfSequenceRemoveEvent(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2".split(",");

        String namedWindow = "create window MyWindow#keepall as SupportBean";
        epService.getEPAdministrator().createEPL(namedWindow);
        String insert = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(insert);
        String delete = "on SupportBean_S0 delete from MyWindow where theString = p00 and intPrimitive = id";
        epService.getEPAdministrator().createEPL(delete);

        String epl = "@name('S1') select * from MyWindow " +
                "match_recognize (" +
                "  partition by theString " +
                "  measures P1.longPrimitive as c0, P2.longPrimitive as c1, P3.longPrimitive as c2" +
                "  pattern (P1 P2 P3) " +
                "  define " +
                "    P1 as P1.intPrimitive = 0," +
                "    P2 as P2.intPrimitive = 1," +
                "    P3 as P3.intPrimitive = 2" +
                ")";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean("A", 0, 1));
        epService.getEPRuntime().sendEvent(makeBean("A", 1, 2));
        epService.getEPRuntime().sendEvent(makeBean("B", 0, 3));
        assertTrue(handler.getContexts().isEmpty());

        // delete A-1 (out-of-sequence remove)
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        epService.getEPRuntime().sendEvent(makeBean("A", 2, 4));
        assertFalse(listener.isInvoked());
        assertTrue(handler.getContexts().isEmpty()); // states: B

        // test overflow
        epService.getEPRuntime().sendEvent(makeBean("C", 0, 5));
        epService.getEPRuntime().sendEvent(makeBean("D", 0, 6));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("E", 0, 7));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        // assert nothing matches for overflowed and deleted
        epService.getEPRuntime().sendEvent(makeBean("E", 1, 8));
        epService.getEPRuntime().sendEvent(makeBean("E", 2, 9));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "C")); // delete c
        epService.getEPRuntime().sendEvent(makeBean("C", 1, 10));
        epService.getEPRuntime().sendEvent(makeBean("C", 2, 11));
        assertFalse(listener.isInvoked());

        // assert match found for B
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 12));
        epService.getEPRuntime().sendEvent(makeBean("B", 2, 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3L, 12L, 13L});

        // no overflow
        epService.getEPRuntime().sendEvent(makeBean("F", 0, 14));
        epService.getEPRuntime().sendEvent(makeBean("G", 0, 15));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("H", 0, 16));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        epService.getEPAdministrator().destroyAllStatements();
    }

    protected static void assertContextEnginePool(EPServiceProvider epService, EPStatement stmt, List<ConditionHandlerContext> contexts, int max, Map<String, Long> counts) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        assertEquals(stmt.getText(), context.getEpl());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionMatchRecognizeStatesMax condition = (ConditionMatchRecognizeStatesMax) context.getEngineCondition();
        assertEquals(max, condition.getMax());
        assertEquals(counts.size(), condition.getCounts().size());
        for (Map.Entry<String, Long> expected : counts.entrySet()) {
            assertEquals("failed for key " + expected.getKey(), expected.getValue(), condition.getCounts().get(expected.getKey()));
        }
        contexts.clear();
    }

    protected static Map<String, Long> getExpectedCountMap(String stmtOne, long countOne, String stmtTwo, long countTwo) {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put(stmtOne, countOne);
        result.put(stmtTwo, countTwo);
        return result;
    }

    protected static Map<String, Long> getExpectedCountMap(String stmtOne, long countOne) {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put(stmtOne, countOne);
        return result;
    }

    protected static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean supportBean = new SupportBean(theString, intPrimitive);
        supportBean.setLongPrimitive(longPrimitive);
        return supportBean;
    }
}
