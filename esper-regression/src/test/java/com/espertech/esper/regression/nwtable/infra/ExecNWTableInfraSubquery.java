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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static org.junit.Assert.*;

public class ExecNWTableInfraSubquery implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);

        runAssertionSubquerySelfCheck(epService, true);
        runAssertionSubquerySelfCheck(epService, false);

        runAssertionSubqueryDeleteInsertReplace(epService, true);
        runAssertionSubqueryDeleteInsertReplace(epService, false);

        runAssertionInvalidSubquery(epService, true);
        runAssertionInvalidSubquery(epService, false);

        runAssertionUncorrelatedSubqueryAggregation(epService, true);
        runAssertionUncorrelatedSubqueryAggregation(epService, false);
    }

    private void runAssertionUncorrelatedSubqueryAggregation(EPServiceProvider epService, boolean namedWindow) {
        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfraUCS#keepall as select theString as a, longPrimitive as b from " + SupportBean.class.getName() :
                "create table MyInfraUCS(a string primary key, b long)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfraUCS select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select irstream (select sum(b) from MyInfraUCS) as value, symbol from " + SupportMarketDataBean.class.getName();
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendMarketBean(epService, "M1");
        String[] fieldsStmt = new String[]{"value", "symbol"};
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M1"});

        sendSupportBean(epService, "S1", 5L, -1L);
        sendMarketBean(epService, "M2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{5L, "M2"});

        sendSupportBean(epService, "S2", 10L, -1L);
        sendMarketBean(epService, "M3");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{15L, "M3"});

        // create 2nd consumer
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectOne); // same stmt
        SupportUpdateListener listenerStmtTwo = new SupportUpdateListener();
        stmtSelectTwo.addListener(listenerStmtTwo);

        sendSupportBean(epService, "S3", 8L, -1L);
        sendMarketBean(epService, "M4");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{23L, "M4"});
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{23L, "M4"});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraUCS", false);
    }

    private void runAssertionInvalidSubquery(EPServiceProvider epService, boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window MyInfraIS#keepall as " + SupportBean.class.getName() :
                "create table MyInfraIS(theString string)";
        epService.getEPAdministrator().createEPL(eplCreate);

        try {
            epService.getEPAdministrator().createEPL("select (select theString from MyInfraIS#lastevent) from MyInfraIS");
            fail();
        } catch (EPException ex) {
            if (namedWindow) {
                assertEquals("Error starting statement: Failed to plan subquery number 1 querying MyInfraIS: Consuming statements to a named window cannot declare a data window view onto the named window [select (select theString from MyInfraIS#lastevent) from MyInfraIS]", ex.getMessage());
            } else {
                SupportMessageAssertUtil.assertMessage(ex, "Views are not supported with tables");
            }
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraIS", false);
    }

    private void runAssertionSubqueryDeleteInsertReplace(EPServiceProvider epService, boolean namedWindow) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall as select theString as key, intBoxed as value from " + SupportBean.class.getName() :
                "create table MyInfra(key string primary key, value int primary key)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // delete
        String stmtTextDelete = "on " + SupportBean.class.getName() + " delete from MyInfra where key = theString";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as key, intBoxed as value from " + SupportBean.class.getName() + " as s0";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean(epService, "E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        sendSupportBean(epService, "E2", 2);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        }

        sendSupportBean(epService, "E1", 3);
        if (namedWindow) {
            assertEquals(2, listenerWindow.getNewDataList().size());
            EPAssertionUtil.assertProps(listenerWindow.getOldDataList().get(0)[0], fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerWindow.getNewDataList().get(1)[0], fields, new Object[]{"E1", 3});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2}, {"E1", 3}});

        listenerWindow.reset();
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionSubquerySelfCheck(EPServiceProvider epService, boolean namedWindow) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfraSSS#keepall as select theString as key, intBoxed as value from " + SupportBean.class.getName() :
                "create table MyInfraSSS (key string primary key, value int)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into (not does insert if key already exists)
        String stmtTextInsertOne = "insert into MyInfraSSS select theString as key, intBoxed as value from " + SupportBean.class.getName() + " as s0" +
                " where not exists (select * from MyInfraSSS as win where win.key = s0.theString)";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean(epService, "E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        } else {
            assertFalse(listenerWindow.isInvoked());
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        sendSupportBean(epService, "E2", 2);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        sendSupportBean(epService, "E1", 3);
        assertFalse(listenerWindow.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        sendSupportBean(epService, "E3", 4);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 4});
            EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
        }

        // Add delete
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyInfraSSS where key = id";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        // delete E2
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 4}});

        sendSupportBean(epService, "E2", 5);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 5});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 4}, {"E2", 5}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraSSS", false);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, long longPrimitive, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
