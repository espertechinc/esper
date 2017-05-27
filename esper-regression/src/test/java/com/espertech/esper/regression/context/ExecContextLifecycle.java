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
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDW;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDWFactory;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextLifecycle implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWFactory.class.getName(), SupportVirtualDW.ITERATE);    // configure with iteration
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSplitStream(epService);
        runAssertionVirtualDataWindow(epService);
        runAssertionNWOtherContextOnExpr(epService);
        runAssertionLifecycle(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionSplitStream(EPServiceProvider epService) throws Exception {
        String eplOne = "create context CtxSegmentedByTarget partition by theString from SupportBean;" +
                "@Name('out') context CtxSegmentedByTarget on SupportBean insert into NewSupportBean select * where intPrimitive = 100;";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplOne);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from NewSupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPAdministrator().destroyAllStatements();
        listener.reset();

        // test with subquery
        String[] fields = "mymax".split(",");
        String eplTwo = "create context CtxSegmentedByTarget partition by theString from SupportBean;" +
                "context CtxSegmentedByTarget create window NewEvent#unique(theString) as SupportBean;" +
                "@Name('out') context CtxSegmentedByTarget on SupportBean " +
                "insert into NewEvent select * where intPrimitive = 100 " +
                "insert into NewEventTwo select (select max(intPrimitive) from NewEvent) as mymax  " +
                "output all;";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplTwo);

        epService.getEPAdministrator().createEPL("select * from NewEventTwo").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionVirtualDataWindow(EPServiceProvider epService) {
        SupportVirtualDWFactory.getWindows().clear();
        SupportVirtualDWFactory.setDestroyed(false);

        epService.getEPAdministrator().createEPL("create context CtxSegmented as partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("context CtxSegmented create window TestVDWWindow.test:vdw() as SupportBean");
        epService.getEPAdministrator().createEPL("select * from TestVDWWindow");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(2, SupportVirtualDWFactory.getWindows().size());   // Independent windows for independent contexts

        epService.getEPAdministrator().destroyAllStatements();
        for (SupportVirtualDW vdw : SupportVirtualDWFactory.getWindows()) {
            assertTrue(vdw.isDestroyed());
        }
        assertTrue(SupportVirtualDWFactory.isDestroyed());
    }

    private void runAssertionNWOtherContextOnExpr(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");
        epService.getEPAdministrator().createEPL("create context TenToFive as start (0, 10, *, *, *) end (0, 17, *, *, *)");

        // Trigger not in context
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL("context NineToFive create window MyWindow#keepall as SupportBean");
        try {
            epService.getEPAdministrator().createEPL("on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Cannot create on-trigger expression: Named window 'MyWindow' was declared with context 'NineToFive', please declare the same context name [on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1]", ex.getMessage());
        }

        // Trigger in different context
        try {
            epService.getEPAdministrator().createEPL("context TenToFive on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Cannot create on-trigger expression: Named window 'MyWindow' was declared with context 'NineToFive', please use the same context instead [context TenToFive on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1]", ex.getMessage());
        }

        // Named window not in context, trigger in different context
        stmtNamedWindow.destroy();
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        try {
            epService.getEPAdministrator().createEPL("context TenToFive on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Cannot create on-trigger expression: Named window 'MyWindow' was declared with context 'null', please use the same context instead [context TenToFive on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1]", ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLifecycle(EPServiceProvider epService) {

        String epl = "@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        ContextManagementService ctxMgmtService = spi.getContextManagementService();
        SchedulingService schedulingService = spi.getSchedulingService();
        assertEquals(0, ctxMgmtService.getContextCount());
        assertEquals(0, schedulingService.getScheduleHandleCount());

        // create and destroy
        EPStatement stmtContext = epService.getEPAdministrator().createEPL(epl);
        assertEquals(1, ctxMgmtService.getContextCount());
        assertEquals(0, schedulingService.getScheduleHandleCount());

        stmtContext.destroy();
        assertEquals(0, ctxMgmtService.getContextCount());

        // create context, create statement, destroy statement, destroy context
        stmtContext = epService.getEPAdministrator().createEPL(epl);
        assertEquals(1, ctxMgmtService.getContextCount());

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('C') context NineToFive select * from SupportBean");
        assertEquals(1, schedulingService.getScheduleHandleCount());

        stmt.destroy();
        assertEquals(0, schedulingService.getScheduleHandleCount());

        stmtContext.destroy();
        assertEquals(0, ctxMgmtService.getContextCount());

        // create same context
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPAdministrator().createEPL("@Name('C') context NineToFive select * from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('D') context NineToFive select * from SupportBean");
        epService.getEPAdministrator().destroyAllStatements();
        assertEquals(0, ctxMgmtService.getContextCount());
        assertEquals(0, schedulingService.getScheduleHandleCount());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        // same context twice
        String eplCreateCtx = "create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
        EPStatement stmtContext = epService.getEPAdministrator().createEPL(eplCreateCtx);
        tryInvalid(epService, eplCreateCtx, "Error starting statement: Context by name 'NineToFive' already exists [");

        // still in use
        epService.getEPAdministrator().createEPL("context NineToFive select * from SupportBean");
        stmtContext.destroy();
        tryInvalid(epService, eplCreateCtx, "Error starting statement: Context by name 'NineToFive' is still referenced by statements and may not be changed");

        // not found
        tryInvalid(epService, "context EightToSix select * from SupportBean", "Error starting statement: Context by name 'EightToSix' has not been declared [");

        // test update: update is not allowed as it is processed out-of-context by runtime
        epService.getEPAdministrator().createEPL("insert into ABCStream select * from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByAString partition by theString from SupportBean");
        try {
            epService.getEPAdministrator().createEPL("context SegmentedByAString update istream ABCStream set intPrimitive = (select id from SupportBean_S0#lastevent) where intPrimitive < 0");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Update IStream is not supported in conjunction with a context [context SegmentedByAString update istream ABCStream set intPrimitive = (select id from SupportBean_S0#lastevent) where intPrimitive < 0]", ex.getMessage());
        }

        // context declaration for create-context
        epService.getEPAdministrator().createEPL("create context ABC start @now end after 5 seconds");
        tryInvalid(epService, "context ABC create context DEF start @now end after 5 seconds",
                "Error starting statement: A create-context statement cannot itself be associated to a context, please declare a nested context instead [context ABC create context DEF start @now end after 5 seconds]");

        epService.getEPAdministrator().destroyAllStatements();
    }
}
