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
import com.espertech.esper.client.context.ContextPartitionCollection;
import com.espertech.esper.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPContextPartitionAdminSPI;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestContextInitatedTerminatedWithDistinct extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testInvalid() {
        // require stream name assignment using 'as'
        tryInvalid("create context MyContext initiated by distinct(theString) SupportBean terminated after 15 seconds",
                "Error starting statement: Distinct-expressions require that a stream name is assigned to the stream using 'as' [create context MyContext initiated by distinct(theString) SupportBean terminated after 15 seconds]");

        // require stream
        tryInvalid("create context MyContext initiated by distinct(a.theString) pattern [a=SupportBean] terminated after 15 seconds",
                "Error starting statement: Distinct-expressions require a stream as the initiated-by condition [create context MyContext initiated by distinct(a.theString) pattern [a=SupportBean] terminated after 15 seconds]");

        // invalid distinct-clause expression
        tryInvalid("create context MyContext initiated by distinct((select * from MyWindow)) SupportBean as sb terminated after 15 seconds",
                   "Error starting statement: Invalid context distinct-clause expression 'subselect_0': Aggregation, sub-select, previous or prior functions are not supported in this context [create context MyContext initiated by distinct((select * from MyWindow)) SupportBean as sb terminated after 15 seconds]");

        // empty list of expressions
        tryInvalid("create context MyContext initiated by distinct() SupportBean terminated after 15 seconds",
                "Error starting statement: Distinct-expressions have not been provided [create context MyContext initiated by distinct() SupportBean terminated after 15 seconds]");

        // non-overlapping context not allowed with distinct
        tryInvalid("create context MyContext start distinct(theString) SupportBean end after 15 seconds",
                "Incorrect syntax near 'distinct' (a reserved keyword) at line 1 column 31 [create context MyContext start distinct(theString) SupportBean end after 15 seconds]");
    }

    public void testDistinctOverlappingSingleKey() {
        epService.getEPAdministrator().createEPL(
                "create context MyContext " +
                "  initiated by distinct(s0.theString) SupportBean(intPrimitive = 0) s0" +
                "  terminated by SupportBean(theString = s0.theString and intPrimitive = 1)");

        String[] fields = "theString,longPrimitive,cnt".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "context MyContext " +
                "select theString, longPrimitive, count(*) as cnt from SupportBean(theString = context.s0.theString)");
        stmt.addListener(listener);

        sendEvent(epService, "A", -1, 10);
        sendEvent(epService, "A", 1, 11);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "A", 0, 12);   // allocate context
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 12L, 1L});

        sendEvent(epService, "A", 0, 13);   // counts towards the existing context, not having a new one
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 13L, 2L});

        sendEvent(epService, "A", -1, 14);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 14L, 3L});

        sendEvent(epService, "A", 1, 15);   // context termination
        sendEvent(epService, "A", -1, 16);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "A", 0, 17);   // allocate context
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 17L, 1L});

        sendEvent(epService, "A", -1, 18);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 18L, 2L});

        sendEvent(epService, "B", 0, 19);   // allocate context
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 19L, 1L});

        sendEvent(epService, "B", -1, 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 20L, 2L});

        sendEvent(epService, "A", 1, 21);   // context termination
        sendEvent(epService, "B", 1, 22);   // context termination
        sendEvent(epService, "A", -1, 23);
        sendEvent(epService, "B", -1, 24);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "A", 0, 25);   // allocate context
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"A", 25L, 1L});

        sendEvent(epService, "B", 0, 26);   // allocate context
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"B", 26L, 1L});
    }

    public void testDistinctOverlappingMultiKey() {
        String epl = "create context MyContext as " +
                "initiated by distinct(theString, intPrimitive) SupportBean as sb " +
                "terminated SupportBean_S1";         // any S1 ends the contexts
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtContext = epService.getEPAdministrator().create(model);
        assertEquals(stmtContext.getText(), model.toEPL());

        String[] fields = "id,p00,p01,cnt".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "context MyContext " +
                "select id, p00, p01, count(*) as cnt " +
                        "from SupportBean_S0(id = context.sb.intPrimitive and p00 = context.sb.theString)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A", "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1, "A", "E1", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1, "A", "E2", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1)); // terminate all
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A", "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A", "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1, "A", "E4", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {2, "B", "E5", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "B", "E6"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1, "B", "E6", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E7"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {2, "B", "E7", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1)); // terminate all
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E8"));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E9"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E9", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E10"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E10", 2L});

        // destroy context partition, should forget about the distinct key
        getSpi(epService).destroyContextPartitions("MyContext", new ContextPartitionSelectorAll());
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E11"));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "B", "E12"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E12", 1L});
    }

    private static void sendEvent(EPServiceProvider engine, String theString, int intPrimitive, long longPrimitive) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        engine.getEPRuntime().sendEvent(event);
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

    private static EPContextPartitionAdminSPI getSpi(EPServiceProvider epService) {
        return ((EPContextPartitionAdminSPI) epService.getEPAdministrator().getContextPartitionAdmin());
    }
}
