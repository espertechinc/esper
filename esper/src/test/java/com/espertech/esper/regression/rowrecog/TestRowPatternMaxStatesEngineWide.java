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

package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.ConditionHandlerContext;
import com.espertech.esper.client.hook.ConditionHandlerFactoryContext;
import com.espertech.esper.client.hook.ConditionMatchRecognizeStatesMax;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRowPatternMaxStatesEngineWide extends TestCase implements SupportBeanConstants
{
    private EPServiceProvider epService;
    private SupportConditionHandlerFactory.SupportConditionHandler handler;
    private SupportUpdateListener listenerOne;
    private SupportUpdateListener listenerTwo;

    public void setUp() throws Exception {
        listenerOne = new SupportUpdateListener();
        listenerTwo = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        handler = null;
    }

    public void testReportDontPreventandRuntimeConfig() {
        String[] fields = "c0".split(",");
        initService(3L, false);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String epl = "@name('S1') select * from SupportBean " +
                "match_recognize (" +
                "  partition by theString " +
                "  measures P1.theString as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.intPrimitive = 1," +
                "    P2 as P2.intPrimitive = 2" +
                ")";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 1));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(new SupportBean("D", 1));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E", 1));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 4));

        epService.getEPRuntime().sendEvent(new SupportBean("D", 2));    // D gone
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"D"});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));    // A gone
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A"});

        epService.getEPRuntime().sendEvent(new SupportBean("C", 2));    // C gone
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"C"});

        epService.getEPRuntime().sendEvent(new SupportBean("F", 1));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("G", 1));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        epService.getEPAdministrator().getConfiguration().setMatchRecognizeMaxStates(4L);

        epService.getEPRuntime().sendEvent(new SupportBean("G", 1));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("H", 1));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 4));

        epService.getEPAdministrator().getConfiguration().setMatchRecognizeMaxStates(null);

        epService.getEPRuntime().sendEvent(new SupportBean("I", 1));
        assertTrue(handler.getContexts().isEmpty());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testTwoStatementNoDelete()
    {
        String[] fields = "c0".split(",");
        initService(3L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

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
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[] {11L});

        // should not overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 15));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 1, 16));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 2, "S2", 1));

        // terminate A
        epService.getEPRuntime().sendEvent(makeBean("A", 2, 10)); // we have no more A-state
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[] {10L});

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
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[] {17L});

        // terminate A
        epService.getEPRuntime().sendEvent(makeBean("A", 2, 19));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[] {19L});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testDataWindowAndStmtStop()
    {
        String[] fields = "c0".split(",");
        initService(4L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String eplOne = "@name('S1') select * from SupportBean(theString = 'A') " +
                "match_recognize (" +
                "  partition by intPrimitive " +
                "  measures P2.intPrimitive as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.longPrimitive = 1," +
                "    P2 as P2.longPrimitive = 2" +
                ")";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(eplOne);
        stmtOne.addListener(listenerOne);

        String eplTwo = "@name('S2') select * from SupportBean(theString = 'B').win:length(2) " +
                "match_recognize (" +
                "  partition by intPrimitive " +
                "  measures P2.intPrimitive as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.longPrimitive = 1," +
                "    P2 as P2.longPrimitive = 2" +
                ")";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        stmtTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(makeBean("A", 100, 1));
        epService.getEPRuntime().sendEvent(makeBean("A", 200, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 100, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 200, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 300, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 400, 1));
        EPAssertionUtil.iteratorToArray(stmtTwo.iterator());
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("A", 300, 1));
        assertContextEnginePool(epService, stmtOne, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 2, "S2", 2));

        // terminate B
        epService.getEPRuntime().sendEvent(makeBean("B", 400, 2));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[] {400});

        // terminate one of A
        epService.getEPRuntime().sendEvent(makeBean("A", 100, 2));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[] {100});

        // fill up A
        epService.getEPRuntime().sendEvent(makeBean("A", 300, 1));
        epService.getEPRuntime().sendEvent(makeBean("A", 400, 1));
        epService.getEPRuntime().sendEvent(makeBean("A", 500, 1));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("B", 500, 1));
        assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, getExpectedCountMap("S1", 4, "S2", 0));

        // destroy statement-1 freeing up all "A"
        stmtOne.destroy();

        // any number of B doesn't trigger overflow because of data window
        epService.getEPRuntime().sendEvent(makeBean("B", 600, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 700, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 800, 1));
        epService.getEPRuntime().sendEvent(makeBean("B", 900, 1));
        assertTrue(handler.getContexts().isEmpty());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testContextPartitionAndOverflow()
    {
        String[] fields = "c0".split(",");
        initService(3L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

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
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A"});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNamedWindowInSequenceRemoveEvent()
    {
        String[] fields = "c0,c1".split(",");
        initService(3L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String namedWindow = "create window MyWindow.win:keepall() as SupportBean";
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
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {2L, 6L});

        // no overflows
        epService.getEPRuntime().sendEvent(makeBean("E", 0, 7));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("F", 0, 9));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        // no match expected
        epService.getEPRuntime().sendEvent(makeBean("F", 1, 10));
        assertFalse(listener.isInvoked());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNamedWindowOutOfSequenceRemoveEvent()
    {
        String[] fields = "c0,c1,c2".split(",");
        initService(3L, true);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String namedWindow = "create window MyWindow.win:keepall() as SupportBean";
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
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L, 12L, 13L});

        // no overflow
        epService.getEPRuntime().sendEvent(makeBean("F", 0, 14));
        epService.getEPRuntime().sendEvent(makeBean("G", 0, 15));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(makeBean("H", 0, 16));
        assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, getExpectedCountMap("S1", 3));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void initService(long max, boolean preventStart) {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(SupportBean.class);
        config.addEventType(SupportBean_S0.class);
        config.addEventType(SupportBean_S1.class);
        config.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        config.getEngineDefaults().getMatchRecognize().setMaxStates(max);
        config.getEngineDefaults().getMatchRecognize().setMaxStatesPreventStart(preventStart);
        config.getEngineDefaults().getLogging().setEnableExecutionDebug(true);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        ConditionHandlerFactoryContext context = SupportConditionHandlerFactory.getFactoryContexts().get(0);
        assertEquals(epService.getURI(), context.getEngineURI());
        handler = SupportConditionHandlerFactory.getLastHandler();
    }

    private static void assertContextEnginePool(EPServiceProvider epService, EPStatement stmt, List<ConditionHandlerContext> contexts, int max, Map<String, Long> counts) {
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

    private static Map<String, Long> getExpectedCountMap(String stmtOne, long countOne, String stmtTwo, long countTwo) {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put(stmtOne, countOne);
        result.put(stmtTwo, countTwo);
        return result;
    }

    private static Map<String, Long> getExpectedCountMap(String stmtOne, long countOne) {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put(stmtOne, countOne);
        return result;
    }

    private static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean supportBean = new SupportBean(theString, intPrimitive);
        supportBean.setLongPrimitive(longPrimitive);
        return supportBean;
    }

    private static Log log = LogFactory.getLog(TestRowPatternMaxStatesEngineWide.class);
}
