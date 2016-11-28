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

package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.io.Serializable;
import java.util.*;

public class TestContextPartitioned extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private EPServiceProviderSPI spi;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        spi = (EPServiceProviderSPI) epService;
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testPatternFilter() {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("stringContainsX", this.getClass().getName(), "stringContainsX");
        String eplContext = "create context IndividualBean partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplContext);

        String eplAnalysis = "context IndividualBean " +
                "select * from pattern [every (event1=SupportBean(stringContainsX(theString) = false) -> event2=SupportBean(stringContainsX(theString) = true))]";
        epService.getEPAdministrator().createEPL(eplAnalysis).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("F1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("F1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("X1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("X1", 0));
    }

    public void testMatchRecognize() {

        String eplContextOne = "create context SegmentedByString partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplContextOne);

        String eplMatchRecog = "context SegmentedByString " +
                "select * from SupportBean\n" +
                "match_recognize ( \n" +
                "  measures A.longPrimitive as a, B.longPrimitive as b\n" +
                "  pattern (A B) \n" +
                "  define " +
                "    A as A.intPrimitive = 1," +
                "    B as B.intPrimitive = 2\n" +
                ")";
        EPStatement stmtMatchRecog = epService.getEPAdministrator().createEPL(eplMatchRecog);
        stmtMatchRecog.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("A", 1, 10));
        epService.getEPRuntime().sendEvent(makeEvent("B", 1, 30));

        epService.getEPRuntime().sendEvent(makeEvent("A", 2, 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b".split(","), new Object[] {10L, 20L});

        epService.getEPRuntime().sendEvent(makeEvent("B", 2, 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b".split(","), new Object[] {30L, 40L});

        epService.getEPAdministrator().destroyAllStatements();

        // try with "prev"
        String eplContextTwo = "create context SegmentedByString partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplContextTwo);

        String eplMatchRecogWithPrev = "context SegmentedByString select * from SupportBean "+
                "match_recognize ( "+
                "  measures A.longPrimitive as e1, B.longPrimitive as e2"+
                "  pattern (A B) "+
                "  define A as A.intPrimitive >= prev(A.intPrimitive),B as B.intPrimitive >= prev(B.intPrimitive) "+
                ")";
        EPStatement stmtMatchRecogWithPrev = epService.getEPAdministrator().createEPL(eplMatchRecogWithPrev);
        stmtMatchRecogWithPrev.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("A", 1, 101));
        epService.getEPRuntime().sendEvent(makeEvent("B", 1, 201));
        epService.getEPRuntime().sendEvent(makeEvent("A", 2, 102));
        epService.getEPRuntime().sendEvent(makeEvent("B", 2, 202));
        epService.getEPRuntime().sendEvent(makeEvent("A", 3, 103));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "e1,e2".split(","), new Object[] {102L, 103L});

        epService.getEPRuntime().sendEvent(makeEvent("B", 3, 203));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "e1,e2".split(","), new Object[] {202L, 203L});
    }

    public void testJoinRemoveStream() {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().getConfiguration().addEventType(WebEvent.class);

        String stmtContext = "create context SegmentedBySession partition by sessionId from WebEvent";
        epService.getEPAdministrator().createEPL(stmtContext);

        String epl = " context SegmentedBySession " +
                " select rstream A.pageName as pageNameA , A.sessionId as sessionIdA, B.pageName as pageNameB, C.pageName as pageNameC from " +
                "WebEvent(pageName='Start')#time(30) A " +
                "full outer join " +
                "WebEvent(pageName='Middle')#time(30) B on A.sessionId = B.sessionId " +
                "full outer join " +
                "WebEvent(pageName='End')#time(30) C on A.sessionId  = C.sessionId " +
                "where A.pageName is not null and (B.pageName is null or C.pageName is null) "
                ;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        statement.addListener(listener);

        // Set up statement for finding missing events
        sendWebEventsComplete(0);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20000));
        sendWebEventsComplete(1);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(40000));
        assertFalse(listener.isInvoked());
        sendWebEventsComplete(2);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60000));
        sendWebEventsIncomplete(3);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(80000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100000));
        assertTrue(listener.isInvoked());
    }

    public void testIterateTargetedCP() {
        epService.getEPAdministrator().createEPL("create context PartitionedByString partition by theString from SupportBean");
        String[] fields = "c0,c1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('StmtOne') context PartitionedByString select context.key1 as c0, sum(intPrimitive) as c1 from SupportBean#length(5)");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));

        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), stmt.safeIterator(), fields, new Object[][]{{"E1", 10}, {"E2", 41}});

        // test iterator targeted
        SupportSelectorPartitioned selector = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(selector), stmt.safeIterator(selector), fields, new Object[][]{{"E2", 41}});
        assertFalse(stmt.iterator(new SupportSelectorPartitioned((List)null)).hasNext());
        assertFalse(stmt.iterator(new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"EX"}))).hasNext());
        assertFalse(stmt.iterator(new SupportSelectorPartitioned(Collections.<Object[]>emptyList())).hasNext());

        // test iterator filtered
        MySelectorFilteredPartitioned filtered = new MySelectorFilteredPartitioned(new Object[] {"E2"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(filtered), stmt.safeIterator(filtered), fields, new Object[][]{{"E2", 41}});

        // test always-false filter - compare context partition info
        MySelectorFilteredPartitioned filteredFalse = new MySelectorFilteredPartitioned(null);
        assertFalse(stmt.iterator(filteredFalse).hasNext());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{new Object[]{"E1"}, new Object[]{"E2"}}, filteredFalse.getContexts().toArray());
        
        try {
            stmt.iterator(new ContextPartitionSelectorCategory() {
                public Set<String> getLabels() {
                    return null;
                }
            });
            fail();
        }
        catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorSegmented] interfaces but received com."));
        }
    }

    public void testInvalid() {
        String epl;

        // invalid filter spec
        epl = "create context SegmentedByAString partition by string from SupportBean(dummy = 1)";
        tryInvalid(epl, "Error starting statement: Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

        // property not found
        epl = "create context SegmentedByAString partition by dummy from SupportBean";
        tryInvalid(epl, "Error starting statement: For context 'SegmentedByAString' property name 'dummy' not found on type SupportBean [");

        // mismatch number pf properties
        epl = "create context SegmentedByAString partition by theString from SupportBean, id, p00 from SupportBean_S0";
        tryInvalid(epl, "Error starting statement: For context 'SegmentedByAString' expected the same number of property names for each event type, found 1 properties for event type 'SupportBean' and 2 properties for event type 'SupportBean_S0' [create context SegmentedByAString partition by theString from SupportBean, id, p00 from SupportBean_S0]");

        // incompatible property types
        epl = "create context SegmentedByAString partition by theString from SupportBean, id from SupportBean_S0";
        tryInvalid(epl, "Error starting statement: For context 'SegmentedByAString' for context 'SegmentedByAString' found mismatch of property types, property 'theString' of type 'java.lang.String' compared to property 'id' of type 'java.lang.Integer' [");

        // duplicate type specification
        epl = "create context SegmentedByAString partition by theString from SupportBean, theString from SupportBean";
        tryInvalid(epl, "Error starting statement: For context 'SegmentedByAString' the event type 'SupportBean' is listed twice [");

        // duplicate type: subtype
        epService.getEPAdministrator().getConfiguration().addEventType(ISupportBaseAB.class);
        epService.getEPAdministrator().getConfiguration().addEventType(ISupportA.class);
        epl = "create context SegmentedByAString partition by baseAB from ISupportBaseAB, a from ISupportA";
        tryInvalid(epl, "Error starting statement: For context 'SegmentedByAString' the event type 'ISupportA' is listed twice: Event type 'ISupportA' is a subtype or supertype of event type 'ISupportBaseAB' [");

        // validate statement not applicable filters
        epService.getEPAdministrator().createEPL("create context SegmentedByAString partition by theString from SupportBean");
        epl = "context SegmentedByAString select * from SupportBean_S0";
        tryInvalid(epl, "Error starting statement: Segmented context 'SegmentedByAString' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [");

        // invalid attempt to partition a named window's streams
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epl = "create context SegmentedByWhat partition by theString from MyWindow";
        tryInvalid(epl, "Error starting statement: Partition criteria may not include named windows [create context SegmentedByWhat partition by theString from MyWindow]");

        // partitioned with named window
        epService.getEPAdministrator().createEPL("create schema SomeSchema(ipAddress string)");
        epService.getEPAdministrator().createEPL("create context TheSomeSchemaCtx Partition By ipAddress From SomeSchema");
        epl = "context TheSomeSchemaCtx create window MyEvent#time(30 sec) (ipAddress string)";
        tryInvalid(epl, "Error starting statement: Segmented context 'TheSomeSchemaCtx' requires that named windows are associated to an existing event type and that the event type is listed among the partitions defined by the create-context statement");
    }

    private void tryInvalid(String epl, String expected) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            if (!ex.getMessage().startsWith(expected)) {
                throw new RuntimeException("Expected/Received:\n" + expected + "\n" + ex.getMessage() + "\n");
            }
            assertTrue(expected.trim().length() != 0);
        }
    }

    public void testLargeNumberContexts() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByAString  partition by theString from SupportBean");

        String[] fields = "col1".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("context SegmentedByAString " +
                "select sum(intPrimitive) as col1," +
                "prev(1, intPrimitive)," +
                "prior(1, intPrimitive)," +
                "(select id from SupportBean_S0#lastevent)" +
                "  from SupportBean#keepall");
        stmtOne.addListener(listener);
        
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{i});
        }
    }

    public void testAdditionalFilters() throws Exception {
        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByAString " +
                "partition by theString from SupportBean(intPrimitive>0), p00 from SupportBean_S0(id > 0)");

        // first send a view events
        epService.getEPRuntime().sendEvent(new SupportBean("B1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(-2, "S0"));
        assertEquals(0, filterSPI.getFilterCountApprox());

        String[] fields = "col1,col2".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("context SegmentedByAString " +
                "select sum(sb.intPrimitive) as col1, sum(s0.id) as col2 " +
                "from pattern [every (s0=SupportBean_S0 or sb=SupportBean)]");
        stmtOne.addListener(listener);

        assertEquals(2, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-3, "S0"));
        epService.getEPRuntime().sendEvent(new SupportBean("S0", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("S1", -2));
        assertFalse(listener.isInvoked());
        assertEquals(2, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 2});

        epService.getEPRuntime().sendEvent(new SupportBean("S1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-2, "S0"));
        epService.getEPRuntime().sendEvent(new SupportBean("S1", -10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 3});

        epService.getEPRuntime().sendEvent(new SupportBean("S0", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{9, 2});

        epService.getEPAdministrator().destroyAllStatements();
        assertEquals(0, filterSPI.getFilterCountApprox());

        // Test unnecessary filter
        String epl = "create context CtxSegmented partition by theString from SupportBean;" +
                     "context CtxSegmented select * from pattern [every a=SupportBean -> c=SupportBean(c.theString=a.theString)];";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
    }

    public void testMultiStatementFilterCount() {
        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        EPStatement stmtContext = epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByAString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0");
        assertEquals(0, filterSPI.getFilterCountApprox());

        // first send a view events
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0"));

        String[] fields = new String[] {"col1"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("context SegmentedByAString select sum(id) as col1 from SupportBean_S0");
        stmtOne.addListener(listener);

        assertEquals(2, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10});

        assertEquals(3, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(8, "S1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{8});

        assertEquals(4, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "S0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{14});

        assertEquals(4, filterSPI.getFilterCountApprox());

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("context SegmentedByAString select sum(intPrimitive) as col1 from SupportBean");
        stmtTwo.addListener(listener);

        assertEquals(6, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("S0", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5});

        assertEquals(6, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("S2", 6));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{6});

        assertEquals(8, filterSPI.getFilterCountApprox());

        stmtOne.destroy();
        assertEquals(5, filterSPI.getFilterCountApprox());  // 5 = 3 from context instances and 2 from context itself

        stmtTwo.destroy();
        assertEquals(0, filterSPI.getFilterCountApprox());

        stmtContext.destroy();
        assertEquals(0, filterSPI.getFilterCountApprox());
    }

    public void testSegmentedSubtype() {
        epService.getEPAdministrator().getConfiguration().addEventType("ISupportBaseAB", ISupportBaseAB.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ISupportA", ISupportA.class);
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by baseAB from ISupportBaseAB");

        String[] fields = "col1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context SegmentedByString select count(*) as col1 from ISupportA");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new ISupportAImpl("A1", "AB1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});

        epService.getEPRuntime().sendEvent(new ISupportAImpl("A2", "AB1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L});

        epService.getEPRuntime().sendEvent(new ISupportAImpl("A3", "AB2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});

        epService.getEPRuntime().sendEvent(new ISupportAImpl("A4", "AB1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3L});
    }

    public void testSegmentedJoinMultitypeMultifield() throws Exception {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedBy2Fields " +
                "partition by theString and intPrimitive from SupportBean, p00 and id from SupportBean_S0");

        String[] fields = "c1,c2,c3,c4,c5,c6".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context SegmentedBy2Fields " +
                "select theString as c1, intPrimitive as c2, id as c3, p00 as c4, context.key1 as c5, context.key2 as c6 " +
                "from SupportBean#lastevent, SupportBean_S0#lastevent");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "G1"));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "G2"));
        assertFalse(listener.isInvoked());
        
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 1, 1, "G2", "G2", 1});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "G2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, 2, "G2", "G2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "G1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 1, 1, "G1", "G1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 2, 2, "G1", "G1", 2});

        // ESPER-663
        epService.getEPAdministrator().getConfiguration().addEventType("Event", Event.class);
        String epl =
            "@Audit @Name('CTX') create context Ctx partition by grp, subGrp from Event;\n" +
            "@Audit @Name('Window') context Ctx create window EventData#unique(type) as Event;" +
            "@Audit @Name('Insert') context Ctx insert into EventData select * from Event;" +
            "@Audit @Name('Test') context Ctx select irstream * from EventData;";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("Test").addListener(listener);
        epService.getEPRuntime().sendEvent(new Event("G1", "SG1", 1, 10.45));
        assertTrue(listener.isInvoked());
        epService.getEPAdministrator().destroyAllStatements();

        // Esper-695
        String eplTwo =
                "create context Ctx partition by theString from SupportBean;\n" +
                "context Ctx create window MyWindow#unique(intPrimitive) as SupportBean;" +
                "context Ctx select irstream * from pattern [MyWindow];";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplTwo);
        tryInvalidCreateWindow();
        tryInvalidCreateWindow(); // making sure all is cleaned up
    }

    private void tryInvalidCreateWindow() {
        try {
            epService.getEPAdministrator().createEPL("context Ctx create window MyInvalidWindow#unique(p00) as SupportBean_S0");
            fail();
        }
        catch (EPException ex) {
            assertEquals("Error starting statement: Segmented context 'Ctx' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [context Ctx create window MyInvalidWindow#unique(p00) as SupportBean_S0]", ex.getMessage());
        }
    }

    public void testSegmentedSubselectPrevPrior() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fieldsPrev = new String[] {"theString", "col1"};
        EPStatement stmtPrev = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select theString, (select prev(0, id) from SupportBean_S0#keepall) as col1 from SupportBean");
        stmtPrev.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G1", null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G2", null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrev, new Object[]{"G1", null});  // since returning multiple rows

        stmtPrev.stop();

        String[] fieldsPrior = new String[] {"theString", "col1"};
        EPStatement stmtPrior = epService.getEPAdministrator().createEPL("@Name('B') context SegmentedByString " +
                "select theString, (select prior(0, id) from SupportBean_S0#keepall) as col1 from SupportBean");
        stmtPrior.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G1", null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G2", null});    // since category started as soon as statement added

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G2", 2}); // since returning multiple rows

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"G1", null});  // since returning multiple rows
    }

    public void testSegmentedPrior() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fields = new String[] {"val0", "val1"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select intPrimitive as val0, prior(1, intPrimitive) as val1 from SupportBean");
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 10});
        
        stmtOne.stop();

        stmtOne.destroy();
    }

    public void testSegmentedSubqueryFiltered() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fields = new String[] {"theString", "intPrimitive", "val0"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from SupportBean_S0#lastevent as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb");
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "s1"));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "s2"));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "s2"});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 10, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "s3"));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 10, "s3"});

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 10, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "s3"});
    }

    public void testSegmentedSubqueryNamedWindowIndexShared() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("@Hint('enable_window_subquery_indexshare') create window MyWindow#keepall as SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_S0");

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from MyWindow as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb");
        stmtOne.addListener(listener);

        runAssertionSubqueryNW();
    }

    public void testSegmentedSubqueryNamedWindowIndexUnShared() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_S0");

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from MyWindow as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb");
        stmtOne.addListener(listener);

        runAssertionSubqueryNW();
    }

    private void runAssertionSubqueryNW() {
        String[] fields = new String[] {"theString", "intPrimitive", "val0"};

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "s1"));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "s1"});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 10, "s1"});

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 20, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "s2"));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 20, "s2"});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 20, "s2"});
    }

    public void testSegmentedJoin() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fields = new String[] {"sb.theString", "sb.intPrimitive", "s0.id"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select * from SupportBean#keepall as sb, SupportBean_S0#keepall as s0 " +
                "where intPrimitive = id");
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, 20});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(30));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 30));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 30, 30});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 30, 30});
    }

    public void testSegmentedPattern() {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fields = new String[] {"a.theString", "a.intPrimitive", "b.theString", "b.intPrimitive"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive+1)]");
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, "G2", 21});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "G1", 11});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 21, "G2", 22});

        stmtOne.destroy();

        // add another statement: contexts already exist, this one uses @Consume
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive+1)@Consume]");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20, "G2", 21});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, "G1", 11});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 22));
        assertFalse(listener.isInvoked());

        stmtTwo.destroy();

        // test truly segmented consume
        String[] fieldsThree = new String[] {"a.theString", "a.intPrimitive", "b.id", "b.p00"};
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean_S0(id=a.intPrimitive)@Consume]");
        stmtThree.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1"));   // should be 2 output rows
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastNewData(), fieldsThree, new Object[][] {{"G1", 10, 10, "E1"}, {"G2", 10, 10, "E1"}});
    }

    public void testSegmentedViews() {
        String contextEPL = "@Name('context') create context SegmentedByString as partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(contextEPL);

        String[] fieldsIterate = "intPrimitive".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select irstream intPrimitive, prevwindow(items) as pw from SupportBean#length(2) as items");
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        assertViewData(10, new Object[][]{{"G1", 10}}, null);
        EPAssertionUtil.assertPropsPerRow(stmtOne.iterator(), stmtOne.safeIterator(), fieldsIterate, new Object[][]{{10}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertViewData(20, new Object[][]{{"G2", 20}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        assertViewData(11, new Object[][]{{"G1", 11}, {"G1", 10}}, null);
        EPAssertionUtil.assertPropsPerRow(stmtOne.iterator(), stmtOne.safeIterator(), fieldsIterate, new Object[][]{{10}, {11}, {20}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 21));
        assertViewData(21, new Object[][]{{"G2", 21}, {"G2", 20}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 12));
        assertViewData(12, new Object[][]{{"G1", 12}, {"G1", 11}}, 10);

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 22));
        assertViewData(22, new Object[][]{{"G2", 22}, {"G2", 21}}, 20);

        stmtOne.destroy();
        
        // test SODA
        epService.getEPAdministrator().destroyAllStatements();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(contextEPL);
        assertEquals(contextEPL, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(contextEPL, stmt.getText());

        // test built-in properties
        String[] fields = "c1,c2,c3,c4".split(",");
        String ctx = "SegmentedByString";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select context.name as c1, context.id as c2, context.key1 as c3, theString as c4 " +
                "from SupportBean#length(2) as items");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, 0, "G1", "G1"});
        epService.getEPAdministrator().destroyAllStatements();

        // test grouped delivery
        epService.getEPAdministrator().createEPL("create variable boolean trigger = false");
        epService.getEPAdministrator().createEPL("create context MyCtx partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('Out') context MyCtx select * from SupportBean#expr(not trigger) for grouped_delivery(theString)");
        epService.getEPAdministrator().getStatement("Out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().setVariableValue("trigger", true);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(100));

        assertEquals(2, listener.getNewDataList().size());
    }

    public void testJoinWhereClauseOnPartitionKey() throws Exception {
        String epl = "create context MyCtx partition by theString from SupportBean;\n" +
                "@name('select') context MyCtx select * from SupportBean#lastevent as sb, SupportBean_S0#lastevent as s0 " +
                "where theString is 'Test'";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("select").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("Test", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertTrue(listener.isInvoked());
    }

    private void assertViewData(int newIntExpected, Object[][] newArrayExpected, Integer oldIntExpected) {
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(newIntExpected, listener.getLastNewData()[0].get("intPrimitive"));
        SupportBean[] beans = (SupportBean[]) listener.getLastNewData()[0].get("pw");
        assertEquals(newArrayExpected.length, beans.length);
        for (int i = 0; i < beans.length; i++) {
            assertEquals(newArrayExpected[i][0], beans[i].getTheString());
            assertEquals(newArrayExpected[i][1], beans[i].getIntPrimitive());
        }

        if (oldIntExpected != null) {
            assertEquals(1, listener.getLastOldData().length);
            assertEquals(oldIntExpected, listener.getLastOldData()[0].get("intPrimitive"));
        }
        else {
            assertNull(listener.getLastOldData());
        }
        listener.reset();
    }

    private static class MySelectorFilteredPartitioned implements ContextPartitionSelectorFiltered {

        private Object[] match;

        private List<Object[]> contexts = new ArrayList<Object[]>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<Integer>();

        private MySelectorFilteredPartitioned(Object[] match) {
            this.match = match;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierPartitioned id = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
            if (match == null && cpids.contains(id.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
            }
            cpids.add(id.getContextPartitionId());
            contexts.add(id.getKeys());
            return Arrays.equals(id.getKeys(), match);
        }

        public List<Object[]> getContexts() {
            return contexts;
        }
    }

    public static class Event implements Serializable {
   		public String grp;
   		public String subGrp;
   		public int type;
   		public double value;

   		public Event() {}

   		public Event(final String group, final String subGroup, final int type, final double value) {
   			grp = group;
   			subGrp = subGroup;
   			this.type = type;
   			this.value = value;
   		}

   		public String getGrp() {
   			return grp;
   		}

   		public void setGrp(final String group) {
   			grp = group;
   		}

   		public String getSubGrp() {
   			return subGrp;
   		}

   		public void setSubGrp(final String subGroup) {
   			subGrp = subGroup;
   		}

   		public int getType() {
   			return type;
   		}

   		public void setType(final int type) {
   			this.type = type;
   		}

   		public double getValue() {
   			return value;
   		}

   		public void setValue(final double value) {
   			this.value = value;
   		}

   		@Override
   		public boolean equals(final Object obj) {
   			if (this == obj) {
   				return true;
   			}
   			if (obj instanceof Event) {
   				final Event evt = (Event) obj;
   				return grp.equals(evt.grp) && subGrp.equals(evt.subGrp) && type == evt.type && Math.abs(value - evt.value) < 1e-6;
   			}

   			return false;
   		}

   		@Override
   		public String toString() {
   			return "(" + grp + ", " + subGrp + ")@" + type + "=" + value;
   		}

   	}

    private void sendWebEventsIncomplete(int id) {
        epService.getEPRuntime().sendEvent(new WebEvent("Start", String.valueOf(id)));
        epService.getEPRuntime().sendEvent(new WebEvent("End", String.valueOf(id)));
    }

    private void sendWebEventsComplete(int id) {
        epService.getEPRuntime().sendEvent(new WebEvent("Start", String.valueOf(id)));
        epService.getEPRuntime().sendEvent(new WebEvent("Middle", String.valueOf(id)));
        epService.getEPRuntime().sendEvent(new WebEvent("End", String.valueOf(id)));
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static boolean stringContainsX(String theString) {
        return theString.contains("X");
    }

    public static class WebEvent implements Serializable {
        private final String pageName;
        private final String sessionId;

        public WebEvent(String pageName, String sessionId) {
            this.pageName = pageName;
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getPageName() {
            return pageName;
        }
    }
}
