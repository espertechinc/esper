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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestInfraOnDelete extends TestCase
{
    private EPServiceProviderSPI epService;
    private SupportUpdateListener listenerInfra;
    private SupportUpdateListener listenerDelete;
    private SupportUpdateListener listenerSelect;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerInfra = new SupportUpdateListener();
        listenerDelete = new SupportUpdateListener();
        listenerSelect = new SupportUpdateListener();        
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_A.class, SupportBean_B.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerInfra = null;
        listenerDelete = null;
        listenerSelect = null;
    }
    
    public void testDeleteCondition() throws Exception {
        runAssertionDeleteCondition(true);
        runAssertionDeleteCondition(false);
    }

    public void testDeletePattern() throws Exception {
        runAssertionDeletePattern(true);
        runAssertionDeletePattern(false);
    }

    public void testDeleteAll() throws Exception {
        runAssertionDeleteAll(true);
        runAssertionDeleteAll(false);
    }

    private void runAssertionDeleteAll(boolean namedWindow) throws Exception
    {
        // create window
        String stmtTextCreate = namedWindow ?
                "@Name('CreateInfra') create window MyInfra#keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName() :
                "@Name('CreateInfra') create table MyInfra (a string primary key, b int)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerInfra);

        // create delete stmt
        String stmtTextDelete = "@Name('OnDelete') on " + SupportBean_A.class.getName() + " delete from MyInfra";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerDelete);
        EPAssertionUtil.assertEqualsAnyOrder(stmtDelete.getEventType().getPropertyNames(), new String[]{"a", "b"});

        // create insert into
        String stmtTextInsertOne = "@Name('Insert') insert into MyInfra select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String[] fields = new String[] {"a", "b"};
        String stmtTextSelect = "@Name('Select') select irstream MyInfra.a as a, b from MyInfra as s1";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listenerSelect);

        // Delete all events, no result expected
        sendSupportBean_A("A1");
        assertFalse(listenerInfra.isInvoked());
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerDelete.isInvoked());
        assertEquals(0, getCount("MyInfra"));

        // send 1 event
        sendSupportBean("E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        else {
            assertFalse(listenerInfra.isInvoked());
            assertFalse(listenerSelect.isInvoked());
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, null);
        assertEquals(1, getCount("MyInfra"));

        // Delete all events, 1 row expected
        sendSupportBean_A("A2");
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E1", 1}});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        assertEquals(0, getCount("MyInfra"));

        // send 2 events
        sendSupportBean("E2", 2);
        sendSupportBean("E3", 3);
        listenerInfra.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2}, {"E3", 3}});
        assertFalse(listenerDelete.isInvoked());
        assertEquals(2, getCount("MyInfra"));

        // Delete all events, 2 rows expected
        sendSupportBean_A("A2");
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.getLastOldData()[0], fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertProps(listenerInfra.getLastOldData()[1], fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E2", 2}, {"E3", 3}});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        assertEquals(2, listenerDelete.getLastNewData().length);
        EPAssertionUtil.assertProps(listenerDelete.getLastNewData()[0], fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertProps(listenerDelete.getLastNewData()[1], fields, new Object[]{"E3", 3});
        assertEquals(0, getCount("MyInfra"));

        listenerInfra.reset();
        listenerDelete.reset();
        listenerSelect.reset();
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionDeletePattern(boolean isNamedWindow) throws Exception
    {
        // create infra
        String stmtTextCreate = isNamedWindow ?
                "create window MyInfra#keepall() as select theString as a, intPrimitive as b from SupportBean" :
                "create table MyInfra(a string primary key, b int)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerInfra);

        // create delete stmt
        String stmtTextDelete = "on pattern [every ea=" + SupportBean_A.class.getName() + " or every eb=" + SupportBean_B.class.getName() + "] " + " delete from MyInfra";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // send 1 event
        String[] fields = new String[] {"a", "b"};
        sendSupportBean("E1", 1);
        if (isNamedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, null);
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});
        assertEquals(1, getCount("MyInfra"));

        // Delete all events using A, 1 row expected
        sendSupportBean_A("A1");
        if (isNamedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E1", 1}});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        assertEquals(0, getCount("MyInfra"));

        // send 1 event
        sendSupportBean("E2", 2);
        if (isNamedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2}});
        assertEquals(1, getCount("MyInfra"));

        // Delete all events using B, 1 row expected
        sendSupportBean_B("B1");
        if (isNamedWindow) {
            EPAssertionUtil.assertProps(listenerInfra.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E2", 2}});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        assertEquals(0, getCount("MyInfra"));

        stmtDelete.destroy();
        stmtCreate.destroy();
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionDeleteCondition(boolean isNamedWindow) throws Exception {    
    
        // create infra
        String stmtTextCreate = isNamedWindow ?
                "create window MyInfra#keepall() as select theString as a, intPrimitive as b from SupportBean" :
                "create table MyInfra (a string primary key, b int)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerInfra);

        // create delete stmt
        String stmtTextDelete = "on SupportBean_A delete from MyInfra where 'X' || a || 'X' = id";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create delete stmt
        stmtTextDelete = "on SupportBean_B delete from MyInfra where b < 5";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // send 3 event
        sendSupportBean("E1", 1);
        sendSupportBean("E2", 2);
        sendSupportBean("E3", 3);
        assertEquals(3, getCount("MyInfra"));
        listenerInfra.reset();
        String[] fields = new String[] {"a", "b"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

        // delete E2
        sendSupportBean_A("XE2X");
        if (isNamedWindow) {
            assertEquals(1, listenerInfra.getLastOldData().length);
            EPAssertionUtil.assertProps(listenerInfra.getLastOldData()[0], fields, new Object[]{"E2", 2});
        }
        listenerInfra.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        assertEquals(2, getCount("MyInfra"));

        sendSupportBean("E7", 7);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E7", 7}});
        assertEquals(3, getCount("MyInfra"));

        // delete all under 5
        sendSupportBean_B("B1");
        if (isNamedWindow) {
            assertEquals(2, listenerInfra.getLastOldData().length);
            EPAssertionUtil.assertProps(listenerInfra.getLastOldData()[0], fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerInfra.getLastOldData()[1], fields, new Object[]{"E3", 3});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E7", 7}});
        assertEquals(1, getCount("MyInfra"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private SupportBean_A sendSupportBean_A(String id)
    {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean_B sendSupportBean_B(String id)
    {
        SupportBean_B bean = new SupportBean_B(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private long getCount(String windowOrTableName) throws Exception
    {
        return (Long) epService.getEPRuntime().executeQuery("select count(*) as c0 from " + windowOrTableName).getArray()[0].get("c0");
    }
}
