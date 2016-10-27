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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.TimeZone;

public class TestTimerScheduleObserverTimeZoneEST extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setShareViews(false);
        config.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        config.getEngineDefaults().getExpression().setTimeZone(TimeZone.getTimeZone("GMT-4:00"));
        epService = EPServiceProviderManager.getDefaultProvider(config);
        listener = new SupportUpdateListener();
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testCurrentTimeWTime() {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T08:59:00.000GMT-04:00");

        String epl = "select * from pattern[timer:schedule(date: current_timestamp.withTime(9, 0, 0, 0))]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        sendCurrentTime(iso, "2012-10-01T08:59:59.999GMT-4:00");
        assertFalse(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-10-01T09:00:00.000GMT-4:00");
        assertTrue(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-10-03T09:00:00.000GMT-4:00");
        assertFalse(listener.getIsInvokedAndReset());

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void sendCurrentTime(EPServiceProviderIsolated iso, String time) {
        iso.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSecWZone(time)));
    }
}


