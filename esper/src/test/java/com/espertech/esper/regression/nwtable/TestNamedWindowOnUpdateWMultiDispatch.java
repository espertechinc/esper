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
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TestNamedWindowOnUpdateWMultiDispatch extends TestCase
{
    public void testMultipleDataWindowIntersectOnUpdate() {
        runAssertion(true, null, null);
        runAssertion(false, true, ConfigurationEngineDefaults.Threading.Locking.SPIN);
        runAssertion(false, true, ConfigurationEngineDefaults.Threading.Locking.SUSPEND);
        runAssertion(false, false, null);
    }

    private void runAssertion(boolean useDefault, Boolean preserve, ConfigurationEngineDefaults.Threading.Locking locking) {
        Configuration config = SupportConfigFactory.getConfiguration();
        if (!useDefault) {
            config.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchPreserveOrder(preserve);
            config.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchLocking(locking);
        }

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "company,value,total".split(",");

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        // ESPER-568
        epService.getEPAdministrator().createEPL("create schema S2 ( company string, value double, total double)");
	    EPStatement stmtWin = epService.getEPAdministrator().createEPL("create window S2Win#time(25 hour)#firstunique(company) as S2");
        epService.getEPAdministrator().createEPL("insert into S2Win select * from S2#firstunique(company)");
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
        // this example does not have @priority thereby it is undefined whether there are two counts delivered or one
        if (listener.getLastNewData().length == 2) {
            assertEquals(1L, listener.getLastNewData()[0].get("cnt"));
            assertEquals(2L, listener.getLastNewData()[1].get("cnt"));
        }
        else {
            assertEquals(2L, listener.assertOneGetNewAndReset().get("cnt"));
        }
        EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), fields, new Object[][]{{"AComp", 3.0, 7.0}, {"BComp", 4.0, 0.0}});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void createSendEvent(EPServiceProvider engine, String typeName, String company, double value, double total) {
        HashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("company", company);
        map.put("value", value);
        map.put("total", total);
        if (EventRepresentationEnum.getEngineDefault(engine).isObjectArrayEvent()) {
            engine.getEPRuntime().sendEvent(map.values().toArray(), typeName);
        }
        else {
            engine.getEPRuntime().sendEvent(map, typeName);
        }
    }
}