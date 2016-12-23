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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewExpressionBatch extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNewestEventOldestEvent() {

        // try with include-trigger-event
        String[] fields = new String[] {"theString"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_event.intPrimitive != oldest_event.intPrimitive, false)");
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1"}, {"E2"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}}, new Object[][]{{"E3"}});
        stmtOne.destroy();

        // try with include-trigger-event
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_event.intPrimitive != oldest_event.intPrimitive, true)");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E1"}, {"E2"}, {"E3"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
    }

    public void testLengthBatch()
    {
        String[] fields = new String[] {"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(current_count >= 3, true)");
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastOldData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 7));
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 9));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E7"}, {"E8"}, {"E9"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastOldData(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}});

        stmt.destroy();
    }

    public void testTimeBatch()
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = new String[] {"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_timestamp - oldest_timestamp > 2000)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1500));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3000));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3100));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5100));
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 7));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5101));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastOldData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});
    }

    public void testVariableBatch()
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable boolean POST = false");

        String[] fields = new String[] {"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(POST)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("POST", true);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1001));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, new Object[][]{{"E2"}});

        epService.getEPRuntime().setVariableValue("POST", false);
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("POST", true);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2001));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}, {"E5"}}, new Object[][]{{"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E6"}}, new Object[][]{{"E4"}, {"E5"}});

        stmt.stop();
    }

    public void testDynamicTimeBatch()
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable long SIZE = 1000");

        String[] fields = new String[] {"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_timestamp - oldest_timestamp > SIZE)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1900));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertFalse(listener.isInvoked());
        
        epService.getEPRuntime().setVariableValue("SIZE", 500);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1901));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}, {"E2"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2300));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2500));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}}, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3100));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("SIZE", 999);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3700));
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(4100));
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}}, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
    }

    public void testUDFBuiltin()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // not instrumented

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("udf", TestViewExpressionWindow.LocalUDF.class.getName(), "evaluateExpiryUDF");
        epService.getEPAdministrator().createEPL("select * from SupportBean#expr_batch(udf(theString, view_reference, expired_count))");

        TestViewExpressionWindow.LocalUDF.setResult(true);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertEquals("E1", TestViewExpressionWindow.LocalUDF.getKey());
        assertEquals(0, (int) TestViewExpressionWindow.LocalUDF.getExpiryCount());
        assertNotNull(TestViewExpressionWindow.LocalUDF.getViewref());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        TestViewExpressionWindow.LocalUDF.setResult(false);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertEquals("E3", TestViewExpressionWindow.LocalUDF.getKey());
        assertEquals(0, (int) TestViewExpressionWindow.LocalUDF.getExpiryCount());
        assertNotNull(TestViewExpressionWindow.LocalUDF.getViewref());
    }

    public void testInvalid() {
        tryInvalid("select * from SupportBean#expr_batch(1)",
                   "Error starting statement: Error attaching view to event stream: Invalid return value for expiry expression, expected a boolean return value but received Integer [select * from SupportBean#expr_batch(1)]");

        tryInvalid("select * from SupportBean#expr_batch((select * from SupportBean#lastevent))",
                   "Error starting statement: Error attaching view to event stream: Invalid expiry expression: Sub-select, previous or prior functions are not supported in this context [select * from SupportBean#expr_batch((select * from SupportBean#lastevent))]");
    }

    public void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    public void testNamedWindowDelete() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        
        String[] fields = new String[] {"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window NW#expr_batch(current_count > 3) as SupportBean");
        stmt.addListener(listener);

        epService.getEPAdministrator().createEPL("insert into NW select * from SupportBean");
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPAdministrator().createEPL("on SupportBean_A delete from NW where theString = id");
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}, {"E3"}, {"E4"}, {"E5"}}, null);
    }

    public void testPrev() {
        String[] fields = new String[] {"val0"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select prev(1, theString) as val0 from SupportBean#expr_batch(current_count > 2)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listener.isInvoked());
        
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{null}, {"E1"}, {"E2"}}, null);
    }

    public void testEventPropBatch() {
        String[] fields = new String[] {"val0"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString as val0 from SupportBean#expr_batch(intPrimitive > 0)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}, {"E4"}}, new Object[][]{{"E2"}});
    }

    public void testAggregation() {
        String[] fields = new String[] {"theString"};

        // Test un-grouped
        EPStatement stmtUngrouped = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#expr_batch(sum(intPrimitive) > 100)");
        stmtUngrouped.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 90));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 101));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 99));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E5"}, {"E6"}, {"E7"}}, new Object[][]{{"E4"}});
        stmtUngrouped.destroy();

        // Test grouped
        EPStatement stmtGrouped = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#groupwin(intPrimitive)#expr_batch(sum(longPrimitive) > 100)");
        stmtGrouped.addListener(listener);

        sendEvent("E1", 1, 10);
        sendEvent("E2", 2, 10);
        sendEvent("E3", 1, 90);
        sendEvent("E4", 2, 80);
        sendEvent("E5", 2, 10);
        assertFalse(listener.isInvoked());

        sendEvent("E6", 2, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}, {"E4"}, {"E5"}, {"E6"}}, null);

        sendEvent("E7", 2, 50);
        assertFalse(listener.isInvoked());

        sendEvent("E8", 1, 2);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}, {"E3"}, {"E8"}}, null);

        sendEvent("E9", 2, 50);
        sendEvent("E10", 1, 101);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E10"}}, new Object[][]{{"E1"}, {"E3"}, {"E8"}});

        sendEvent("E11", 2, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E7"}, {"E9"}, {"E11"}}, new Object[][]{{"E2"}, {"E4"}, {"E5"}, {"E6"}});

        sendEvent("E12", 1, 102);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E12"}}, new Object[][]{{"E10"}});
        stmtGrouped.destroy();

        // Test on-delete
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window NW#expr_batch(sum(intPrimitive) >= 10) as SupportBean");
        stmt.addListener(listener);
        epService.getEPAdministrator().createEPL("insert into NW select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 8));
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from NW where theString = id");
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}, {"E3"}, {"E4"}}, null);
    }

    private void sendEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
