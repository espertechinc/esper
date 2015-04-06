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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.HashMap;

public class TestNamedWindowProcessingOrder extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("Event", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testDispatchBackQueue() {
        runAssertionDispatchBackQueue(EventRepresentationEnum.OBJECTARRAY);
        runAssertionDispatchBackQueue(EventRepresentationEnum.DEFAULT);
        runAssertionDispatchBackQueue(EventRepresentationEnum.MAP);
    }

    public void runAssertionDispatchBackQueue(EventRepresentationEnum eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema StartValueEvent as (dummy string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TestForwardEvent as (prop1 string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TestInputEvent as (dummy string)");
        epService.getEPAdministrator().createEPL("insert into TestForwardEvent select'V1' as prop1 from TestInputEvent");

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window NamedWin.std:unique(prop1) (prop1 string, prop2 string)");

        epService.getEPAdministrator().createEPL("insert into NamedWin select 'V1' as prop1, 'O1' as prop2 from StartValueEvent");

        epService.getEPAdministrator().createEPL("on TestForwardEvent update NamedWin as work set prop2 = 'U1' where work.prop1 = 'V1'");

        String[] fields = "prop1,prop2".split(",");
        String eplSelect = "select irstream prop1, prop2 from NamedWin";
        epService.getEPAdministrator().createEPL(eplSelect).addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {"dummyValue"}, "StartValueEvent");
        }
        else {
            epService.getEPRuntime().sendEvent(new HashMap<String, String>(), "StartValueEvent");
        }

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"V1", "O1"});

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {"dummyValue"}, "TestInputEvent");
        }
        else {
            epService.getEPRuntime().sendEvent(new HashMap<String, String>(), "TestInputEvent");
        }
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"V1", "O1"});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], fields, new Object[]{"V1", "U1"});

        epService.initialize();
    }

    public void testOrderedDeleteAndSelect()
    {
        String stmtText;
        stmtText = "create window MyWindow.std:lastevent() as select * from Event";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "insert into MyWindow select * from Event";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "on MyWindow e delete from MyWindow win where win.theString=e.theString and e.intPrimitive = 7";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "on MyWindow e delete from MyWindow win where win.theString=e.theString and e.intPrimitive = 5";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "on MyWindow e insert into ResultStream select e.* from MyWindow";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "select * from ResultStream";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 7));
        assertFalse("E1", listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 8));
        assertEquals("E2", listener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        assertFalse("E3", listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 6));
        assertEquals("E4", listener.assertOneGetNewAndReset().get("theString"));
    }
}
