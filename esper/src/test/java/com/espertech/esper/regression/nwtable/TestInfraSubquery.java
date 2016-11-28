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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

public class TestInfraSubquery extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerStmtOne;
    private SupportUpdateListener listenerStmtTwo;
    private SupportUpdateListener listenerStmtDelete;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);
        listenerWindow = new SupportUpdateListener();
        listenerStmtOne = new SupportUpdateListener();
        listenerStmtTwo = new SupportUpdateListener();
        listenerStmtDelete = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
        listenerStmtOne = null;
        listenerStmtTwo = null;
        listenerStmtDelete = null;
    }

    public void testSubquerySelfCheck() {
        runAssertionSubquerySelfCheck(true);
        runAssertionSubquerySelfCheck(false);
    }

    public void testSubqueryDeleteInsertReplace() {
        runAssertionSubqueryDeleteInsertReplace(true);
        runAssertionSubqueryDeleteInsertReplace(false);
    }

    public void testInvalidSubquery() {
        runAssertionInvalidSubquery(true);
        runAssertionInvalidSubquery(false);
    }

    public void testAssertionUncorrelatedSubqueryAggregation() {
        runAssertionUncorrelatedSubqueryAggregation(true);
        runAssertionUncorrelatedSubqueryAggregation(false);
    }

    private void runAssertionUncorrelatedSubqueryAggregation(boolean namedWindow)
    {
        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall as select theString as a, longPrimitive as b from " + SupportBean.class.getName() :
                "create table MyInfra(a string primary key, b long)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select irstream (select sum(b) from MyInfra) as value, symbol from " + SupportMarketDataBean.class.getName();
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendMarketBean("M1");
        String fieldsStmt[] = new String[] {"value", "symbol"};
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M1"});

        sendSupportBean("S1", 5L, -1L);
        sendMarketBean("M2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{5L, "M2"});

        sendSupportBean("S2", 10L, -1L);
        sendMarketBean("M3");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{15L, "M3"});

        // create 2nd consumer
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectOne); // same stmt
        stmtSelectTwo.addListener(listenerStmtTwo);

        sendSupportBean("S3", 8L, -1L);
        sendMarketBean("M4");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{23L, "M4"});
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{23L, "M4"});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    public void runAssertionInvalidSubquery(boolean namedWindow)
    {
        String eplCreate = namedWindow ?
            "create window MyInfra#keepall as " + SupportBean.class.getName() :
            "create table MyInfra(theString string)";
        epService.getEPAdministrator().createEPL(eplCreate);

        try
        {
            epService.getEPAdministrator().createEPL("select (select theString from MyInfra#lastevent) from MyInfra");
            fail();
        }
        catch (EPException ex)
        {
            if (namedWindow) {
                assertEquals("Error starting statement: Failed to plan subquery number 1 querying MyInfra: Consuming statements to a named window cannot declare a data window view onto the named window [select (select theString from MyInfra#lastevent) from MyInfra]", ex.getMessage());
            }
            else {
                SupportMessageAssertUtil.assertMessage(ex, "Views are not supported with tables");
            }
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionSubqueryDeleteInsertReplace(boolean namedWindow)
    {
        String fields[] = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall as select theString as key, intBoxed as value from " + SupportBean.class.getName() :
                "create table MyInfra(key string primary key, value int primary key)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // delete
        String stmtTextDelete = "on " + SupportBean.class.getName() + " delete from MyInfra where key = theString";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as key, intBoxed as value from " + SupportBean.class.getName() + " as s0";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean("E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        sendSupportBean("E2", 2);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        }
        else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        }

        sendSupportBean("E1", 3);
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

    private void runAssertionSubquerySelfCheck(boolean namedWindow)
    {
        String fields[] = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall as select theString as key, intBoxed as value from " + SupportBean.class.getName() :
                "create table MyInfra (key string primary key, value int)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into (not does insert if key already exists)
        String stmtTextInsertOne = "insert into MyInfra select theString as key, intBoxed as value from " + SupportBean.class.getName() + " as s0" +
                                    " where not exists (select * from MyInfra as win where win.key = s0.theString)";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean("E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        else {
            assertFalse(listenerWindow.isInvoked());
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        sendSupportBean("E2", 2);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        sendSupportBean("E1", 3);
        assertFalse(listenerWindow.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        sendSupportBean("E3", 4);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 4});
            EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
        }
        else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
        }

        // Add delete
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyInfra where key = id";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        // delete E2
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 4}});

        sendSupportBean("E2", 5);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 5});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 4}, {"E2", 5}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private SupportBean sendSupportBean(String theString, long longPrimitive, Long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0l, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
