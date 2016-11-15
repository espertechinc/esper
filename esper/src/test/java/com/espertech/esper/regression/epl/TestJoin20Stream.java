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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
 import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;

import java.io.StringWriter;

public class TestJoin20Stream extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void test20StreamJoin()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class.getName());
        
        StringWriter buf = new StringWriter();
        buf.append("select * from ");

        String delimiter = "";
        for (int i = 0; i < 20; i++) {
            buf.append(delimiter);
            buf.append("S0(id=" + i + ")#lastevent() as s_" + i);
            delimiter = ", ";
        }
        EPStatement stmt = epService.getEPAdministrator().createEPL(buf.toString());
        stmt.addListener(listener);

        for (int i = 0; i < 19; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i));
        }
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_S0(19));
        assertTrue(listener.isInvoked());
    }
}