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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.context.ContextPartitionSelectorSegmented;
import com.espertech.esper.client.context.InvalidContextPartitionSelector;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.context.SupportContextPropUtil;
import com.espertech.esper.supportregression.context.SupportSelectorById;
import com.espertech.esper.supportregression.context.SupportSelectorFilteredInitTerm;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.AgentInstanceAssertionUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextInitTerm implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNoTerminationCondition(epService);
        runAssertionStartZeroInitiatedNow(epService);
        runAssertionEndSameEventAsAnalyzed(epService);
        runAssertionContextPartitionSelection(epService);
        runAssertionFilterInitiatedFilterAllTerminated(epService);
        runAssertionFilterInitiatedFilterTerminatedCorrelatedOutputSnapshot(epService);
        runAssertionScheduleFilterResources(epService);
        runAssertionPatternIntervalZeroInitiatedNow(epService);
        runAssertionPatternInclusion(epService);
        runAssertionPatternInitiatedStraightSelect(epService);
        runAssertionFilterInitiatedStraightEquals(epService);
        runAssertionFilterAllOperators(epService);
        runAssertionFilterBooleanOperator(epService);
        runAssertionTerminateTwoContextSameTime(epService);
        runAssertionOutputSnapshotWhenTerminated(epService);
        runAssertionOutputAllEvery2AndTerminated(epService);
        runAssertionOutputWhenExprWhenTerminatedCondition(epService);
        runAssertionOutputOnlyWhenTerminatedCondition(epService);
        runAssertionOutputOnlyWhenSetAndWhenTerminatedSet(epService);
        runAssertionOutputOnlyWhenTerminatedThenSet(epService);
        runAssertionCrontab(epService);
        runAssertionStartNowCalMonthScoped(epService);
    }

    private void runAssertionNoTerminationCondition(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5));

        tryAssertionNoTerminationConditionOverlapping(epService, false);
        tryAssertionNoTerminationConditionOverlapping(epService, true);

        tryAssertionNoTerminationConditionNonoverlapping(epService, false);
        tryAssertionNoTerminationConditionNonoverlapping(epService, true);

        tryAssertionNoTerminationConditionNested(epService, false);
        tryAssertionNoTerminationConditionNested(epService, true);
    }

    private void runAssertionStartZeroInitiatedNow(EPServiceProvider epService) {
        String[] fieldsOne = "c0,c1".split(",");

        // test start-after with immediate start
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String contextExpr = "create context CtxPerId start after 0 sec end after 60 sec";
        epService.getEPAdministrator().createEPL(contextExpr);
        EPStatement stream = epService.getEPAdministrator().createEPL("context CtxPerId select theString as c0, intPrimitive as c1 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stream.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(59999));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E2", 2});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternIntervalZeroInitiatedNow(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextInitTerm.class)) {
            return;
        }

        String[] fieldsOne = "c0,c1".split(",");

        // test initiated-by pattern with immediate start
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000));
        String contextExprTwo = "create context CtxPerId initiated by pattern [timer:interval(0) or every timer:interval(1 min)] terminated after 60 sec";
        epService.getEPAdministrator().createEPL(contextExprTwo);
        EPStatement streamTwo = epService.getEPAdministrator().createEPL("context CtxPerId select theString as c0, sum(intPrimitive) as c1 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        streamTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E1", 10});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000 + 59999));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E2", 30});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000 + 60000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E3", 4});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternInclusion(EPServiceProvider epService) {
        String[] fields = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String contextExpr = "create context CtxPerId initiated by pattern [every-distinct (a.theString, 10 sec) a=SupportBean]@Inclusive terminated after 10 sec ";
        epService.getEPAdministrator().createEPL(contextExpr);
        String streamExpr = "context CtxPerId select * from SupportBean(theString = context.a.theString) output last when terminated";
        EPStatement stream = epService.getEPAdministrator().createEPL(streamExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stream.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(8000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(9999));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10100));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 4});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(16100));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 6));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20099));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 5});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(26100 - 1));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(26100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 6});

        epService.getEPAdministrator().destroyAllStatements();

        // test multiple pattern with multiple events
        String contextExprMulti = "create context CtxPerId initiated by pattern [every a=SupportBean_S0 -> b=SupportBean_S1]@Inclusive terminated after 10 sec ";
        epService.getEPAdministrator().createEPL(contextExprMulti);
        String streamExprMulti = "context CtxPerId select * from pattern [every a=SupportBean_S0 -> b=SupportBean_S1]";
        EPStatement streamMulti = epService.getEPAdministrator().createEPL(streamExprMulti);
        streamMulti.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "S1_1"));
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEndSameEventAsAnalyzed(EPServiceProvider epService) {

        // same event terminates - not included
        String[] fields = "c1,c2,c3,c4".split(",");
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "start SupportBean " +
                "end SupportBean(intPrimitive=11)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx " +
                "select min(intPrimitive) as c1, max(intPrimitive) as c2, sum(intPrimitive) as c3, avg(intPrimitive) as c4 from SupportBean " +
                "output snapshot when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 10, 10, 10d});

        epService.getEPAdministrator().destroyAllStatements();

        // same event terminates - included
        fields = "c1,c2,c3,c4".split(",");
        epService.getEPAdministrator().createEPL("create schema MyCtxTerminate(theString string)");
        epService.getEPAdministrator().createEPL("create context MyCtx as start SupportBean end MyCtxTerminate");
        stmt = epService.getEPAdministrator().createEPL("context MyCtx " +
                "select min(intPrimitive) as c1, max(intPrimitive) as c2, sum(intPrimitive) as c3, avg(intPrimitive) as c4 from SupportBean " +
                "output snapshot when terminated");
        stmt.addListener(listener);
        epService.getEPAdministrator().createEPL("insert into MyCtxTerminate select theString from SupportBean(intPrimitive=11)");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 11, 21, 10.5d});

        // test with audit
        String epl = "@Audit create context AdBreakCtx as initiated by SupportBean(intPrimitive > 0) as ad " +
                " terminated by SupportBean(theString=ad.theString, intPrimitive < 0) as endAd";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPAdministrator().createEPL("context AdBreakCtx select count(*) from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -10));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextPartitionSelection(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3".split(",");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context MyCtx as initiated by SupportBean_S0 s0 terminated by SupportBean_S1(id=s0.id)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select context.id as c0, context.s0.p00 as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#keepall group by theString");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        SupportBean_S0 initOne = new SupportBean_S0(1, "S0_1");
        epService.getEPRuntime().sendEvent(initOne);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        SupportBean_S0 initTwo = new SupportBean_S0(2, "S0_2");
        epService.getEPRuntime().sendEvent(initTwo);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 101));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), stmt.safeIterator(), fields, new Object[][]{{0, "S0_1", "E1", 6}, {0, "S0_1", "E2", 10}, {0, "S0_1", "E3", 201}, {1, "S0_2", "E1", 3}, {1, "S0_2", "E3", 201}});
        SupportContextPropUtil.assertContextProps(epService, "MyCtx", new int[] {0, 1}, "startTime,endTime,s0", new Object[][] {{1000L, null, initOne}, {2000L, null, initTwo}});

        // test iterator targeted by context partition id
        SupportSelectorById selectorById = new SupportSelectorById(Collections.singleton(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(selectorById), stmt.safeIterator(selectorById), fields, new Object[][]{{1, "S0_2", "E1", 3}, {1, "S0_2", "E3", 201}});

        // test iterator targeted by property on triggering event
        SupportSelectorFilteredInitTerm filtered = new SupportSelectorFilteredInitTerm("S0_2");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(filtered), stmt.safeIterator(filtered), fields, new Object[][]{{1, "S0_2", "E1", 3}, {1, "S0_2", "E3", 201}});

        // test always-false filter - compare context partition info
        filtered = new SupportSelectorFilteredInitTerm(null);
        assertFalse(stmt.iterator(filtered).hasNext());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{1000L, 2000L}, filtered.getContextsStartTimes());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"S0_1", "S0_2"}, filtered.getP00PropertyValues());

        try {
            stmt.iterator(new ContextPartitionSelectorSegmented() {
                public List<Object[]> getPartitionKeys() {
                    return null;
                }
            });
            fail();
        } catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById] interfaces but received com."));
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterInitiatedFilterAllTerminated(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create context MyContext as " +
                "initiated by SupportBean_S0 " +
                "terminated by SupportBean_S1");

        String[] fields = "c1".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context MyContext select sum(intPrimitive) as c1 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0_1")); // initiate one

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "S0_2"));  // initiate another

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{5}, {3}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{9}, {7}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "S1_1"));  // terminate all
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterInitiatedFilterTerminatedCorrelatedOutputSnapshot(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as " +
                "initiated by SupportBean_S0 as s0 " +
                "terminated by SupportBean_S1(p10 = s0.p00)");

        String[] fields = "c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall output snapshot when terminated");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "G1"));    // starts it
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "GX"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "G1"));  // terminate
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "G2"));    // starts new one
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "G3"));    // also starts new one

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "G2"));  // terminate G2
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 15});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "G3"));  // terminate G3
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 11});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionScheduleFilterResources(EPServiceProvider epService) {
        // test no-context statement
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#time(30)");
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());

        stmt.destroy();
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        // test initiated
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) spi.getFilterService();

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        String eplCtx = "create context EverySupportBean as " +
                "initiated by SupportBean as sb " +
                "terminated after 1 minutes";
        epService.getEPAdministrator().createEPL(eplCtx);

        epService.getEPAdministrator().createEPL("context EverySupportBean select * from SupportBean_S0#time(2 min) sb0");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(1, filterServiceSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(2, filterServiceSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "S0_1"));
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(2, filterServiceSPI.getFilterCountApprox());

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(1, filterServiceSPI.getFilterCountApprox());

        epService.getEPAdministrator().destroyAllStatements();
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(0, filterServiceSPI.getFilterCountApprox());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternInitiatedStraightSelect(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        String eplCtx = "create context EverySupportBean as " +
                "initiated by pattern [every (a=SupportBean_S0 or b=SupportBean_S1)] " +
                "terminated after 1 minutes";
        epService.getEPAdministrator().createEPL(eplCtx);

        String[] fields = "c1,c2,c3".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select context.a.id as c1, context.b.id as c2, theString as c3 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 2, "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{null, 2, "E2"}, {3, null, "E2"}});

        epService.getEPAdministrator().destroyAllStatements();

        // test SODA
        assertSODA(epService, eplCtx);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterInitiatedStraightEquals(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        String ctxEPL = "create context EverySupportBean as " +
                "initiated by SupportBean(theString like \"I%\") as sb " +
                "terminated after 1 minutes";
        epService.getEPAdministrator().createEPL(ctxEPL);

        String[] fields = "c1".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select sum(longPrimitive) as c1 from SupportBean(intPrimitive = context.sb.intPrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", -1, -2L));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(makeEvent("I1", 2, 4L)); // counts towards stuff
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4L});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 2, 3L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{7L});

        epService.getEPRuntime().sendEvent(makeEvent("I2", 3, 14L)); // counts towards stuff
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{14L});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 2, 2L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{9L});

        epService.getEPRuntime().sendEvent(makeEvent("E4", 3, 15L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{29L});

        sendTimeEvent(epService, "2002-05-1T08:01:30.000");

        epService.getEPRuntime().sendEvent(makeEvent("E", -1, -2L));
        assertFalse(listener.isInvoked());

        // test SODA
        epService.getEPAdministrator().destroyAllStatements();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(ctxEPL);
        assertEquals(ctxEPL, model.toEPL());
        EPStatement stmtModel = epService.getEPAdministrator().create(model);
        assertEquals(ctxEPL, stmtModel.getText());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterAllOperators(EPServiceProvider epService) {

        // test plain
        epService.getEPAdministrator().createEPL("create context EverySupportBean as " +
                "initiated by SupportBean_S0 as sb " +
                "terminated after 10 days 5 hours 2 minutes 1 sec 11 milliseconds");

        tryOperator(epService, "context.sb.id = intBoxed", new Object[][]{{10, true}, {9, false}, {null, false}});
        tryOperator(epService, "intBoxed = context.sb.id", new Object[][]{{10, true}, {9, false}, {null, false}});

        tryOperator(epService, "context.sb.id > intBoxed", new Object[][]{{11, false}, {10, false}, {9, true}, {8, true}});
        tryOperator(epService, "context.sb.id >= intBoxed", new Object[][]{{11, false}, {10, true}, {9, true}, {8, true}});
        tryOperator(epService, "context.sb.id < intBoxed", new Object[][]{{11, true}, {10, false}, {9, false}, {8, false}});
        tryOperator(epService, "context.sb.id <= intBoxed", new Object[][]{{11, true}, {10, true}, {9, false}, {8, false}});

        tryOperator(epService, "intBoxed < context.sb.id", new Object[][]{{11, false}, {10, false}, {9, true}, {8, true}});
        tryOperator(epService, "intBoxed <= context.sb.id", new Object[][]{{11, false}, {10, true}, {9, true}, {8, true}});
        tryOperator(epService, "intBoxed > context.sb.id", new Object[][]{{11, true}, {10, false}, {9, false}, {8, false}});
        tryOperator(epService, "intBoxed >= context.sb.id", new Object[][]{{11, true}, {10, true}, {9, false}, {8, false}});

        tryOperator(epService, "intBoxed in (context.sb.id)", new Object[][]{{11, false}, {10, true}, {9, false}, {8, false}});
        tryOperator(epService, "intBoxed between context.sb.id and context.sb.id", new Object[][]{{11, false}, {10, true}, {9, false}, {8, false}});

        tryOperator(epService, "context.sb.id != intBoxed", new Object[][]{{10, false}, {9, true}, {null, false}});
        tryOperator(epService, "intBoxed != context.sb.id", new Object[][]{{10, false}, {9, true}, {null, false}});

        tryOperator(epService, "intBoxed not in (context.sb.id)", new Object[][]{{11, true}, {10, false}, {9, true}, {8, true}});
        tryOperator(epService, "intBoxed not between context.sb.id and context.sb.id", new Object[][]{{11, true}, {10, false}, {9, true}, {8, true}});

        tryOperator(epService, "context.sb.id is intBoxed", new Object[][]{{10, true}, {9, false}, {null, false}});
        tryOperator(epService, "intBoxed is context.sb.id", new Object[][]{{10, true}, {9, false}, {null, false}});

        tryOperator(epService, "context.sb.id is not intBoxed", new Object[][]{{10, false}, {9, true}, {null, true}});
        tryOperator(epService, "intBoxed is not context.sb.id", new Object[][]{{10, false}, {9, true}, {null, true}});

        // try coercion
        tryOperator(epService, "context.sb.id = shortBoxed", new Object[][]{{(short) 10, true}, {(short) 9, false}, {null, false}});
        tryOperator(epService, "shortBoxed = context.sb.id", new Object[][]{{(short) 10, true}, {(short) 9, false}, {null, false}});

        tryOperator(epService, "context.sb.id > shortBoxed", new Object[][]{{(short) 11, false}, {(short) 10, false}, {(short) 9, true}, {(short) 8, true}});
        tryOperator(epService, "shortBoxed < context.sb.id", new Object[][]{{(short) 11, false}, {(short) 10, false}, {(short) 9, true}, {(short) 8, true}});

        tryOperator(epService, "shortBoxed in (context.sb.id)", new Object[][]{{(short) 11, false}, {(short) 10, true}, {(short) 9, false}, {(short) 8, false}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOperator(EPServiceProvider epService, String operator, Object[][] testdata) {
        FilterServiceSPI filterSpi = (FilterServiceSPI) ((EPServiceProviderSPI) epService).getFilterService();

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select theString as c0,intPrimitive as c1,context.sb.p00 as c2 " +
                "from SupportBean(" + operator + ")");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // initiate
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S01"));

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean();
            Object testValue = testdata[i][0];
            if (testValue instanceof Integer) {
                bean.setIntBoxed((Integer) testValue);
            } else {
                bean.setShortBoxed((Short) testValue);
            }
            boolean expected = (Boolean) testdata[i][1];

            epService.getEPRuntime().sendEvent(bean);
            assertEquals("Failed at " + i, expected, listener.getAndClearIsInvoked());
        }

        // assert type of expression
        if (filterSpi.isSupportsTakeApply()) {
            FilterSet set = filterSpi.take(Collections.singleton(stmt.getStatementId()));
            assertEquals(1, set.getFilters().size());
            FilterValueSet valueSet = set.getFilters().get(0).getFilterValueSet();
            assertEquals(1, valueSet.getParameters().length);
            FilterValueSetParam para = valueSet.getParameters()[0][0];
            assertTrue(para.getFilterOperator() != FilterOperator.BOOLEAN_EXPRESSION);
        }

        stmt.destroy();
    }

    private void runAssertionFilterBooleanOperator(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context EverySupportBean as " +
                "initiated by SupportBean_S0 as sb " +
                "terminated after 10 days 5 hours 2 minutes 1 sec 11 milliseconds");

        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select theString as c0,intPrimitive as c1,context.sb.p00 as c2 " +
                "from SupportBean(intPrimitive + context.sb.id = 5)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S01"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2, "S01"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S02"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3", 2, "S01"}, {"E3", 2, "S02"}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "S03"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 2, "S01"}, {"E4", 2, "S02"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"E5", 1, "S03"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTerminateTwoContextSameTime(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        String eplContext = "@Name('CTX') create context CtxInitiated " +
                "initiated by SupportBean_S0 as sb0 " +
                "terminated after 1 minute";
        epService.getEPAdministrator().createEPL(eplContext);
        SupportUpdateListener listener = new SupportUpdateListener();

        String[] fields = "c1,c2,c3".split(",");
        String eplGrouped = "@Name('S1') context CtxInitiated select theString as c1, sum(intPrimitive) as c2, context.sb0.p00 as c3 from SupportBean";
        epService.getEPAdministrator().createEPL(eplGrouped).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "SB01"));

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 2, "SB01"});

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 5, "SB01"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "SB02"));

        epService.getEPRuntime().sendEvent(new SupportBean("G4", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"G4", 9, "SB01"}, {"G4", 4, "SB02"}});

        epService.getEPRuntime().sendEvent(new SupportBean("G5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"G5", 14, "SB01"}, {"G5", 9, "SB02"}});

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean("G6", 6));
        assertFalse(listener.getAndClearIsInvoked());

        // clean up
        epService.getEPAdministrator().getStatement("S1").destroy();
        epService.getEPAdministrator().getStatement("CTX").destroy();
    }

    private void runAssertionOutputSnapshotWhenTerminated(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and snapshot
        String[] fields = "c1".split(",");
        String epl = "context EveryMinute select sum(intPrimitive) as c1 from SupportBean output snapshot when terminated";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendTimeEvent(epService, "2002-05-1T08:01:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendTimeEvent(epService, "2002-05-1T08:01:59.999");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate
        sendTimeEvent(epService, "2002-05-1T08:02:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1 + 2 + 3});

        sendTimeEvent(epService, "2002-05-1T08:02:01.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        assertFalse(listener.getAndClearIsInvoked());

        assertSODA(epService, epl);

        // terminate
        sendTimeEvent(epService, "2002-05-1T08:03:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4 + 5 + 6});

        stmt.destroy();

        // test late-coming statement without "terminated"
        EPStatementSPI stmtTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EveryMinute " +
                "select context.id as c0, sum(intPrimitive) as c1 from SupportBean output snapshot every 2 events");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E10", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E11", 2));
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-1T08:04:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E12", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E13", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{7});

        // terminate
        sendTimeEvent(epService, "2002-05-1T08:05:00.000");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputAllEvery2AndTerminated(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and every 2 events output all with group by
        String[] fields = "c1,c2".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EveryMinute " +
                "select theString as c1, sum(intPrimitive) as c2 from SupportBean group by theString output all every 2 events and when terminated order by theString asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendTimeEvent(epService, "2002-05-1T08:01:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 1 + 2}});

        sendTimeEvent(epService, "2002-05-1T08:01:59.999");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate
        sendTimeEvent(epService, "2002-05-1T08:02:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 1 + 2}, {"E2", 3}});

        sendTimeEvent(epService, "2002-05-1T08:02:01.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 4}, {"E5", 5}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 14}, {"E5", 5}, {"E6", 6}});

        // terminate
        sendTimeEvent(epService, "2002-05-1T08:03:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 14}, {"E5", 5}, {"E6", 6}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", -2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", -1}, {"E6", -2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputWhenExprWhenTerminatedCondition(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and every 2 events output all with group by
        String[] fields = "c0".split(",");
        String epl = "context EveryMinute " +
                "select theString as c0 from SupportBean output when count_insert>1 and when terminated and count_insert>0";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}});

        sendTimeEvent(epService, "2002-05-1T08:01:59.999");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate, new context partition
        sendTimeEvent(epService, "2002-05-1T08:02:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3"}});

        sendTimeEvent(epService, "2002-05-1T08:02:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4"}, {"E5"}});

        sendTimeEvent(epService, "2002-05-1T08:03:00.000");
        assertFalse(listener.isInvoked());

        assertSODA(epService, epl);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputOnlyWhenTerminatedCondition(EPServiceProvider epService) {

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and every 2 events output all with group by
        String[] fields = "c0".split(",");
        String epl = "context EveryMinute " +
                "select theString as c0 from SupportBean output when terminated and count_insert > 0";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        // terminate, new context partition
        sendTimeEvent(epService, "2002-05-1T08:02:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}});

        // terminate, new context partition
        sendTimeEvent(epService, "2002-05-1T08:03:00.000");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputOnlyWhenSetAndWhenTerminatedSet(EPServiceProvider epService) {

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // include then-set and both real-time and terminated output
        epService.getEPAdministrator().createEPL("create variable int myvar = 0");
        String eplOne = "context EveryMinute select theString as c0 from SupportBean " +
                "output when true " +
                "then set myvar=1 " +
                "and when terminated " +
                "then set myvar=2";
        EPStatementSPI stmtOne = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertEquals(1, epService.getEPRuntime().getVariableValue("myvar"));
        assertTrue(listener.getAndClearIsInvoked());

        sendTimeEvent(epService, "2002-05-1T08:02:00.000"); // terminate, new context partition
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(2, epService.getEPRuntime().getVariableValue("myvar"));

        assertSODA(epService, eplOne);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputOnlyWhenTerminatedThenSet(EPServiceProvider epService) {

        String[] fields = "c0".split(",");
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create variable int myvar = 0");
        epService.getEPAdministrator().createEPL("create context EverySupportBeanS0 as " +
                "initiated by SupportBean_S0 as s0 " +
                "terminated after 1 min");

        // include only-terminated output with set
        epService.getEPRuntime().setVariableValue("myvar", 0);
        String eplTwo = "context EverySupportBeanS0 select theString as c0 from SupportBean " +
                "output when terminated " +
                "then set myvar=10";
        EPStatementSPI stmtTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        // terminate, new context partition
        sendTimeEvent(epService, "2002-05-1T08:01:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4"}});
        assertEquals(10, epService.getEPRuntime().getVariableValue("myvar"));

        assertSODA(epService, eplTwo);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCrontab(EPServiceProvider epService) {
        FilterServiceSPI filterSPI = (FilterServiceSPI) ((EPServiceProviderSPI) epService).getFilterService();
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 3 min");

        String[] fields = "c1,c2".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("@IterableUnbound context EveryMinute select theString as c1, sum(intPrimitive) as c2 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.getAndClearIsInvoked());
        assertEquals(0, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, null);

        sendTimeEvent(epService, "2002-05-1T08:01:00.000");

        assertEquals(1, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        Object[][] expected = new Object[][]{{"E2", 5}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent(epService, "2002-05-1T08:01:59.999");

        assertEquals(1, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 6));
        expected = new Object[][]{{"E3", 11}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent(epService, "2002-05-1T08:02:00.000");

        assertEquals(2, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2);
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 7));
        expected = new Object[][]{{"E4", 18}, {"E4", 7}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent(epService, "2002-05-1T08:02:59.999");

        assertEquals(2, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2);
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 8));
        expected = new Object[][]{{"E5", 26}, {"E5", 15}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent(epService, "2002-05-1T08:03:00.000");

        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 3);
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 9));
        expected = new Object[][]{{"E6", 35}, {"E6", 24}, {"E6", 9}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent(epService, "2002-05-1T08:04:00.000");

        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 3);
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 10));
        expected = new Object[][]{{"E7", 34}, {"E7", 19}, {"E7", 10}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent(epService, "2002-05-1T08:05:00.000");

        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 3);
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 11));
        expected = new Object[][]{{"E8", 30}, {"E8", 21}, {"E8", 11}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        // assert certain keywords are valid: last keyword, timezone
        epService.getEPAdministrator().createEPL("create context CtxMonthly1 start (0, 0, 1, *, *, 0) end(59, 23, last, *, *, 59)");
        epService.getEPAdministrator().createEPL("create context CtxMonthly2 start (0, 0, 1, *, *) end(59, 23, last, *, *)");
        epService.getEPAdministrator().createEPL("create context CtxMonthly3 start (0, 0, 1, *, *, 0, 'GMT-5') end(59, 23, last, *, *, 59, 'GMT-8')");
        tryInvalid(epService, "create context CtxMonthly4 start (0) end(*,*,*,*,*)",
                "Error starting statement: Invalid schedule specification: Invalid number of crontab parameters, expecting between 5 and 7 parameters, received 1 [create context CtxMonthly4 start (0) end(*,*,*,*,*)]");
        tryInvalid(epService, "create context CtxMonthly4 start (*,*,*,*,*) end(*,*,*,*,*,*,*,*)",
                "Error starting statement: Invalid schedule specification: Invalid number of crontab parameters, expecting between 5 and 7 parameters, received 8 [create context CtxMonthly4 start (*,*,*,*,*) end(*,*,*,*,*,*,*,*)]");

        // test invalid -after
        tryInvalid(epService, "create context CtxMonthly4 start after 1 second end after -1 seconds",
                "Error starting statement: Invalid negative time period expression '-1 seconds' [create context CtxMonthly4 start after 1 second end after -1 seconds]");
        tryInvalid(epService, "create context CtxMonthly4 start after -1 second end after 1 seconds",
                "Error starting statement: Invalid negative time period expression '-1 seconds' [create context CtxMonthly4 start after -1 second end after 1 seconds]");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStartNowCalMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        epService.getEPAdministrator().createEPL("create context MyCtx start SupportBean_S1 end after 1 month");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("context MyCtx select * from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listener.getAndClearIsInvoked());

        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertTrue(listener.getAndClearIsInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionNoTerminationConditionOverlapping(EPServiceProvider epService, boolean soda) {

        SupportModelHelper.createByCompileOrParse(epService, soda, "create context SupportBeanInstanceCtx as initiated by SupportBean as sb");
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, "context SupportBeanInstanceCtx " +
                "select id, context.sb.intPrimitive as sbint, context.startTime as starttime, context.endTime as endtime from SupportBean_S0(p00=context.sb.theString)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "id,sbint,starttime,endtime".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("P1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("P2", 200));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "P2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 200, 5L, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 100, 5L, null});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionNoTerminationConditionNonoverlapping(EPServiceProvider epService, boolean soda) {

        SupportModelHelper.createByCompileOrParse(epService, soda, "create context SupportBeanInstanceCtx as start SupportBean as sb");
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, "context SupportBeanInstanceCtx " +
                "select id, context.sb.intPrimitive as sbint, context.startTime as starttime, context.endTime as endtime from SupportBean_S0(p00=context.sb.theString)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "id,sbint,starttime,endtime".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("P1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("P2", 200));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "P2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 100, 5L, null});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionNoTerminationConditionNested(EPServiceProvider epService, boolean soda) {

        SupportModelHelper.createByCompileOrParse(epService, soda, "create context MyCtx as " +
                "context Lvl1Ctx as start SupportBean_S0 as s0, " +
                "context Lvl2Ctx as start SupportBean_S1 as s1");

        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx " +
                "select theString, context.Lvl1Ctx.s0.p00 as p00, context.Lvl2Ctx.s1.p10 as p10 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,p00,p10".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("P1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean("P1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "B"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "A", "B"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendTimeEvent(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private void assertSODA(EPServiceProvider epService, String epl) {
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtModel = epService.getEPAdministrator().create(model);
        assertEquals(epl, stmtModel.getText());
        stmtModel.destroy();
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }

    public static class Event implements Serializable {
        private final String productID;

        public Event(String productId) {
            this.productID = productId;
        }

        public String getProductID() {
            return productID;
        }
    }
}
