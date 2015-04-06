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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestFilterExpressionsLargeThreading extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportEvent", SupportTradeEvent.class);
        config.getEngineDefaults().getExecution().setThreadingProfile(ConfigurationEngineDefaults.ThreadingProfile.LARGE);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNullBooleanExpr()
    {
        String stmtOneText = "every event1=SupportEvent(userId like '123%')";
        EPStatement statement = epService.getEPAdministrator().createPattern(stmtOneText);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportTradeEvent(1, null, 1001));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportTradeEvent(2, "1234", 1001));
        assertEquals(2, listener.assertOneGetNewAndReset().get("event1.id"));
    }
}
