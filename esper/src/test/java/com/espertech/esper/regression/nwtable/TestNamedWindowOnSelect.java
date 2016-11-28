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
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.support.util.IndexBackingTableInfo;
import junit.framework.TestCase;

public class TestNamedWindowOnSelect extends TestCase implements IndexBackingTableInfo
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerSelect;
    private SupportUpdateListener listenerConsumer;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerSelect = new SupportUpdateListener();
        listenerConsumer = new SupportUpdateListener();
        SupportQueryPlanIndexHook.reset();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerSelect = null;
        listenerConsumer = null;
    }

    public void testInsertIntoWildcardUndType()
    {
        String[] fields = new String[] {"theString", "intPrimitive"};

        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select * from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from " + SupportBean.class.getName() + "(theString like 'E%')";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create on-select stmt
        String stmtTextSelect = "on " + SupportBean_A.class.getName() + " insert into MyStream select mywin.* from MyWindow as mywin order by theString asc";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listenerSelect);
        assertEquals(StatementType.ON_INSERT, ((EPStatementSPI) stmtSelect).getStatementMetadata().getStatementType());

        // create consuming statement
        String stmtTextConsumer = "select * from default.MyStream";
        EPStatement stmtConsumer = epService.getEPAdministrator().createEPL(stmtTextConsumer);
        stmtConsumer.addListener(listenerConsumer);

        // create second inserting statement
        String stmtTextInsertTwo = "insert into MyStream select * from " + SupportBean.class.getName() + "(theString like 'I%')";
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        // send event
        sendSupportBean("E1", 1);
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerConsumer.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // fire trigger
        sendSupportBean_A("A1");
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertProps(listenerConsumer.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        // insert via 2nd insert into
        sendSupportBean("I2", 2);
        assertFalse(listenerSelect.isInvoked());
        EPAssertionUtil.assertProps(listenerConsumer.assertOneGetNewAndReset(), fields, new Object[]{"I2", 2});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // send event
        sendSupportBean("E3", 3);
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerConsumer.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

        // fire trigger
        sendSupportBean_A("A2");
        assertEquals(1, listenerSelect.getNewDataList().size());
        EPAssertionUtil.assertPropsPerRow(listenerSelect.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        listenerSelect.reset();
        assertEquals(2, listenerConsumer.getNewDataList().size());
        EPAssertionUtil.assertPropsPerRow(listenerConsumer.getNewDataListFlattened(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        listenerConsumer.reset();

        // check type
        EventType consumerType = stmtConsumer.getEventType();
        assertEquals(String.class, consumerType.getPropertyType("theString"));
        assertTrue(consumerType.getPropertyNames().length > 10);
        assertEquals(SupportBean.class, consumerType.getUnderlyingType());

        // check type
        EventType onSelectType = stmtSelect.getEventType();
        assertEquals(String.class, onSelectType.getPropertyType("theString"));
        assertTrue(onSelectType.getPropertyNames().length > 10);
        assertEquals(SupportBean.class, onSelectType.getUnderlyingType());

        // delete all from named window
        String stmtTextDelete = "on " + SupportBean_B.class.getName() + " delete from MyWindow";
        epService.getEPAdministrator().createEPL(stmtTextDelete);
        sendSupportBean_B("B1");

        // fire trigger - nothing to insert
        sendSupportBean_A("A3");

        stmtConsumer.destroy();
        stmtSelect.destroy();
        stmtCreate.destroy();
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

    private void assertCountS0Window(long expected) {
        assertEquals(expected, epService.getEPRuntime().executeQuery("select count(*) as c0 from S0Window").getArray()[0].get("c0"));
    }
}
