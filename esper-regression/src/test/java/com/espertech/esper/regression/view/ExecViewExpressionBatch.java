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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecViewExpressionBatch implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionNewestEventOldestEvent(epService);
        runAssertionLengthBatch(epService);
        runAssertionTimeBatch(epService);
        runAssertionVariableBatch(epService);
        runAssertionDynamicTimeBatch(epService);
        runAssertionUDFBuiltin(epService);
        runAssertionInvalid(epService);
        runAssertionNamedWindowDelete(epService);
        runAssertionPrev(epService);
        runAssertionEventPropBatch(epService);
        runAssertionAggregation(epService);
    }

    private void runAssertionNewestEventOldestEvent(EPServiceProvider epService) {

        // try with include-trigger-event
        String[] fields = new String[]{"theString"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_event.intPrimitive != oldest_event.intPrimitive, false)");
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmtTwo.destroy();
    }

    private void runAssertionLengthBatch(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(current_count >= 3, true)");
        SupportUpdateListener listener = new SupportUpdateListener();
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

    private void runAssertionTimeBatch(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_timestamp - oldest_timestamp > 2000)");
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionVariableBatch(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable boolean POST = false");

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(POST)");
        SupportUpdateListener listener = new SupportUpdateListener();
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

    private void runAssertionDynamicTimeBatch(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable long SIZE = 1000");

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr_batch(newest_timestamp - oldest_timestamp > SIZE)");
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionUDFBuiltin(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("udf", ExecViewExpressionWindow.LocalUDF.class.getName(), "evaluateExpiryUDF");
        epService.getEPAdministrator().createEPL("select * from SupportBean#expr_batch(udf(theString, view_reference, expired_count))");

        ExecViewExpressionWindow.LocalUDF.setResult(true);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertEquals("E1", ExecViewExpressionWindow.LocalUDF.getKey());
        assertEquals(0, (int) ExecViewExpressionWindow.LocalUDF.getExpiryCount());
        assertNotNull(ExecViewExpressionWindow.LocalUDF.getViewref());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        ExecViewExpressionWindow.LocalUDF.setResult(false);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertEquals("E3", ExecViewExpressionWindow.LocalUDF.getKey());
        assertEquals(0, (int) ExecViewExpressionWindow.LocalUDF.getExpiryCount());
        assertNotNull(ExecViewExpressionWindow.LocalUDF.getViewref());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from SupportBean#expr_batch(1)",
                "Error starting statement: Error attaching view to event stream: Invalid return value for expiry expression, expected a boolean return value but received int [select * from SupportBean#expr_batch(1)]");

        tryInvalid(epService, "select * from SupportBean#expr_batch((select * from SupportBean#lastevent))",
                "Error starting statement: Error attaching view to event stream: Invalid expiry expression: Sub-select, previous or prior functions are not supported in this context [select * from SupportBean#expr_batch((select * from SupportBean#lastevent))]");

        tryInvalid(epService, "select * from SupportBean#expr_batch(null < 0)",
                "Error starting statement: Error attaching view to event stream: Invalid parameter expression 0 for Expression-batch view: Failed to validate view parameter expression 'null<0': Implicit conversion from datatype 'null' to numeric is not allowed");
    }

    private void runAssertionNamedWindowDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window NW#expr_batch(current_count > 3) as SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
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

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPrev(EPServiceProvider epService) {
        String[] fields = new String[]{"val0"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select prev(1, theString) as val0 from SupportBean#expr_batch(current_count > 2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{null}, {"E1"}, {"E2"}}, null);

        stmt.destroy();
    }

    private void runAssertionEventPropBatch(EPServiceProvider epService) {
        String[] fields = new String[]{"val0"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString as val0 from SupportBean#expr_batch(intPrimitive > 0)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}, {"E4"}}, new Object[][]{{"E2"}});

        stmt.destroy();
    }

    private void runAssertionAggregation(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};

        // Test un-grouped
        EPStatement stmtUngrouped = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#expr_batch(sum(intPrimitive) > 100)");
        SupportUpdateListener listener = new SupportUpdateListener();
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

        sendEvent(epService, "E1", 1, 10);
        sendEvent(epService, "E2", 2, 10);
        sendEvent(epService, "E3", 1, 90);
        sendEvent(epService, "E4", 2, 80);
        sendEvent(epService, "E5", 2, 10);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E6", 2, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}, {"E4"}, {"E5"}, {"E6"}}, null);

        sendEvent(epService, "E7", 2, 50);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E8", 1, 2);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}, {"E3"}, {"E8"}}, null);

        sendEvent(epService, "E9", 2, 50);
        sendEvent(epService, "E10", 1, 101);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E10"}}, new Object[][]{{"E1"}, {"E3"}, {"E8"}});

        sendEvent(epService, "E11", 2, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E7"}, {"E9"}, {"E11"}}, new Object[][]{{"E2"}, {"E4"}, {"E5"}, {"E6"}});

        sendEvent(epService, "E12", 1, 102);
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

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
