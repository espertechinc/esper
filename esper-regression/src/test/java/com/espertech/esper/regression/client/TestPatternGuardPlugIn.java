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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestPatternGuardPlugIn extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInPatternGuard("myplugin", "count_to", MyCountToPatternGuardFactory.class.getName());
        configuration.addEventType("Bean", SupportBean.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testGuard()
    {
        if (SupportConfigFactory.skipTest(TestPatternGuardPlugIn.class)) {
            return;
        }

        String stmtText = "select * from pattern [(every Bean) where myplugin:count_to(10)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        for (int i = 0; i < 10; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean());
            assertTrue(listener.isInvoked());
            listener.reset();
        }
        
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());
    }

    public void testGuardVariable()
    {
        if (SupportConfigFactory.skipTest(TestPatternGuardPlugIn.class)) {
            return;
        }

        epService.getEPAdministrator().createEPL("create variable int COUNT_TO = 3");
        String stmtText = "select * from pattern [(every Bean) where myplugin:count_to(COUNT_TO)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        for (int i = 0; i < 3; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean());
            assertTrue(listener.isInvoked());
            listener.reset();
        }

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());
    }

    public void testInvalid()
    {
        if (SupportConfigFactory.skipTest(TestPatternGuardPlugIn.class)) {
            return;
        }

        try
        {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.addPlugInPatternGuard("namespace", "name", String.class.getName());
            epService = EPServiceProviderManager.getDefaultProvider(configuration);
            epService.initialize();
            String stmtText = "select * from pattern [every " + SupportBean.class.getName() +
                               " where namespace:name(10)]";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            SupportMessageAssertUtil.assertMessage(ex, "Failed to resolve pattern guard '" + SupportBean.class.getName() + " where namespace:name(10)': Error casting guard factory instance to com.espertech.esper.pattern.guard.GuardFactory interface for guard 'name'");
        }
    }
}
