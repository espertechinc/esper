package com.espertech.esper.regression.enummethod;/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestExpressionDefAliasFor extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testDocSamples() {
        epService.getEPAdministrator().createEPL("create schema SampleEvent()");
        epService.getEPAdministrator().createEPL("expression twoPI alias for {Math.PI * 2}\n" +
                "select twoPI from SampleEvent");

        epService.getEPAdministrator().createEPL("create schema EnterRoomEvent()");
        epService.getEPAdministrator().createEPL("expression countPeople alias for {count(*)} \n" +
                "select countPeople from EnterRoomEvent.win:time(10 seconds) having countPeople > 10");
    }

    public void testNestedAlias() {
        String[] fields = "c0".split(",");
        epService.getEPAdministrator().createEPL("create expression F1 alias for {10}");
        epService.getEPAdministrator().createEPL("create expression F2 alias for {20}");
        epService.getEPAdministrator().createEPL("create expression F3 alias for {F1+F2}");
        epService.getEPAdministrator().createEPL("select F3 as c0 from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {30});
    }

    public void testAliasAggregation() {
        String epl = "@Audit expression total alias for {sum(intPrimitive)} " +
                "select total, total+1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        String[] fields = "total,total+1".split(",");
        for (String field : fields) {
            assertEquals(Integer.class, stmt.getEventType().getPropertyType(field));
        }

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 11});
    }

    public void testGlobalAliasAndSODA() {
        String eplDeclare = "create expression myaliastwo alias for {2}";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplDeclare);
        assertEquals(eplDeclare, model.toEPL());
        EPStatement stmtDeclare = epService.getEPAdministrator().create(model);
        assertEquals(eplDeclare, stmtDeclare.getText());

        epService.getEPAdministrator().createEPL("create expression myalias alias for {1}");
        epService.getEPAdministrator().createEPL("select myaliastwo from SupportBean(intPrimitive = myalias)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(2, listener.assertOneGetNewAndReset().get("myaliastwo"));
    }

    public void testInvalid() {
        tryInvalid("expression total alias for {sum(xxx)} select total+1 from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'total+1': Error validating expression alias 'total': Failed to validate alias expression body expression 'sum(xxx)': Property named 'xxx' is not valid in any stream [expression total alias for {sum(xxx)} select total+1 from SupportBean]");
        tryInvalid("expression total xxx for {1} select total+1 from SupportBean",
                "For expression alias 'total' expecting 'alias' keyword but received 'xxx' [expression total xxx for {1} select total+1 from SupportBean]");
        tryInvalid("expression total(a) alias for {1} select total+1 from SupportBean",
                "For expression alias 'total' expecting no parameters but received 'a' [expression total(a) alias for {1} select total+1 from SupportBean]");
        tryInvalid("expression total alias for {a -> 1} select total+1 from SupportBean",
                "For expression alias 'total' expecting an expression without parameters but received 'a ->' [expression total alias for {a -> 1} select total+1 from SupportBean]");
        tryInvalid("expression total alias for ['some text'] select total+1 from SupportBean",
                "For expression alias 'total' expecting an expression but received a script [expression total alias for ['some text'] select total+1 from SupportBean]");
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
}
