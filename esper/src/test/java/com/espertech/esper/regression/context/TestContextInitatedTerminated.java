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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.AgentInstanceAssertionUtil;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;

public class TestContextInitatedTerminated extends TestCase {

    private EPServiceProvider epService;
    private EPServiceProviderSPI spi;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
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

    public void testStartZeroInitiatedNow() {
        String[] fieldsOne = "c0,c1".split(",");

        // test start-after with immediate start
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String contextExpr =  "create context CtxPerId start after 0 sec end after 60 sec";
        epService.getEPAdministrator().createEPL(contextExpr);
        EPStatement stream = epService.getEPAdministrator().createEPL("context CtxPerId select theString as c0, intPrimitive as c1 from SupportBean");
        stream.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[] {"E1", 1});
        
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(59999));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[] {"E2", 2});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();

        // test initiated-by pattern with immediate start
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000));
        String contextExprTwo =  "create context CtxPerId initiated by pattern [timer:interval(0) or every timer:interval(1 min)] terminated after 60 sec";
        epService.getEPAdministrator().createEPL(contextExprTwo);
        EPStatement streamTwo = epService.getEPAdministrator().createEPL("context CtxPerId select theString as c0, sum(intPrimitive) as c1 from SupportBean");
        streamTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[] {"E1", 10});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000+59999));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[] {"E2", 30});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000+60000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[] {"E3", 4});
    }

    public void testPatternInclusion() {
        String[] fields = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String contextExpr =  "create context CtxPerId initiated by pattern [every-distinct (a.theString, 10 sec) a=SupportBean]@Inclusive terminated after 10 sec ";
        epService.getEPAdministrator().createEPL(contextExpr);
        String streamExpr = "context CtxPerId select * from SupportBean(theString = context.a.theString) output last when terminated";
        EPStatement stream = epService.getEPAdministrator().createEPL(streamExpr);
        stream.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(8000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(9999));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 3});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10100));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 4});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(16100));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 6));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20099));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 5});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(26100-1));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(26100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 6});

        epService.getEPAdministrator().destroyAllStatements();

        // test multiple pattern with multiple events
        String contextExprMulti =  "create context CtxPerId initiated by pattern [every a=SupportBean_S0 -> b=SupportBean_S1]@Inclusive terminated after 10 sec ";
        epService.getEPAdministrator().createEPL(contextExprMulti);
        String streamExprMulti = "context CtxPerId select * from pattern [every a=SupportBean_S0 -> b=SupportBean_S1]";
        EPStatement streamMulti = epService.getEPAdministrator().createEPL(streamExprMulti);
        streamMulti.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "S1_1"));
        assertTrue(listener.isInvoked());
    }

    public void testEndSameEventAsAnalyzed() {

        // same event terminates - not included
        String[] fields = "c1,c2,c3,c4".split(",");
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "start SupportBean " +
                "end SupportBean(intPrimitive=11)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx " +
                "select min(intPrimitive) as c1, max(intPrimitive) as c2, sum(intPrimitive) as c3, avg(intPrimitive) as c4 from SupportBean " +
                "output snapshot when terminated");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 10, 10, 10d});

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
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 11, 21, 10.5d});

        // test with audit
        String epl = "@Audit create context AdBreakCtx as initiated by SupportBean(intPrimitive > 0) as ad " +
            " terminated by SupportBean(theString=ad.theString, intPrimitive < 0) as endAd";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPAdministrator().createEPL("context AdBreakCtx select count(*) from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -10));
    }

    public void testContextPartitionSelection() {
        String[] fields = "c0,c1,c2,c3".split(",");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context MyCtx as initiated by SupportBean_S0 s0 terminated by SupportBean_S1(id=s0.id)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select context.id as c0, context.s0.p00 as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean.win:keepall() group by theString");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 101));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), stmt.safeIterator(), fields, new Object[][]{{0, "S0_1", "E1", 6}, {0, "S0_1", "E2", 10}, {0, "S0_1", "E3", 201}, {1, "S0_2", "E1", 3}, {1, "S0_2", "E3", 201}});

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
        }
        catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById] interfaces but received com."));
        }
    }

    public void testFilterInitiatedFilterAllTerminated() {

        epService.getEPAdministrator().createEPL("create context MyContext as " +
                "initiated by SupportBean_S0 " +
                "terminated by SupportBean_S1");

        String[] fields = "c1".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context MyContext select sum(intPrimitive) as c1 from SupportBean");
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
    }

    public void testFilterInitiatedFilterTerminatedCorrelatedOutputSnapshot() {
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as " +
                "initiated by SupportBean_S0 as s0 " +
                "terminated by SupportBean_S1(p10 = s0.p00)");

        String[] fields = "c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean.win:keepall() output snapshot when terminated");
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
    }

    public void testScheduleFilterResources() {
        // test no-context statement
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.win:time(30)");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());

        stmt.destroy();
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());

        // test initiated
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) spi.getFilterService();

        sendTimeEvent("2002-05-1T8:00:00.000");
        String eplCtx = "create context EverySupportBean as " +
                "initiated by SupportBean as sb " +
                "terminated after 1 minutes";
        epService.getEPAdministrator().createEPL(eplCtx);

        epService.getEPAdministrator().createEPL("context EverySupportBean select * from SupportBean_S0.win:time(2 min) sb0");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(1, filterServiceSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(2, filterServiceSPI.getFilterCountApprox());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "S0_1"));
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(2, filterServiceSPI.getFilterCountApprox());

        sendTimeEvent("2002-05-1T8:01:00.000");
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(1, filterServiceSPI.getFilterCountApprox());

        epService.getEPAdministrator().destroyAllStatements();
        assertEquals(0, spi.getSchedulingService().getScheduleHandleCount());
        assertEquals(0, filterServiceSPI.getFilterCountApprox());
    }

    public void testPatternInitiatedStraightSelect() {
        sendTimeEvent("2002-05-1T8:00:00.000");
        String eplCtx = "create context EverySupportBean as " +
                "initiated by pattern [every (a=SupportBean_S0 or b=SupportBean_S1)] " +
                "terminated after 1 minutes";
        epService.getEPAdministrator().createEPL(eplCtx);

        String[] fields = "c1,c2,c3".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select context.a.id as c1, context.b.id as c2, theString as c3 from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 2, "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{null, 2, "E2"}, {3, null, "E2"}});

        epService.getEPAdministrator().destroyAllStatements();

        // test SODA
        assertSODA(eplCtx);
    }

    public void testFilterInitiatedStraightEquals() {
        sendTimeEvent("2002-05-1T8:00:00.000");
        String ctxEPL = "create context EverySupportBean as " +
                "initiated by SupportBean(theString like \"I%\") as sb " +
                "terminated after 1 minutes";
        epService.getEPAdministrator().createEPL(ctxEPL);

        String[] fields = "c1".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select sum(longPrimitive) as c1 from SupportBean(intPrimitive = context.sb.intPrimitive)");
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

        sendTimeEvent("2002-05-1T8:01:30.000");

        epService.getEPRuntime().sendEvent(makeEvent("E", -1, -2L));
        assertFalse(listener.isInvoked());

        // test SODA
        epService.getEPAdministrator().destroyAllStatements();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(ctxEPL);
        assertEquals(ctxEPL, model.toEPL());
        EPStatement stmtModel = epService.getEPAdministrator().create(model);
        assertEquals(ctxEPL, stmtModel.getText());
    }

    public void testFilterAllOperators() {

        // test plain
        epService.getEPAdministrator().createEPL("create context EverySupportBean as " +
                "initiated by SupportBean_S0 as sb " +
                "terminated after 10 days 5 hours 2 minutes 1 sec 11 milliseconds");

        tryOperator("context.sb.id = intBoxed", new Object[][] {{10, true}, {9, false}, {null, false}});
        tryOperator("intBoxed = context.sb.id", new Object[][] {{10, true}, {9, false}, {null, false}});

        tryOperator("context.sb.id > intBoxed", new Object[][] {{11, false}, {10, false}, {9, true}, {8, true}});
        tryOperator("context.sb.id >= intBoxed", new Object[][] {{11, false}, {10, true}, {9, true}, {8, true}});
        tryOperator("context.sb.id < intBoxed", new Object[][] {{11, true}, {10, false}, {9, false}, {8, false}});
        tryOperator("context.sb.id <= intBoxed", new Object[][] {{11, true}, {10, true}, {9, false}, {8, false}});

        tryOperator("intBoxed < context.sb.id", new Object[][] {{11, false}, {10, false}, {9, true}, {8, true}});
        tryOperator("intBoxed <= context.sb.id", new Object[][] {{11, false}, {10, true}, {9, true}, {8, true}});
        tryOperator("intBoxed > context.sb.id", new Object[][] {{11, true}, {10, false}, {9, false}, {8, false}});
        tryOperator("intBoxed >= context.sb.id", new Object[][] {{11, true}, {10, true}, {9, false}, {8, false}});

        tryOperator("intBoxed in (context.sb.id)", new Object[][] {{11, false}, {10, true}, {9, false}, {8, false}});
        tryOperator("intBoxed between context.sb.id and context.sb.id", new Object[][] {{11, false}, {10, true}, {9, false}, {8, false}});

        tryOperator("context.sb.id != intBoxed", new Object[][] {{10, false}, {9, true}, {null, false}});
        tryOperator("intBoxed != context.sb.id", new Object[][] {{10, false}, {9, true}, {null, false}});

        tryOperator("intBoxed not in (context.sb.id)", new Object[][] {{11, true}, {10, false}, {9, true}, {8, true}});
        tryOperator("intBoxed not between context.sb.id and context.sb.id", new Object[][] {{11, true}, {10, false}, {9, true}, {8, true}});

        tryOperator("context.sb.id is intBoxed", new Object[][] {{10, true}, {9, false}, {null, false}});
        tryOperator("intBoxed is context.sb.id", new Object[][] {{10, true}, {9, false}, {null, false}});

        tryOperator("context.sb.id is not intBoxed", new Object[][] {{10, false}, {9, true}, {null, true}});
        tryOperator("intBoxed is not context.sb.id", new Object[][] {{10, false}, {9, true}, {null, true}});

        // try coercion
        tryOperator("context.sb.id = shortBoxed", new Object[][] {{(short)10, true}, {(short)9, false}, {null, false}});
        tryOperator("shortBoxed = context.sb.id", new Object[][] {{(short)10, true}, {(short)9, false}, {null, false}});

        tryOperator("context.sb.id > shortBoxed", new Object[][] {{(short)11, false}, {(short)10, false}, {(short)9, true}, {(short)8, true}});
        tryOperator("shortBoxed < context.sb.id", new Object[][] {{(short)11, false}, {(short)10, false}, {(short)9, true}, {(short)8, true}});

        tryOperator("shortBoxed in (context.sb.id)", new Object[][] {{(short)11, false}, {(short)10, true}, {(short)9, false}, {(short)8, false}});
    }

    private void tryOperator(String operator, Object[][] testdata) {
        FilterServiceSPI filterSpi = (FilterServiceSPI) spi.getFilterService();

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select theString as c0,intPrimitive as c1,context.sb.p00 as c2 " +
                "from SupportBean(" + operator + ")");
        stmt.addListener(listener);

        // initiate
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S01"));

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean();
            Object testValue = testdata[i][0];
            if (testValue instanceof Integer) {
                bean.setIntBoxed((Integer) testValue);
            }
            else {
                bean.setShortBoxed((Short) testValue);
            }
            boolean expected = (Boolean) testdata[i][1];

            epService.getEPRuntime().sendEvent(bean);
            assertEquals("Failed at " + i, expected, listener.getAndClearIsInvoked());
        }

        // assert type of expression
        FilterSet set = filterSpi.take(Collections.singleton(stmt.getStatementId()));
        assertEquals(1, set.getFilters().size());
        FilterValueSet valueSet = set.getFilters().get(0).getFilterValueSet();
        assertEquals(1, valueSet.getParameters().length);
        FilterValueSetParam para = valueSet.getParameters()[0][0];
        assertTrue(para.getFilterOperator() != FilterOperator.BOOLEAN_EXPRESSION);

        stmt.destroy();
    }

    public void testFilterBooleanOperator() {
        epService.getEPAdministrator().createEPL("create context EverySupportBean as " +
                "initiated by SupportBean_S0 as sb " +
                "terminated after 10 days 5 hours 2 minutes 1 sec 11 milliseconds");

        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EverySupportBean " +
                "select theString as c0,intPrimitive as c1,context.sb.p00 as c2 " +
                "from SupportBean(intPrimitive + context.sb.id = 5)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S01"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2, "S01"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S02"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3", 2, "S01"}, {"E3", 2, "S02"}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "S03"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 2, "S01"}, {"E4", 2, "S02"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E5", 1, "S03"}});
    }

    public void testTerminateTwoContextSameTime() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);

        sendTimeEvent("2002-05-1T8:00:00.000");
        String eplContext = "@Name('CTX') create context CtxInitiated " +
                "initiated by SupportBean_S0 as sb0 " +
                "terminated after 1 minute";
        epService.getEPAdministrator().createEPL(eplContext);

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

        sendTimeEvent("2002-05-1T8:01:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean("G6", 6));
        assertFalse(listener.getAndClearIsInvoked());

        // clean up
        epService.getEPAdministrator().getStatement("S1").destroy();
        epService.getEPAdministrator().getStatement("CTX").destroy();
    }

    public void testOutputSnapshotWhenTerminated() {
        sendTimeEvent("2002-05-1T8:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and snapshot
        String[] fields = "c1".split(",");
        String epl = "context EveryMinute select sum(intPrimitive) as c1 from SupportBean output snapshot when terminated";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendTimeEvent("2002-05-1T8:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendTimeEvent("2002-05-1T8:01:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendTimeEvent("2002-05-1T8:01:59.999");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate
        sendTimeEvent("2002-05-1T8:02:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1 + 2 + 3});

        sendTimeEvent("2002-05-1T8:02:01.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        assertFalse(listener.getAndClearIsInvoked());

        assertSODA(epl);

        // terminate
        sendTimeEvent("2002-05-1T8:03:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4 + 5 + 6});

        stmt.destroy();

        // test late-coming statement without "terminated"
        EPStatementSPI stmtTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EveryMinute " +
                "select context.id as c0, sum(intPrimitive) as c1 from SupportBean output snapshot every 2 events");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E10", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E11", 2));
        assertFalse(listener.isInvoked());

        sendTimeEvent("2002-05-1T8:04:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E12", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E13", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{7});

        // terminate
        sendTimeEvent("2002-05-1T8:05:00.000");
        assertFalse(listener.isInvoked());
    }

    public void testOutputAllEvery2AndTerminated() {
        sendTimeEvent("2002-05-1T8:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and every 2 events output all with group by
        String[] fields = "c1,c2".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("context EveryMinute " +
                "select theString as c1, sum(intPrimitive) as c2 from SupportBean group by theString output all every 2 events and when terminated order by theString asc");
        stmt.addListener(listener);

        sendTimeEvent("2002-05-1T8:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendTimeEvent("2002-05-1T8:01:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 1 + 2}});

        sendTimeEvent("2002-05-1T8:01:59.999");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate
        sendTimeEvent("2002-05-1T8:02:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 1 + 2}, {"E2", 3}});

        sendTimeEvent("2002-05-1T8:02:01.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 4}, {"E5", 5}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 14}, {"E5", 5}, {"E6", 6}});

        // terminate
        sendTimeEvent("2002-05-1T8:03:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4", 14}, {"E5", 5}, {"E6", 6}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", -2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", -1}, {"E6", -2}});
    }

    public void testOutputWhenExprWhenTerminatedCondition() {
        sendTimeEvent("2002-05-1T8:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and every 2 events output all with group by
        String[] fields = "c0".split(",");
        String epl = "context EveryMinute " +
                "select theString as c0 from SupportBean output when count_insert>1 and when terminated and count_insert>0";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendTimeEvent("2002-05-1T8:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}});

        sendTimeEvent("2002-05-1T8:01:59.999");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate, new context partition
        sendTimeEvent("2002-05-1T8:02:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3"}});

        sendTimeEvent("2002-05-1T8:02:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4"}, {"E5"}});

        sendTimeEvent("2002-05-1T8:03:00.000");
        assertFalse(listener.isInvoked());

        assertSODA(epl);
    }

    public void testOutputOnlyWhenTerminatedCondition() {

        sendTimeEvent("2002-05-1T8:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 1 min");

        // test when-terminated and every 2 events output all with group by
        String[] fields = "c0".split(",");
        String epl = "context EveryMinute " +
                "select theString as c0 from SupportBean output when terminated and count_insert > 0";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendTimeEvent("2002-05-1T8:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        // terminate, new context partition
        sendTimeEvent("2002-05-1T8:02:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}});

        // terminate, new context partition
        sendTimeEvent("2002-05-1T8:03:00.000");
        assertFalse(listener.isInvoked());
    }

    public void testOutputOnlyWhenSetAndWhenTerminatedSet() {

        sendTimeEvent("2002-05-1T8:00:00.000");
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
        stmtOne.addListener(listener);

        sendTimeEvent("2002-05-1T8:01:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertEquals(1, epService.getEPRuntime().getVariableValue("myvar"));
        assertTrue(listener.getAndClearIsInvoked());

        sendTimeEvent("2002-05-1T8:02:00.000"); // terminate, new context partition
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(2, epService.getEPRuntime().getVariableValue("myvar"));

        assertSODA(eplOne);
    }

    public void testOutputOnlyWhenTerminatedThenSet() {

        String[] fields = "c0".split(",");
        sendTimeEvent("2002-05-1T8:00:00.000");
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
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        // terminate, new context partition
        sendTimeEvent("2002-05-1T8:01:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E4"}});
        assertEquals(10, epService.getEPRuntime().getVariableValue("myvar"));

        assertSODA(eplTwo);
    }

    public void testCrontab() {
        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        sendTimeEvent("2002-05-1T8:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryMinute as " +
                "initiated by pattern[every timer:at(*, *, *, *, *)] " +
                "terminated after 3 min");

        String[] fields = "c1,c2".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("@IterableUnbound context EveryMinute select theString as c1, sum(intPrimitive) as c2 from SupportBean");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.getAndClearIsInvoked());
        assertEquals(0, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, null);

        sendTimeEvent("2002-05-1T8:01:00.000");

        assertEquals(1, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        Object[][] expected = new Object[][]{{"E2", 5}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent("2002-05-1T8:01:59.999");

        assertEquals(1, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 6));
        expected = new Object[][]{{"E3", 11}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent("2002-05-1T8:02:00.000");

        assertEquals(2, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2);
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 7));
        expected = new Object[][]{{"E4", 18}, {"E4", 7}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent("2002-05-1T8:02:59.999");

        assertEquals(2, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 2);
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 8));
        expected = new Object[][]{{"E5", 26}, {"E5", 15}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent("2002-05-1T8:03:00.000");

        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 3);
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 9));
        expected = new Object[][]{{"E6", 35}, {"E6", 24}, {"E6", 9}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent("2002-05-1T8:04:00.000");

        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 3);
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 10));
        expected = new Object[][]{{"E7", 34}, {"E7", 19}, {"E7", 10}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        sendTimeEvent("2002-05-1T8:05:00.000");

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
        tryInvalid("create context CtxMonthly4 start (0) end(*,*,*,*,*)",
                "Error starting statement: Invalid schedule specification: Invalid number of crontab parameters, expecting between 5 and 7 parameters, received 1 [create context CtxMonthly4 start (0) end(*,*,*,*,*)]");
        tryInvalid("create context CtxMonthly4 start (*,*,*,*,*) end(*,*,*,*,*,*,*,*)",
                "Error starting statement: Invalid schedule specification: Invalid number of crontab parameters, expecting between 5 and 7 parameters, received 8 [create context CtxMonthly4 start (*,*,*,*,*) end(*,*,*,*,*,*,*,*)]");

        // test invalid -after
        tryInvalid("create context CtxMonthly4 start after 1 second end after -1 seconds",
                "Error starting statement: Invalid negative time period expression '-1 seconds' [create context CtxMonthly4 start after 1 second end after -1 seconds]");
        tryInvalid("create context CtxMonthly4 start after -1 second end after 1 seconds",
                "Error starting statement: Invalid negative time period expression '-1 seconds' [create context CtxMonthly4 start after -1 second end after 1 seconds]");
    }

    public void testStartNowCalMonthScoped() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);
        sendCurrentTime("2002-02-01T9:00:00.000");
        epService.getEPAdministrator().createEPL("create context MyCtx start SupportBean_S1 end after 1 month");
        epService.getEPAdministrator().createEPL("context MyCtx select * from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listener.getAndClearIsInvoked());

        sendCurrentTimeWithMinus("2002-03-01T9:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertTrue(listener.getAndClearIsInvoked());

        sendCurrentTime("2002-03-01T9:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());
    }

    private void sendTimeEvent(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private void assertSODA(String epl) {
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtModel = epService.getEPAdministrator().create(model);
        assertEquals(epl, stmtModel.getText());
        stmtModel.destroy();
    }

    private void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void sendCurrentTime(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
