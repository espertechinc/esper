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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPContextPartitionAdminSPI;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.filter.FilterSet;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.context.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.AgentInstanceAssertionUtil;

import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextNested implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.addEventType("SupportBean_S2", SupportBean_S2.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNestedContextWithFilterUDF(epService);
        runAssertionIterateTargetedCP(epService);
        runAssertionInvalid(epService);
        runAssertionIterator(epService);
        runAssertionPartitionedWithFilter(epService);
        runAssertionNestingFilterCorrectness(epService, false);
        runAssertionNestingFilterCorrectness(epService, true);
        runAssertionPartitionOverlap(epService);
        runAssertionPartitionedNonOverlap(epService);
        runAssertionCategoryOverPatternInitiated(epService);
        runAssertionSingleEventTriggerNested(epService);
        runAssertion4ContextsNested(epService);
        runAssertionTemporalOverlapOverPartition(epService);
        runAssertion3ContextsTermporalOverCategoryOverPartition(epService);
        runAssertionTemporalFixedOverHash(epService);
        runAssertionCategoryOverTemporalOverlapping(epService);
        runAssertionFixedTemporalOverPartitioned(epService);
        runAssertionPartitionedOverFixedTemporal(epService);
        runAssertionContextProps(epService);
        runAssertionLateComingStatement(epService);
        runAssertionPartitionWithMultiPropsAndTerm(epService);
        runAssertionNestedOverlappingAndPattern(epService);
        runAssertionNestedNonOverlapping(epService);
        runAssertionPartitionedOverPatternInitiated(epService);
        runAssertionInitWStartNow(epService);
    }

    private void runAssertionInitWStartNow(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context Ctx "
                + "context C0 initiated by SupportBean as criteria terminated by SupportBean(theString='x'), "
                + "context C1 start @now end (*,*,*,*,*,*/5)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context Ctx select context.C0.criteria as c0, event, count(*) as cnt from SupportBean_S0(p00=context.C0.criteria.theString) as event");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean criteriaA = new SupportBean("A", 0);
        epService.getEPRuntime().sendEvent(criteriaA);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 0));
        assertFalse(listener.isInvoked());

        SupportBean_S0 s0 = new SupportBean_S0(2, "A");
        epService.getEPRuntime().sendEvent(s0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,event,cnt".split(","), new Object[] {criteriaA, s0, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,cnt".split(","), new Object[] {criteriaA, 2L});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000000));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,cnt".split(","), new Object[] {criteriaA, 1L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPartitionedOverPatternInitiated(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context TheContext " +
                "context C0 partition by theString from SupportBean," +
                "context C1 initiated by SupportBean(intPrimitive=1) terminated by SupportBean(intPrimitive=2)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context TheContext select theString, sum(longPrimitive) as theSum from SupportBean output last when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportBean(epService, "A", 0, 1);
        sendSupportBean(epService, "B", 0, 2);
        sendSupportBean(epService, "C", 1, 3);
        sendSupportBean(epService, "D", 1, 4);
        sendSupportBean(epService, "A", 0, 5);
        sendSupportBean(epService, "C", 0, 6);
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "C", 2, -10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString,theSum".split(","), new Object[] {"C", -1L});

        sendSupportBean(epService, "D", 2, 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString,theSum".split(","), new Object[] {"D", 9L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedContextWithFilterUDF(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(
                "customEnabled", ExecContextNested.class.getName(), "customMatch", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(
                "customDisabled", ExecContextNested.class.getName(), "customMatch", ConfigurationPlugInSingleRowFunction.FilterOptimizable.DISABLED);
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context ACtx initiated by SupportBean_S0 as s0 terminated after 24 hours, " +
                "context BCtx initiated by SupportBean_S1 as s1 terminated after 1 hour");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context NestedContext select * " +
                "from SupportBean(" +
                "customEnabled(theString, context.ACtx.s0.p00, intPrimitive, context.BCtx.s1.id)" +
                " and " +
                "customDisabled(theString, context.ACtx.s0.p00, intPrimitive, context.BCtx.s1.id))");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "S1"));
        epService.getEPRuntime().sendEvent(new SupportBean("X", -1));
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    public static boolean customMatch(String theString, String p00, int intPrimitive, int s1id) {
        assertEquals("X", theString);
        assertEquals("S0", p00);
        assertEquals(-1, intPrimitive);
        assertEquals(2, s1id);
        return true;
    }

    private void runAssertionIterateTargetedCP(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context ACtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id), " +
                "context BCtx group by intPrimitive < 0 as grp1, group by intPrimitive = 0 as grp2, group by intPrimitive > 0 as grp3 from SupportBean");

        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('StmtOne') context NestedContext " +
                "select context.ACtx.s0.p00 as c0, context.BCtx.label as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#length(5) group by theString");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));

        Object[][] expectedAll = new Object[][]{{"S0_1", "grp1", "E2", -1}, {"S0_1", "grp3", "E3", 5}, {"S0_1", "grp3", "E1", 3}, {"S0_2", "grp3", "E1", 2}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), stmt.safeIterator(), fields, expectedAll);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(ContextPartitionSelectorAll.INSTANCE), stmt.safeIterator(ContextPartitionSelectorAll.INSTANCE), fields, expectedAll);
        SupportSelectorById allIds = new SupportSelectorById(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5)));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(allIds), stmt.safeIterator(allIds), fields, expectedAll);

        // test iterator targeted
        ContextPartitionSelector firstOne = new SupportSelectorFilteredInitTerm("S0_2");
        ContextPartitionSelector secondOne = new SupportSelectorCategory(Collections.singleton("grp3"));
        SupportSelectorNested nestedSelector = new SupportSelectorNested(Collections.singletonList(new ContextPartitionSelector[]{firstOne, secondOne}));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(nestedSelector), stmt.safeIterator(nestedSelector), fields, new Object[][]{{"S0_2", "grp3", "E1", 2}});

        ContextPartitionSelector firstTwo = new SupportSelectorFilteredInitTerm("S0_1");
        ContextPartitionSelector secondTwo = new SupportSelectorCategory(Collections.singleton("grp1"));
        SupportSelectorNested nestedSelectorTwo = new SupportSelectorNested(Arrays.asList(new ContextPartitionSelector[]{firstOne, secondOne}, new ContextPartitionSelector[]{firstTwo, secondTwo}));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(nestedSelectorTwo), stmt.safeIterator(nestedSelectorTwo), fields, new Object[][]{{"S0_2", "grp3", "E1", 2}, {"S0_1", "grp1", "E2", -1}});

        // test iterator filtered : not supported for nested
        try {
            MySelectorFilteredNested filtered = new MySelectorFilteredNested(new Object[]{"S0_2", "grp3"});
            stmt.iterator(filtered);
            fail();
        } catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorById, ContextPartitionSelectorNested] interfaces but received com."));
        }

        epService.getEPAdministrator().destroyAllStatements();

        // test 3 nesting levels and targeted
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context ACtx group by intPrimitive < 0 as i1, group by intPrimitive = 0 as i2, group by intPrimitive > 0 as i3 from SupportBean," +
                "context BCtx group by longPrimitive < 0 as l1, group by longPrimitive = 0 as l2, group by longPrimitive > 0 as l3 from SupportBean," +
                "context CCtx group by boolPrimitive = true as b1, group by boolPrimitive = false as b2 from SupportBean");

        String[] fieldsSelect = "c0,c1,c2,c3".split(",");
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("@Name('StmtOne') context NestedContext " +
                "select context.ACtx.label as c0, context.BCtx.label as c1, context.CCtx.label as c2, count(*) as c3 from SupportBean#length(5) having count(*) > 0");

        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 10L, true));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 2, -10L, false));
        epService.getEPRuntime().sendEvent(makeEvent("E3", 1, 11L, false));
        epService.getEPRuntime().sendEvent(makeEvent("E4", 0, 0L, true));
        epService.getEPRuntime().sendEvent(makeEvent("E5", -1, 10L, false));
        epService.getEPRuntime().sendEvent(makeEvent("E6", -1, 10L, true));

        Object[][] expectedRows = new Object[][]{
                {"i1", "l3", "b1", 2L},
                {"i3", "l1", "b2", 1L},
                {"i1", "l3", "b2", 1L},
                {"i2", "l2", "b1", 1L},
                {"i3", "l3", "b2", 1L},
        };
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelect.iterator(), stmtSelect.safeIterator(), fieldsSelect, expectedRows);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelect.iterator(ContextPartitionSelectorAll.INSTANCE), stmtSelect.safeIterator(ContextPartitionSelectorAll.INSTANCE), fields, expectedRows);

        // test iterator targeted
        ContextPartitionSelector[] selectors = new ContextPartitionSelector[]{
            new SupportSelectorCategory(Collections.singleton("i3")),
            new SupportSelectorCategory(Collections.singleton("l1")),
            new SupportSelectorCategory(Collections.singleton("b2"))
        };
        SupportSelectorNested nestedSelectorSelect = new SupportSelectorNested(Collections.singletonList(selectors));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelect.iterator(nestedSelectorSelect), stmtSelect.safeIterator(nestedSelectorSelect), fieldsSelect, new Object[][]{{"i3", "l1", "b2", 1L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        // invalid same sub-context name twice
        epl = "create context ABC context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *)";
        tryInvalid(epService, epl, "Error starting statement: Context by name 'EightToNine' has already been declared within nested context 'ABC' [");

        // validate statement added to nested context
        epl = "create context ABC context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), context PartCtx as partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(epl);
        epl = "context ABC select * from SupportBean_S0";
        tryInvalid(epService, epl, "Error starting statement: Segmented context 'PartCtx' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [");
    }

    private void runAssertionIterator(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegByString partition by theString from SupportBean");

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.EightToNine.startTime as c0, context.SegByString.key1 as c1, intPrimitive as c2 from SupportBean#keepall");
        stmtUser.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        Object[][] expected = new Object[][]{{DateTime.parseDefaultMSec("2002-05-1T08:00:00.000"), "E1", 1}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(stmtUser.iterator(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(stmtUser.safeIterator(), fields, expected);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        expected = new Object[][]{{DateTime.parseDefaultMSec("2002-05-1T08:00:00.000"), "E1", 1}, {DateTime.parseDefaultMSec("2002-05-1T08:00:00.000"), "E1", 2}};
        EPAssertionUtil.assertPropsPerRow(stmtUser.iterator(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(stmtUser.safeIterator(), fields, expected);

        // extract path
        if (getSpi(epService).isSupportsExtract()) {
            getSpi(epService).extractPaths("NestedContext", new ContextPartitionSelectorAll());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPartitionedWithFilter(EPServiceProvider epService) {
        runAssertionPartitionedNonOverlap(epService);
        runAssertionPartitionOverlap(epService);
    }

    private void runAssertionNestingFilterCorrectness(EPServiceProvider epService, boolean isolationAllowed) {
        String eplContext;
        String eplSelect = "context TheContext select count(*) from SupportBean";
        EPStatementSPI spiCtx;
        EPStatementSPI spiStmt;
        SupportBean bean;

        // category over partition
        eplContext = "create context TheContext " +
                "context CtxCategory as group intPrimitive < 0 as negative, group intPrimitive > 0 as positive from SupportBean, " +
                "context CtxPartition as partition by theString from SupportBean";
        spiCtx = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplContext);
        spiStmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplSelect);

        assertFilters(epService, isolationAllowed, "SupportBean(intPrimitive<0),SupportBean(intPrimitive>0)", spiCtx);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        assertFilters(epService, isolationAllowed, "SupportBean(theStringisE1,intPrimitive<0)", spiStmt);
        epService.getEPAdministrator().destroyAllStatements();

        // category over partition over category
        eplContext = "create context TheContext " +
                "context CtxCategoryOne as group intPrimitive < 0 as negative, group intPrimitive > 0 as positive from SupportBean, " +
                "context CtxPartition as partition by theString from SupportBean," +
                "context CtxCategoryTwo as group longPrimitive < 0 as negative, group longPrimitive > 0 as positive from SupportBean";
        spiCtx = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplContext);
        spiStmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplSelect);

        assertFilters(epService, isolationAllowed, "SupportBean(intPrimitive<0),SupportBean(intPrimitive>0)", spiCtx);
        bean = new SupportBean("E1", -1);
        bean.setLongPrimitive(1);
        epService.getEPRuntime().sendEvent(bean);
        assertFilters(epService, isolationAllowed, "SupportBean(longPrimitive<0,theStringisE1,intPrimitive<0),SupportBean(longPrimitive>0,theStringisE1,intPrimitive<0)", spiStmt);
        assertFilters(epService, isolationAllowed, "SupportBean(intPrimitive<0),SupportBean(intPrimitive>0)", spiCtx);
        epService.getEPAdministrator().destroyAllStatements();

        // partition over partition over partition
        eplContext = "create context TheContext " +
                "context CtxOne as partition by theString from SupportBean, " +
                "context CtxTwo as partition by intPrimitive from SupportBean," +
                "context CtxThree as partition by longPrimitive from SupportBean";
        spiCtx = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplContext);
        spiStmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplSelect);

        assertFilters(epService, isolationAllowed, "SupportBean()", spiCtx);
        bean = new SupportBean("E1", 2);
        bean.setLongPrimitive(3);
        epService.getEPRuntime().sendEvent(bean);
        assertFilters(epService, isolationAllowed, "SupportBean(longPrimitiveis3,intPrimitiveis2,theStringisE1)", spiStmt);
        assertFilters(epService, isolationAllowed, "SupportBean(),SupportBean(theStringisE1),SupportBean(theStringisE1,intPrimitiveis2)", spiCtx);
        epService.getEPAdministrator().destroyAllStatements();

        // category over hash
        eplContext = "create context TheContext " +
                "context CtxCategoryOne as group intPrimitive < 0 as negative, group intPrimitive > 0 as positive from SupportBean, " +
                "context CtxTwo as coalesce by consistent_hash_crc32(theString) from SupportBean granularity 100";
        spiCtx = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplContext);
        spiStmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplSelect);

        assertFilters(epService, isolationAllowed, "SupportBean(intPrimitive<0),SupportBean(intPrimitive>0)", spiCtx);
        bean = new SupportBean("E1", 2);
        bean.setLongPrimitive(3);
        epService.getEPRuntime().sendEvent(bean);
        assertFilters(epService, isolationAllowed, "SupportBean(consistent_hash_crc32(theString)=33,intPrimitive>0)", spiStmt);
        assertFilters(epService, isolationAllowed, "SupportBean(intPrimitive<0),SupportBean(intPrimitive>0)", spiCtx);
        epService.getEPAdministrator().destroyAllStatements();

        eplContext = "create context TheContext " +
                "context CtxOne as partition by theString from SupportBean, " +
                "context CtxTwo as start pattern [SupportBean_S0] end pattern[SupportBean_S1]";
        spiCtx = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplContext);
        spiStmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplSelect);

        assertFilters(epService, isolationAllowed, "SupportBean()", spiCtx);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertFilters(epService, isolationAllowed, "", spiStmt);
        assertFilters(epService, isolationAllowed, "SupportBean(),SupportBean_S0()", spiCtx);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private static void assertFilters(EPServiceProvider epService, boolean allowIsolation, String expected, EPStatementSPI spiStmt) {
        if (!allowIsolation) {
            return;
        }
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        if (!filterSPI.isSupportsTakeApply()) {
            return;
        }
        FilterSet set = filterSPI.take(Collections.singleton(spiStmt.getStatementId()));
        assertEquals(expected, set.toString());
        filterSPI.apply(set);
    }

    private void runAssertionPartitionOverlap(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(TestEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(EndEvent.class);
        epService.getEPAdministrator().createEPL("@Audit('pattern-instances') create context TheContext"
                + " context CtxSession partition by id from TestEvent, "
                + " context CtxStartEnd start TestEvent as te end EndEvent(id=te.id)");
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "context TheContext select firstEvent from TestEvent#firstevent() as firstEvent"
                        + " inner join TestEvent#lastevent as lastEvent");
        SupportSubscriber supportSubscriber = new SupportSubscriber();
        stmt.setSubscriber(supportSubscriber);

        for (int i = 0; i < 2; i++) {
            epService.getEPRuntime().sendEvent(new TestEvent(1, 5));
            epService.getEPRuntime().sendEvent(new TestEvent(2, 10));
            epService.getEPRuntime().sendEvent(new EndEvent(1));

            supportSubscriber.reset();
            epService.getEPRuntime().sendEvent(new TestEvent(2, 15));
            assertEquals(10, ((TestEvent) supportSubscriber.assertOneGetNewAndReset()).getTime());

            epService.getEPRuntime().sendEvent(new EndEvent(1));
            epService.getEPRuntime().sendEvent(new EndEvent(2));
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPartitionedNonOverlap(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        String eplCtx = "create context NestedContext as " +
                "context SegByString as partition by theString from SupportBean(intPrimitive > 0), " +
                "context InitCtx initiated by SupportBean_S0 as s0 terminated after 60 seconds";
        epService.getEPAdministrator().createEPL(eplCtx);

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.InitCtx.s0.p00 as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean group by theString");
        stmtUser.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        SupportBean_S0 s0_1 = new SupportBean_S0(1, "S0_1");
        epService.getEPRuntime().sendEvent(s0_1);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -5));
        assertFalse(listener.isInvoked());
        SupportContextPropUtil.assertContextPropsNested(epService, "NestedContext", new int[] {0}, "SegByString,InitCtx".split(","), new String[] {"key1", "s0"},
                new Object[][][] {{{"E1"}, {s0_1}}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_1", "E1", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        assertFalse(listener.isInvoked());

        SupportBean_S0 s0_2 = new SupportBean_S0(2, "S0_2");
        epService.getEPRuntime().sendEvent(s0_2);

        SupportContextPropUtil.assertContextPropsNested(epService, "NestedContext", new int[] {0,1,2}, "SegByString,InitCtx".split(","), new String[] {"key1", "s0"},
                new Object[][][] {{{"E1"}, {s0_1}}, {{"E1"}, {s0_2}}, {{"E2"}, {s0_2}}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "E2", 4}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_1", "E1", 8}, {"S0_2", "E1", 6}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCategoryOverPatternInitiated(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        String eplCtx = "create context NestedContext as " +
                "context ByCat as group intPrimitive < 0 as g1, group intPrimitive > 0 as g2, group intPrimitive = 0 as g3 from SupportBean, " +
                "context InitCtx as initiated by pattern [every a=SupportBean_S0 -> b=SupportBean_S1(id = a.id)] terminated after 10 sec";
        epService.getEPAdministrator().createEPL(eplCtx);

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatementSPI stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.ByCat.label as c0, context.InitCtx.a.p00 as c1, context.InitCtx.b.p10 as c2, sum(intPrimitive) as c3 from SupportBean group by theString");
        stmtUser.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "S1_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "S1_2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 3}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g1", "S0_1", "S1_2", -2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g3", "S0_1", "S1_2", 0}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 8}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 6}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "S0_3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "S1_3"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 7));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_1", "S1_2", 15}, {"g2", "S0_3", "S1_3", 7}});

        sendTimeEvent(epService, "2002-05-1T08:00:10.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(104, "S0_4"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(104, "S1_4"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 9));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"g2", "S0_4", "S1_4", 9}});

        if (getSpi(epService).isSupportsExtract()) {
            getSpi(epService).extractPaths("NestedContext", new ContextPartitionSelectorAll());
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSingleEventTriggerNested(EPServiceProvider epService) {
        // Test partitioned context
        //
        String eplCtxOne = "create context NestedContext as " +
                "context SegByString as partition by theString from SupportBean, " +
                "context SegByInt as partition by intPrimitive from SupportBean, " +
                "context SegByLong as partition by longPrimitive from SupportBean ";
        EPStatement stmtCtxOne = epService.getEPAdministrator().createEPL(eplCtxOne);

        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String[] fieldsOne = "c0,c1,c2,c3".split(",");
        EPStatementSPI stmtUserOne = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.SegByString.key1 as c0, context.SegByInt.key1 as c1, context.SegByLong.key1 as c2, count(*) as c3 from SupportBean");
        stmtUserOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 10, 100L, 1L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getAndResetLastNewData(), fieldsOne, new Object[][]{{"E2", 10, 100L, 1L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 11, 100));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 11, 100L, 1L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 101));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 10, 101L, 1L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 100));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getAndResetLastNewData(), fieldsOne, new Object[][]{{"E1", 10, 100L, 2L}});

        stmtCtxOne.destroy();
        stmtUserOne.destroy();

        // Test partitioned context
        //
        String eplCtxTwo = "create context NestedContext as " +
                "context HashOne coalesce by hash_code(theString) from SupportBean granularity 10, " +
                "context HashTwo coalesce by hash_code(intPrimitive) from SupportBean granularity 10";
        EPStatement stmtCtxTwo = epService.getEPAdministrator().createEPL(eplCtxTwo);

        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        String[] fieldsTwo = "c1,c2".split(",");
        EPStatementSPI stmtUserTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "theString as c1, count(*) as c2 from SupportBean");
        stmtUserTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(listenerTwo.getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E1", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRow(listenerTwo.getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E2", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(listenerTwo.getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E1", 2L}});

        stmtCtxTwo.destroy();
        stmtUserTwo.destroy();

        // Test partitioned context
        //
        String eplCtxThree = "create context NestedContext as " +
                "context InitOne initiated by SupportBean(theString like 'I%') as sb0 terminated after 10 sec, " +
                "context InitTwo initiated by SupportBean(intPrimitive > 0) as sb1 terminated after 10 sec";
        EPStatement stmtCtxThree = epService.getEPAdministrator().createEPL(eplCtxThree);

        SupportUpdateListener listenerThree = new SupportUpdateListener();
        String[] fieldsThree = "c1,c2".split(",");
        EPStatementSPI stmtUserThree = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "theString as c1, count(*) as c2 from SupportBean");
        stmtUserThree.addListener(listenerThree);

        epService.getEPRuntime().sendEvent(new SupportBean("I1", 1));
        EPAssertionUtil.assertPropsPerRow(listenerThree.getAndResetLastNewData(), fieldsThree, new Object[][]{{"I1", 1L}});

        stmtCtxThree.destroy();
        stmtUserThree.destroy();
    }

    private void runAssertion4ContextsNested(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        sendTimeEvent(epService, "2002-05-1T07:00:00.000");

        String eplCtx = "create context NestedContext as " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context InitCtx0 initiated by SupportBean_S0 as s0 terminated after 60 seconds, " +
                "context InitCtx1 initiated by SupportBean_S1 as s1 terminated after 30 seconds, " +
                "context InitCtx2 initiated by SupportBean_S2 as s2 terminated after 10 seconds";
        epService.getEPAdministrator().createEPL(eplCtx);

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1,c2,c3,c4".split(",");
        EPStatementSPI stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.InitCtx0.s0.p00 as c1, context.InitCtx1.s1.p10 as c2, context.InitCtx2.s2.p20 as c3, sum(intPrimitive) as c4 from SupportBean");
        stmtUser.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "S1_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(200, "S2_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "S1_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(200, "S2_2"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 5}});

        sendTimeEvent(epService, "2002-05-1T08:00:05.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "S1_3"));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 9}});

        epService.getEPRuntime().sendEvent(new SupportBean_S2(201, "S2_3"));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_2", 14}, {"S0_2", "S1_2", "S2_3", 5}, {"S0_2", "S1_3", "S2_3", 5}});

        sendTimeEvent(epService, "2002-05-1T08:00:10.000"); // terminate S2_2 leaf

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_3", 11}, {"S0_2", "S1_3", "S2_3", 11}});

        sendTimeEvent(epService, "2002-05-1T08:00:15.000"); // terminate S0_2/S1_2/S2_3 and S0_2/S1_3/S2_3 leafs

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 7));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(201, "S2_4"));
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_2", "S2_4", 8}, {"S0_2", "S1_3", "S2_4", 8}});

        sendTimeEvent(epService, "2002-05-1T08:00:30.000"); // terminate S1_2 branch

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 9));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(105, "S1_5"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(205, "S2_5"));
        epService.getEPRuntime().sendEvent(new SupportBean("E10", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "S1_3", "S2_5", 10}, {"S0_2", "S1_5", "S2_5", 10}});

        sendTimeEvent(epService, "2002-05-1T08:00:60.000"); // terminate S0_2 branch, only the "8to9" is left

        epService.getEPRuntime().sendEvent(new SupportBean("E11", 11));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(6, "S0_6"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(106, "S1_6"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(206, "S2_6"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_6", "S1_6", "S2_6", 12}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(7, "S0_7"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(107, "S1_7"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(207, "S2_7"));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 13));
        assertEquals(4, listener.getAndResetLastNewData().length);

        sendTimeEvent(epService, "2002-05-1T10:00:00.000"); // terminate all

        epService.getEPRuntime().sendEvent(new SupportBean("E14", 14));
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-2T08:00:00.000"); // start next day

        epService.getEPRuntime().sendEvent(new SupportBean_S0(8, "S0_8"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(108, "S1_8"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(208, "S2_8"));
        epService.getEPRuntime().sendEvent(new SupportBean("E15", 15));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_8", "S1_8", "S2_8", 15}});

        stmtUser.stop();
        epService.getEPRuntime().sendEvent(new SupportBean("E16", 16));
        assertFalse(listener.isInvoked());
        assertEquals(0, filterSPI.getFilterCountApprox());
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        AgentInstanceAssertionUtil.assertInstanceCounts(stmtUser.getStatementContext(), 0);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTemporalOverlapOverPartition(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        String eplCtx = "create context NestedContext as " +
                "context InitCtx initiated by SupportBean_S0(id > 0) as s0 terminated after 10 seconds, " +
                "context SegmCtx as partition by theString from SupportBean(intPrimitive > 0)";
        epService.getEPAdministrator().createEPL(eplCtx);

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1,c2,c3".split(",");
        EPStatementSPI stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.InitCtx.s0.p00 as c1, context.SegmCtx.key1 as c2, sum(intPrimitive) as c3 from SupportBean");
        stmtUser.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "E3", 3});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "E4", 4});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "E3", 8});

        sendTimeEvent(epService, "2002-05-1T08:00:05.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-2, "S0_3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_4"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "E3", 14}, {"S0_4", "E3", 6}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 7));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S0_2", "E4", 11}, {"S0_4", "E4", 7}});

        sendTimeEvent(epService, "2002-05-1T08:00:10.000"); // expires first context

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 8));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_4", "E3", 14});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_4", "E4", 16});

        sendTimeEvent(epService, "2002-05-1T08:00:15.000"); // expires second context

        epService.getEPRuntime().sendEvent(new SupportBean("Ex", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_5"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", -10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_5", "E4", 10});

        sendTimeEvent(epService, "2002-05-1T08:00:25.000"); // expires second context

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 10));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion3ContextsTermporalOverCategoryOverPartition(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        String eplCtx = "create context NestedContext as " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context ByCat as group intPrimitive<0 as g1, group intPrimitive=0 as g2, group intPrimitive>0 as g3 from SupportBean, " +
                "context SegmentedByString as partition by theString from SupportBean";
        EPStatement stmtCtx = epService.getEPAdministrator().createEPL(eplCtx);

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1,c2,c3".split(",");
        EPStatementSPI stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.ByCat.label as c1, context.SegmentedByString.key1 as c2, sum(longPrimitive) as c3 from SupportBean");
        stmtUser.addListener(listener);

        tryAssertion3Contexts(epService, listener, fields, "2002-05-1T09:00:00.000");

        stmtCtx.destroy();
        stmtUser.destroy();

        sendTimeEvent(epService, "2002-05-2T08:00:00.000");

        // test SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplCtx);
        assertEquals(eplCtx, model.toEPL());
        EPStatement stmtCtxTwo = epService.getEPAdministrator().create(model);
        assertEquals(eplCtx, stmtCtxTwo.getText());

        stmtUser = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.ByCat.label as c1, context.SegmentedByString.key1 as c2, sum(longPrimitive) as c3 from SupportBean");
        stmtUser.addListener(listener);

        tryAssertion3Contexts(epService, listener, fields, "2002-05-2T09:00:00.000");

        epService.getEPAdministrator().destroyAllStatements();
    }

    /**
     * Root: Temporal
     * Sub: Hash
     */
    private void runAssertionTemporalFixedOverHash(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        sendTimeEvent(epService, "2002-05-1T07:00:00.000");

        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context HashedCtx coalesce hash_code(intPrimitive) from SupportBean granularity 10 preallocate");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1,c2".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "theString as c1, count(*) as c2 from SupportBean group by theString");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:00.000"); // start context

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendTimeEvent(epService, "2002-05-1T09:00:00.000"); // terminate

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-2T08:00:00.000"); // start context

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    /**
     * Root: Category
     * Sub: Initiated
     */
    private void runAssertionCategoryOverTemporalOverlapping(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");

        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context ByCat " +
                "  group intPrimitive < 0 and intPrimitive != -9999 as g1, " +
                "  group intPrimitive = 0 as g2, " +
                "  group intPrimitive > 0 as g3 from SupportBean, " +
                "context InitGrd initiated by SupportBean(theString like 'init%') as sb terminated after 10 seconds");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1,c2,c3".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.ByCat.label as c1, context.InitGrd.sb.theString as c2, count(*) as c3 from SupportBean");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("init_1", -9999));
        epService.getEPRuntime().sendEvent(new SupportBean("X100", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("X101", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("X102", -10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("init_2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g2", "init_2", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g2", "init_2", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("init_3", -2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g1", "init_3", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g1", "init_3", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g1", "init_3", 3L});

        sendTimeEvent(epService, "2002-05-1T08:11:00.000"); // terminates all

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 0));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    /**
     * Root: Fixed temporal
     * Sub: Partition by string
     * <p>
     * - Root starts deactivated.
     * - With context destroy before statement destroy
     */
    private void runAssertionFixedTemporalOverPartitioned(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        sendTimeEvent(epService, "2002-05-1T07:00:00.000");

        EPStatement stmtCtx = epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegmentedByAString partition by theString from SupportBean");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select count(*) as c1 from SupportBean");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());
        assertEquals(0, filterSPI.getFilterCountApprox());
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());

        // starts EightToNine context
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        assertEquals(1, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});
        assertEquals(2, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});
        assertEquals(3, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L});
        assertEquals(3, filterSPI.getFilterCountApprox());
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());

        // ends EightToNine context
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        assertEquals(0, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertFalse(listener.isInvoked());

        // starts EightToNine context
        sendTimeEvent(epService, "2002-05-2T08:00:00.000");
        assertEquals(1, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2, 0, 0, 0);

        stmtCtx.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L});

        statement.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertFalse(listener.isInvoked());
        assertEquals(0, filterSPI.getFilterCountApprox());
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);
        epService.getEPAdministrator().destroyAllStatements();
    }

    /**
     * Root: Partition by string
     * Sub: Fixed temporal
     * <p>
     * - Sub starts deactivated.
     * - With statement destroy before context destroy
     */
    private void runAssertionPartitionedOverFixedTemporal(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        sendTimeEvent(epService, "2002-05-1T07:00:00.000");

        EPStatement stmtCtx = epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context SegmentedByAString partition by theString from SupportBean, " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *)");
        assertEquals(0, filterSPI.getFilterCountApprox());
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c1".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select count(*) as c1 from SupportBean");
        statement.addListener(listener);
        assertEquals(1, filterSPI.getFilterCountApprox());
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertFalse(listener.isInvoked());
        assertEquals(1, filterSPI.getFilterCountApprox());
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());

        // starts EightToNine context
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        assertEquals(2, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});
        assertEquals(2, filterSPI.getFilterCountApprox());
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});
        assertEquals(3, filterSPI.getFilterCountApprox());
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L});
        assertEquals(3, filterSPI.getFilterCountApprox());

        // ends EightToNine context
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        assertEquals(1, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertFalse(listener.isInvoked());
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());

        // starts EightToNine context
        sendTimeEvent(epService, "2002-05-2T08:00:00.000");
        assertEquals(3, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2, 0, 0, 0);
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());

        statement.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertFalse(listener.isInvoked());
        assertEquals(0, filterSPI.getFilterCountApprox());
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        stmtCtx.destroy();
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);

        epService.getEPAdministrator().destroyAllStatements();
    }

    /**
     * Test nested context properties.
     * <p>
     * Root: Fixed temporal
     * Sub: Partition by string
     * <p>
     * - fixed temportal starts active
     * - starting and stopping statement
     */
    private void runAssertionContextProps(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        sendTimeEvent(epService, "2002-05-1T08:30:00.000");

        EPStatement stmtCtx = epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegmentedByAString partition by theString from SupportBean");

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select " +
                "context.EightToNine.name as c0, " +
                "context.EightToNine.startTime as c1, " +
                "context.SegmentedByAString.name as c2, " +
                "context.SegmentedByAString.key1 as c3, " +
                "context.name as c4, " +
                "intPrimitive as c5," +
                "count(*) as c6 " +
                "from SupportBean");
        statement.addListener(listener);
        assertEquals(1, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EightToNine", DateTime.parseDefaultMSec("2002-05-1T08:30:00.000"),
            "SegmentedByAString", "E1",
            "NestedContext",
            10, 1L});
        assertEquals(2, filterSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EightToNine", DateTime.parseDefaultMSec("2002-05-1T08:30:00.000"),
            "SegmentedByAString", "E2",
            "NestedContext",
            20, 1L});
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2, 0, 0, 0);

        statement.stop();
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(0, filterSPI.getFilterCountApprox());

        statement.start();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EightToNine", DateTime.parseDefaultMSec("2002-05-1T08:30:00.000"),
            "SegmentedByAString", "E2",
            "NestedContext",
            30, 1L});
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(2, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1, 0, 0, 0);

        statement.destroy();
        stmtCtx.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 30));
        assertFalse(listener.isInvoked());
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(0, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);

        epService.getEPAdministrator().destroyAllStatements();
    }

    /**
     * Test late-coming statement.
     * <p>
     * Root: Fixed temporal
     * Sub: Partition by string
     */
    private void runAssertionLateComingStatement(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:30:00.000");

        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context EightToNine as start (0, 8, *, *, *) end (0, 9, *, *, *), " +
                "context SegmentedByAString partition by theString from SupportBean");

        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String[] fields = "c0,c1".split(",");
        EPStatementSPI statementOne = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select theString as c0, count(*) as c1 from SupportBean");
        statementOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatementSPI statementTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select theString as c0, sum(intPrimitive) as c1 from SupportBean");
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 2L});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 30));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{"E2", 30});

        SupportUpdateListener listenerThree = new SupportUpdateListener();
        EPStatementSPI statementThree = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext select theString as c0, min(intPrimitive) as c1 from SupportBean");
        statementThree.addListener(listenerThree);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 40));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3L});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{"E1", 60});
        EPAssertionUtil.assertProps(listenerThree.assertOneGetNewAndReset(), fields, new Object[]{"E1", 40});

        statementTwo.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 4L});
        assertFalse(listenerTwo.isInvoked());
        EPAssertionUtil.assertProps(listenerThree.assertOneGetNewAndReset(), fields, new Object[]{"E1", 40});

        statementOne.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -60));
        assertFalse(listenerOne.isInvoked());
        assertFalse(listenerTwo.isInvoked());
        EPAssertionUtil.assertProps(listenerThree.assertOneGetNewAndReset(), fields, new Object[]{"E1", -60});

        statementThree.destroy();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertion3Contexts(EPServiceProvider epService, SupportUpdateListener listener, String[] fields, String subsequentTime) {

        epService.getEPRuntime().sendEvent(makeEvent("E1", 0, 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g2", "E1", 10L});

        assertPartitionInfo(epService);

        epService.getEPRuntime().sendEvent(makeEvent("E2", 0, 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g2", "E2", 11L});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 0, 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g2", "E1", 22L});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 1, 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g3", "E1", 13L});

        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, 14));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g1", "E1", 14L});

        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 15));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"g1", "E2", 15L});

        sendTimeEvent(epService, subsequentTime);

        epService.getEPRuntime().sendEvent(makeEvent("E2", -1, 15));
        assertFalse(listener.isInvoked());
    }

    private void runAssertionPartitionWithMultiPropsAndTerm(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context PartitionedByKeys partition by theString, intPrimitive from SupportBean, " +
                "context InitiateAndTerm start SupportBean as e1 " +
                "end SupportBean_S0(id=e1.intPrimitive and p00=e1.theString)");

        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI statementOne = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NestedContext " +
                "select theString as c0, intPrimitive as c1, count(longPrimitive) as c2 from SupportBean \n" +
                "output last when terminated");
        statementOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 0, 10));
        epService.getEPRuntime().sendEvent(makeEvent("E1", 0, 10));
        epService.getEPRuntime().sendEvent(makeEvent("E2", 1, 1));
        assertFalse(listenerOne.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 0, 2L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedOverlappingAndPattern(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context PartitionedByKeys partition by theString from SupportBean, " +
                "context TimedImmediate initiated @now and pattern[every timer:interval(10)] terminated after 10 seconds");
        tryAssertion(epService);
    }

    private void runAssertionNestedNonOverlapping(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context PartitionedByKeys partition by theString from SupportBean, " +
                "context TimedImmediate start @now end after 10 seconds");
        tryAssertion(epService);
    }

    private void tryAssertion(EPServiceProvider epService) {

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String[] fields = "c0,c1".split(",");
        EPStatement statementOne = epService.getEPAdministrator().createEPL("context NestedContext " +
                "select theString as c0, sum(intPrimitive) as c1 from SupportBean \n" +
                "output last when terminated");
        statementOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getDataListsFlattened(), fields,
                new Object[][]{{"E1", 1}, {"E2", 2}}, null);
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 4));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20000));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getDataListsFlattened(), fields,
                new Object[][]{{"E1", 3}, {"E3", 4}}, null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private Object makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private Object makeEvent(String theString, int intPrimitive, long longPrimitive, boolean boolPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setBoolPrimitive(boolPrimitive);
        return bean;
    }

    private void sendTimeEvent(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private static EPContextPartitionAdminSPI getSpi(EPServiceProvider epService) {
        return (EPContextPartitionAdminSPI) epService.getEPAdministrator().getContextPartitionAdmin();
    }

    public static class MySelectorFilteredNested implements ContextPartitionSelectorFiltered {

        private final Object[] pathMatch;

        private List<Object[]> paths = new ArrayList<Object[]>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<Integer>();

        public MySelectorFilteredNested(Object[] pathMatch) {
            this.pathMatch = pathMatch;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) contextPartitionIdentifier;
            if (pathMatch == null && cpids.contains(nested.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + nested.getContextPartitionId());
            }
            cpids.add(nested.getContextPartitionId());

            ContextPartitionIdentifierInitiatedTerminated first = (ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[0];
            ContextPartitionIdentifierCategory second = (ContextPartitionIdentifierCategory) nested.getIdentifiers()[1];

            Object[] extract = new Object[2];
            extract[0] = ((EventBean) first.getProperties().get("s0")).get("p00");
            extract[1] = second.getLabel();
            paths.add(extract);

            return paths != null && Arrays.equals(pathMatch, extract);
        }
    }

    private void assertPartitionInfo(EPServiceProvider epService) {
        EPContextPartitionAdmin partitionAdmin = epService.getEPAdministrator().getContextPartitionAdmin();
        ContextPartitionCollection partitions = partitionAdmin.getContextPartitions("NestedContext", ContextPartitionSelectorAll.INSTANCE);
        assertEquals(1, partitions.getDescriptors().size());
        ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) partitions.getDescriptors().values().iterator().next().getIdentifier();
        assertNested(nested);
    }

    private void assertNested(ContextPartitionIdentifierNested nested) {
        assertEquals(0, ((ContextPartitionIdentifierInitiatedTerminated)nested.getIdentifiers()[0]).getStartTime());
        assertEquals("g2", ((ContextPartitionIdentifierCategory)nested.getIdentifiers()[1]).getLabel());
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {"E1"}, ((ContextPartitionIdentifierPartitioned)nested.getIdentifiers()[2]).getKeys());
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static class TestEvent implements Serializable {
        private int time;
        private int id;

        public TestEvent(int id, int time) {
            this.id = id;
            this.time = time;
        }

        public int getTime() {
            return time;
        }

        public int getId() {
            return id;
        }
    }

    public static class EndEvent implements Serializable {
        private int id;

        public EndEvent(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
