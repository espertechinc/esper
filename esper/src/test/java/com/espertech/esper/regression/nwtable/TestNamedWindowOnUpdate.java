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
import com.espertech.esper.support.bean.SupportBeanAbstractSub;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TestNamedWindowOnUpdate extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerWindow = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
    }

    public void testUpdateNonPropertySet() {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("setBeanLongPrimitive999", this.getClass().getName(), "setBeanLongPrimitive999");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S0 as sb " +
                "update MyWindow as mywin" +
                " set mywin.setIntPrimitive(10)," +
                "     setBeanLongPrimitive999(mywin)");
        stmt.addListener(listenerWindow);

        String[] fields = "intPrimitive,longPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listenerWindow.getAndResetLastNewData()[0], fields, new Object[]{10, 999L});
    }

    public void testMultipleDataWindowIntersect() {
        String stmtTextCreate = "create window MyWindow.std:unique(theString).win:length(2) as select * from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        String stmtTextInsertOne = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextUpdate = "on SupportBean_A update MyWindow set intPrimitive=intPrimitive*100 where theString=id";
        epService.getEPAdministrator().createEPL(stmtTextUpdate);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EventBean[] newevents = listenerWindow.getLastNewData();
        EventBean[] oldevents = listenerWindow.getLastOldData();

        assertEquals(1, newevents.length);
        EPAssertionUtil.assertProps(newevents[0], "intPrimitive".split(","), new Object[]{300});
        assertEquals(1, oldevents.length);
        oldevents = EPAssertionUtil.sort(oldevents, "theString");
        EPAssertionUtil.assertPropsPerRow(oldevents, "theString,intPrimitive".split(","), new Object[][]{{"E2", 3}});

        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E2", 300}});
    }
    
    public void testMultipleDataWindowIntersectOnUpdate() {
        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "company,value,total".split(",");

        // ESPER-568
        epService.getEPAdministrator().createEPL("create schema S2 ( company string, value double, total double)");
	    EPStatement stmtWin = epService.getEPAdministrator().createEPL("create window S2Win.win:time(25 hour).std:firstunique(company) as S2");
        epService.getEPAdministrator().createEPL("insert into S2Win select * from S2.std:firstunique(company)");
        epService.getEPAdministrator().createEPL("on S2 as a update S2Win as b set total = b.value + a.value");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select count(*) as cnt from S2Win");
        stmt.addListener(listener);

        createSendEvent(epService, "S2", "AComp", 3.0, 0.0);
        assertEquals(1L, listener.assertOneGetNewAndReset().get("cnt"));
        EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), fields, new Object[][]{{"AComp", 3.0, 0.0}});

        createSendEvent(epService, "S2", "AComp", 6.0, 0.0);
        assertEquals(1L, listener.assertOneGetNewAndReset().get("cnt"));
        EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), fields, new Object[][]{{"AComp", 3.0, 9.0}});

        createSendEvent(epService, "S2", "AComp", 5.0, 0.0);
        assertEquals(1L, listener.assertOneGetNewAndReset().get("cnt"));
        EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), fields, new Object[][]{{"AComp", 3.0, 8.0}});

        createSendEvent(epService, "S2", "BComp", 4.0, 0.0);
        assertEquals(2L, listener.assertOneGetNewAndReset().get("cnt"));
        EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), fields, new Object[][]{{"AComp", 3.0, 7.0}, {"BComp", 4.0, 0.0}});
    }

    private void createSendEvent(EPServiceProvider engine, String typeName, String company, double value, double total) {
        HashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("company", company);
        map.put("value", value);
        map.put("total", total);
        if (EventRepresentationEnum.getEngineDefault(epService).isObjectArrayEvent()) {
            engine.getEPRuntime().sendEvent(map.values().toArray(), typeName);
        }
        else {
            engine.getEPRuntime().sendEvent(map, typeName);
        }
    }

    public void testMultipleDataWindowUnion() {
        String stmtTextCreate = "create window MyWindow.std:unique(theString).win:length(2) retain-union as select * from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        String stmtTextInsertOne = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextUpdate = "on SupportBean_A update MyWindow mw set mw.intPrimitive=intPrimitive*100 where theString=id";
        epService.getEPAdministrator().createEPL(stmtTextUpdate);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EventBean[] newevents = listenerWindow.getLastNewData();
        EventBean[] oldevents = listenerWindow.getLastOldData();

        assertEquals(1, newevents.length);
        EPAssertionUtil.assertProps(newevents[0], "intPrimitive".split(","), new Object[]{300});
        assertEquals(1, oldevents.length);
        EPAssertionUtil.assertPropsPerRow(oldevents, "theString,intPrimitive".split(","), new Object[][]{{"E2", 3}});

        EventBean[] events = EPAssertionUtil.sort(stmtCreate.iterator(), "theString");
        EPAssertionUtil.assertPropsPerRow(events, "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E2", 300}});
    }

    public void testSubclass()
    {
        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select * from " + SupportBeanAbstractSub.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from " + SupportBeanAbstractSub.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create update
        String stmtTextUpdate = "on " + SupportBean.class.getName() + " update MyWindow set v1=theString, v2=theString";
        epService.getEPAdministrator().createEPL(stmtTextUpdate);
        
        epService.getEPRuntime().sendEvent(new SupportBeanAbstractSub("value2"));
        listenerWindow.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], new String[]{"v1", "v2"}, new Object[]{"E1", "E1"});
    }

    public static void setBeanLongPrimitive999(SupportBean event) {
        event.setLongPrimitive(999);
    }
}