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
package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.util.SupportEngineFactory;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestOutputLimitMicrosecondResolution extends TestCase {

    private Map<TimeUnit, EPServiceProvider> epServices;

    public void setUp() {
        epServices = SupportEngineFactory.setupEnginesByTimeUnit();
    }

    public void tearDown() {
        epServices = null;
    }

    public void testMicrosecond() {
        for (EPServiceProvider epService : epServices.values()) {
            epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        }

        runAssertionOutputLimit(epServices.get(TimeUnit.MILLISECONDS), 0, "1", 1000, 1000);
        runAssertionOutputLimit(epServices.get(TimeUnit.MICROSECONDS), 0, "1", 1000000, 1000000);
        runAssertionOutputLimit(epServices.get(TimeUnit.MILLISECONDS), 789123456789L, "0.1", 789123456789L + 100, 100);
        runAssertionOutputLimit(epServices.get(TimeUnit.MICROSECONDS), 789123456789L, "0.1", 789123456789L + 100000, 100000);
    }

    private void runAssertionOutputLimit(EPServiceProvider epService, long startTime, String size, long flipTime, long repeatTime) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("isolated");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = isolated.getEPAdministrator().createEPL("select * from SupportBean output every " + size + " seconds", "s0", null);
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime - 1));
        assertFalse(listener.isInvoked());

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime));
        assertTrue(listener.getIsInvokedAndReset());

        isolated.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(repeatTime + flipTime - 1));
        assertFalse(listener.isInvoked());

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(repeatTime + flipTime));
        assertTrue(listener.getIsInvokedAndReset());

        isolated.destroy();
    }
}
