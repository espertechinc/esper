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

public class ExecViewExpressionWindow implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionNewestEventOldestEvent(epService);
        runAssertionLengthWindow(epService);
        runAssertionTimeWindow(epService);
        runAssertionVariable(epService);
        runAssertionDynamicTimeWindow(epService);
        runAssertionUDFBuiltin(epService);
        runAssertionInvalid(epService);
        runAssertionNamedWindowDelete(epService);
        runAssertionPrev(epService);
        runAssertionAggregation(epService);
    }

    private void runAssertionNewestEventOldestEvent(EPServiceProvider epService) {

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr(newest_event.intPrimitive = oldest_event.intPrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E4"}}, new Object[][]{{"E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}, {"E5"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E7"}}, new Object[][]{{"E4"}, {"E5"}, {"E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E7"}});

        stmt.destroy();
    }

    private void runAssertionLengthWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#expr(current_count <= 2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

        stmt.destroy();
    }

    private void runAssertionTimeWindow(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr(oldest_timestamp > newest_timestamp - 2000)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1500));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2500));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3000));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}, {"E5"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E5"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E1"}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3499));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}, {"E5"}, {"E6"}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3500));
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 7));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E7"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E2"}, {"E3"}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 8));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E8"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E8"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionVariable(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable boolean KEEP = true");

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr(KEEP)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        epService.getEPRuntime().setVariableValue("KEEP", false);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        listener.reset();
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1001));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E2"});
        listener.reset();
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().setVariableValue("KEEP", true);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3"}});

        stmt.stop();
    }

    private void runAssertionDynamicTimeWindow(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create variable long SIZE = 1000");

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#expr(newest_timestamp - oldest_timestamp < SIZE)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}});

        epService.getEPRuntime().setVariableValue("SIZE", 10000);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

        epService.getEPRuntime().setVariableValue("SIZE", 2000);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(6000));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3"}, {"E4"}});

        stmt.destroy();
    }

    private void runAssertionUDFBuiltin(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("udf", LocalUDF.class.getName(), "evaluateExpiryUDF");
        epService.getEPAdministrator().createEPL("select * from SupportBean#expr(udf(theString, view_reference, expired_count))");

        LocalUDF.setResult(true);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertEquals("E1", LocalUDF.getKey());
        assertEquals(0, (int) LocalUDF.getExpiryCount());
        assertNotNull(LocalUDF.getViewref());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        LocalUDF.setResult(false);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertEquals("E3", LocalUDF.getKey());
        assertEquals(2, (int) LocalUDF.getExpiryCount());
        assertNotNull(LocalUDF.getViewref());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from SupportBean#expr(1)",
                "Error starting statement: Error attaching view to event stream: Invalid return value for expiry expression, expected a boolean return value but received int [select * from SupportBean#expr(1)]");

        tryInvalid(epService, "select * from SupportBean#expr((select * from SupportBean#lastevent))",
                "Error starting statement: Error attaching view to event stream: Invalid expiry expression: Sub-select, previous or prior functions are not supported in this context [select * from SupportBean#expr((select * from SupportBean#lastevent))]");
    }

    private void runAssertionNamedWindowDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String[] fields = new String[]{"theString"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window NW#expr(true) as SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPAdministrator().createEPL("insert into NW select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        listener.reset();
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPAdministrator().createEPL("on SupportBean_A delete from NW where theString = id");
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E3"}});
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E2"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPrev(EPServiceProvider epService) {
        String[] fields = new String[]{"val0"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select prev(1, theString) as val0 from SupportBean#expr(true)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        stmt.destroy();
    }

    private void runAssertionAggregation(EPServiceProvider epService) {
        // Test ungrouped
        String[] fields = new String[]{"theString"};
        EPStatement stmtUngrouped = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#expr(sum(intPrimitive) < 10)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtUngrouped.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 9));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E2"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 11));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, new Object[][]{{"E2"}, {"E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 12));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, new Object[][]{{"E4"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E5"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E5"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 2));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E5"}, {"E6"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E6"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 3));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E5"}, {"E6"}, {"E7"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E7"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 6));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E7"}, {"E8"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E8"}}, new Object[][]{{"E5"}, {"E6"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 9));
        EPAssertionUtil.assertPropsPerRow(stmtUngrouped.iterator(), fields, new Object[][]{{"E9"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E9"}}, new Object[][]{{"E7"}, {"E8"}});

        stmtUngrouped.destroy();

        // Test grouped
        EPStatement stmtGrouped = epService.getEPAdministrator().createEPL("select irstream theString from SupportBean#groupwin(intPrimitive)#expr(sum(longPrimitive) < 10)");
        stmtGrouped.addListener(listener);

        sendEvent(epService, "E1", 1, 5);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

        sendEvent(epService, "E2", 2, 2);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, null);

        sendEvent(epService, "E3", 1, 3);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, null);

        sendEvent(epService, "E4", 2, 4);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, null);

        sendEvent(epService, "E5", 2, 6);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E5"}}, new Object[][]{{"E2"}, {"E4"}});

        sendEvent(epService, "E6", 1, 2);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E6"}}, new Object[][]{{"E1"}});

        stmtGrouped.destroy();

        // Test on-delete
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window NW#expr(sum(intPrimitive) < 10) as SupportBean");
        stmt.addListener(listener);
        epService.getEPAdministrator().createEPL("insert into NW select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, null);

        epService.getEPAdministrator().createEPL("on SupportBean_A delete from NW where theString = id");
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, null, new Object[][]{{"E2"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 7));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, new Object[][]{{"E1"}});

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static class LocalUDF {

        private static String key;
        private static Integer expiryCount;
        private static Object viewref;
        private static boolean result;

        public static boolean evaluateExpiryUDF(String key, Object viewref, Integer expiryCount) {
            LocalUDF.key = key;
            LocalUDF.viewref = viewref;
            LocalUDF.expiryCount = expiryCount;
            return result;
        }

        public static String getKey() {
            return key;
        }

        public static Integer getExpiryCount() {
            return expiryCount;
        }

        public static Object getViewref() {
            return viewref;
        }

        public static void setResult(boolean result) {
            LocalUDF.result = result;
        }
    }
}
