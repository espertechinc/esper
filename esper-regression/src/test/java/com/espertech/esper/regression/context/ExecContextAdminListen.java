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
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.context.SupportContextListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.ContextStateCacheHook;

import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.supportregression.context.SupportContextListenUtil.*;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecContextAdminListen implements RegressionExecution, ContextStateCacheHook {

    public void configure(Configuration configuration) throws Exception {
        for (Class clazz : new Class[] {SupportBean_S0.class, SupportBean_S1.class, SupportBean.class}) {
            configuration.addEventType(clazz);
        }
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInitTerm(epService);
        runAssertionHash(epService);
        runAssertionCategory(epService);
        runAssertionNested(epService);
        runAssertionContextAddRemoveListener(epService);
        runAssertionPartitionAddRemoveListener(epService);
        runAssertionMultipleStatements(epService);
    }

    private void runAssertionMultipleStatements(EPServiceProvider epService) {
        String name = "MyContextStartS0EndS1";
        String contextEPL = "create context MyContextStartS0EndS1 start SupportBean_S0 as s0 end SupportBean_S1";
        epService.getEPAdministrator().createEPL(contextEPL);

        SupportContextListener listener = new SupportContextListener(epService);
        epService.getEPAdministrator().getContextPartitionAdmin().addContextPartitionStateListener("MyContextStartS0EndS1", listener);
        epService.getEPAdministrator().createEPL("@name('a') context MyContextStartS0EndS1 select count(*) from SupportBean");
        epService.getEPAdministrator().createEPL("@name('b') context MyContextStartS0EndS1 select count(*) from SupportBean_S0");
        listener.assertAndReset(eventContextWStmt(name, ContextStateEventContextStatementAdded.class, "a"), eventContext(name, ContextStateEventContextActivated.class), eventContextWStmt(name, ContextStateEventContextStatementAdded.class, "b"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        listener.assertAndReset(eventPartitionInitTerm(name, ContextStateEventContextPartitionAllocated.class));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPartitionAddRemoveListener(EPServiceProvider epService) {
        String epl = "create context MyContextStartEnd start SupportBean_S0 as s0 end SupportBean_S1";
        runAssertionPartitionAddRemoveListener(epService, epl, "MyContextStartEnd");

        epl = "create context MyContextStartEndWithNeverEnding " +
                "context NeverEndingStory start @now, " +
                "context ABSession start SupportBean_S0 as s0 end SupportBean_S1";
        runAssertionPartitionAddRemoveListener(epService, epl, "MyContextStartEndWithNeverEnding");
    }

    private void runAssertionPartitionAddRemoveListener(EPServiceProvider epService, String eplContext, String contextName) {
        EPStatement context = epService.getEPAdministrator().createEPL(eplContext);
        EPStatement stmt = epService.getEPAdministrator().createEPL("context " + contextName + " select count(*) from SupportBean");
        EPContextPartitionAdmin api = epService.getEPAdministrator().getContextPartitionAdmin();

        SupportContextListener[] listeners = new SupportContextListener[3];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new SupportContextListener(epService);
            epService.getEPAdministrator().getContextPartitionAdmin().addContextPartitionStateListener(contextName, listeners[i]);
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].assertAndReset(eventPartitionInitTerm(contextName, ContextStateEventContextPartitionAllocated.class));
        }

        api.removeContextPartitionStateListener(contextName, listeners[0]);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        listeners[0].assertNotInvoked();
        listeners[1].assertAndReset(eventPartitionInitTerm(contextName, ContextStateEventContextPartitionDeallocated.class));
        listeners[2].assertAndReset(eventPartitionInitTerm(contextName, ContextStateEventContextPartitionDeallocated.class));

        Iterator<ContextPartitionStateListener> it = api.getContextPartitionStateListeners(contextName);
        assertSame(listeners[1], it.next());
        assertSame(listeners[2], it.next());
        assertFalse(it.hasNext());

        api.removeContextPartitionStateListeners(contextName);
        assertFalse(api.getContextPartitionStateListeners(contextName).hasNext());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].assertNotInvoked();
        }

        stmt.destroy();
        context.destroy();
    }

    private void runAssertionContextAddRemoveListener(EPServiceProvider epService) {
        EPContextPartitionAdmin api = epService.getEPAdministrator().getContextPartitionAdmin();

        String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S1";
        SupportContextListener[] listeners = new SupportContextListener[3];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new SupportContextListener(epService);
            epService.getEPAdministrator().getContextPartitionAdmin().addContextStateListener(listeners[i]);
        }

        EPStatement context = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].assertAndReset(eventContext("MyContext", ContextStateEventContextCreated.class));
        }

        api.removeContextStateListener(listeners[0]);
        context.destroy();
        listeners[0].assertNotInvoked();
        listeners[1].assertAndReset(eventContext("MyContext", ContextStateEventContextDestroyed.class));
        listeners[2].assertAndReset(eventContext("MyContext", ContextStateEventContextDestroyed.class));

        Iterator<ContextStateListener> it = api.getContextStateListeners();
        assertSame(listeners[1], it.next());
        assertSame(listeners[2], it.next());
        assertFalse(it.hasNext());

        api.removeContextStateListeners();
        assertFalse(api.getContextStateListeners().hasNext());

        context = epService.getEPAdministrator().createEPL(epl);
        context.destroy();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].assertNotInvoked();
        }
    }

    private void runAssertionNested(EPServiceProvider epService) {
        SupportContextListener listener = new SupportContextListener(epService);
        epService.getEPAdministrator().getContextPartitionAdmin().addContextStateListener(listener);

        EPStatement context = epService.getEPAdministrator().createEPL("create context MyContext " +
                "context ContextPosNeg group by intPrimitive > 0 as pos, group by intPrimitive < 0 as neg from SupportBean, " +
                "context ByString partition by theString from SupportBean");
        listener.assertAndReset(eventContext("MyContext", ContextStateEventContextCreated.class));

        EPStatement stmt = epService.getEPAdministrator().createEPL("@name('stmt-0') context MyContext select count(*) from SupportBean");
        listener.assertAndReset(eventContextWStmt("MyContext", ContextStateEventContextStatementAdded.class, "stmt-0"), eventContext("MyContext", ContextStateEventContextActivated.class));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        List<ContextStateEventContextPartitionAllocated> allocated = listener.getAllocatedEvents();
        assertEquals(1, allocated.size());
        EPAssertionUtil.assertEqualsExactOrder("E1".split(","), ((ContextPartitionIdentifierPartitioned) allocated.get(0).getIdentifier()).getKeys());
        assertEquals(1, listener.getAndReset().size());

        stmt.destroy();
        listener.assertAndReset(eventPartitionInitTerm("MyContext", ContextStateEventContextPartitionDeallocated.class), eventContext("MyContext", ContextStateEventContextDeactivated.class), eventContextWStmt("MyContext", ContextStateEventContextStatementRemoved.class, "stmt-0"));

        context.destroy();
        listener.assertAndReset(eventContext("MyContext", ContextStateEventContextDestroyed.class));

        epService.getEPAdministrator().getContextPartitionAdmin().removeContextStateListeners();
    }

    private void runAssertionCategory(EPServiceProvider epService) {
        SupportContextListener listener = new SupportContextListener(epService);
        epService.getEPAdministrator().getContextPartitionAdmin().addContextStateListener(listener);

        EPStatement context = epService.getEPAdministrator().createEPL("create context MyContext group by intPrimitive > 0 as pos, group by intPrimitive < 0 as neg from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@name('stmt-0') context MyContext select count(*) from SupportBean");

        List<ContextStateEventContextPartitionAllocated> allocated = listener.getAllocatedEvents();
        assertEquals(2, allocated.size());
        assertEquals("neg", ((ContextPartitionIdentifierCategory) allocated.get(1).getIdentifier()).getLabel());
        listener.getAndReset();

        stmt.destroy();
        context.destroy();
        epService.getEPAdministrator().getContextPartitionAdmin().removeContextStateListeners();
    }

    private void runAssertionHash(EPServiceProvider epService) {
        SupportContextListener listener = new SupportContextListener(epService);
        epService.getEPAdministrator().getContextPartitionAdmin().addContextStateListener(listener);

        EPStatement context = epService.getEPAdministrator().createEPL("create context MyContext coalesce by consistent_hash_crc32(theString) from SupportBean granularity 2 preallocate");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@name('stmt-0') context MyContext select count(*) from SupportBean");

        List<ContextStateEventContextPartitionAllocated> allocated = listener.getAllocatedEvents();
        assertEquals(2, allocated.size());
        assertEquals(1, ((ContextPartitionIdentifierHash) allocated.get(1).getIdentifier()).getHash());
        listener.getAndReset();

        context.destroy();
        listener.assertNotInvoked();

        stmt.destroy();
        listener.assertAndReset(
                eventPartitionInitTerm("MyContext", ContextStateEventContextPartitionDeallocated.class),
                eventPartitionInitTerm("MyContext", ContextStateEventContextPartitionDeallocated.class),
                eventContext("MyContext", ContextStateEventContextDeactivated.class),
                eventContextWStmt("MyContext", ContextStateEventContextStatementRemoved.class, "stmt-0"),
                eventContext("MyContext", ContextStateEventContextDestroyed.class));

        epService.getEPAdministrator().getContextPartitionAdmin().removeContextStateListeners();
    }

    private void runAssertionInitTerm(EPServiceProvider epService) {
        SupportContextListener listener = new SupportContextListener(epService);
        epService.getEPAdministrator().getContextPartitionAdmin().addContextStateListener(listener);

        EPStatement context = epService.getEPAdministrator().createEPL("create context MyContext start SupportBean_S0 as s0 end SupportBean_S1");
        listener.assertAndReset(eventContext("MyContext", ContextStateEventContextCreated.class));

        EPStatement stmt = epService.getEPAdministrator().createEPL("@name('stmt-0') context MyContext select count(*) from SupportBean");
        listener.assertAndReset(eventContextWStmt("MyContext", ContextStateEventContextStatementAdded.class, "stmt-0"), eventContext("MyContext", ContextStateEventContextActivated.class));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        listener.assertAndReset(eventPartitionInitTerm("MyContext", ContextStateEventContextPartitionAllocated.class));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        listener.assertAndReset(eventPartitionInitTerm("MyContext", ContextStateEventContextPartitionDeallocated.class));

        stmt.destroy();
        listener.assertAndReset(eventContext("MyContext", ContextStateEventContextDeactivated.class), eventContextWStmt("MyContext", ContextStateEventContextStatementRemoved.class, "stmt-0"));

        context.destroy();
        listener.assertAndReset(eventContext("MyContext", ContextStateEventContextDestroyed.class));

        epService.getEPAdministrator().getContextPartitionAdmin().removeContextStateListeners();
    }
}
