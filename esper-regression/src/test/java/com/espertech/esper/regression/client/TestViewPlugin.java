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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewPlugin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("A", SupportMarketDataBean.class);
        configuration.addPlugInView("mynamespace", "flushedsimple", MyFlushedSimpleViewFactory.class.getName());
        configuration.addPlugInView("mynamespace", "invalid", String.class.getName());
        epService = EPServiceProviderManager.getProvider("TestViewPlugin", configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
        epService.destroy();
    }

    public void testPlugInViewFlushed()
    {
        String text = "select * from A.mynamespace:flushedsimple(price)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(testListener);

        sendEvent(1);
        sendEvent(2);
        assertFalse(testListener.isInvoked());

        stmt.stop();
        assertEquals(2, testListener.getLastNewData().length);
    }

    public void testPlugInViewTrend()
    {
        epService.getEPAdministrator().getConfiguration().addPlugInView("mynamespace", "trendspotter", MyTrendSpotterViewFactory.class.getName());
        String text = "select irstream * from A.mynamespace:trendspotter(price)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(testListener);

        sendEvent(10);
        assertReceived(1L, null);

        sendEvent(11);
        assertReceived(2L, 1L);

        sendEvent(12);
        assertReceived(3L, 2L);

        sendEvent(11);
        assertReceived(0L, 3L);

        sendEvent(12);
        assertReceived(1L, 0L);

        sendEvent(0);
        assertReceived(0L, 1L);

        sendEvent(0);
        assertReceived(0L, 0L);

        sendEvent(1);
        assertReceived(1L, 0L);

        sendEvent(1);
        assertReceived(1L, 1L);

        sendEvent(2);
        assertReceived(2L, 1L);

        sendEvent(2);
        assertReceived(2L, 2L);
    }

    public void testInvalid()
    {
        tryInvalid("select * from A.mynamespace:xxx()",
                "Error starting statement: View name 'mynamespace:xxx' is not a known view name [select * from A.mynamespace:xxx()]");
        tryInvalid("select * from A.mynamespace:invalid()", "Error starting statement: Error casting view factory instance to com.espertech.esper.view.ViewFactory interface for view 'invalid' [select * from A.mynamespace:invalid()]");
    }

    private void sendEvent(double price)
    {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("", price, null, null));
    }

    private void assertReceived(Long newTrendCount, Long oldTrendCount)
    {
        EPAssertionUtil.assertPropsPerRow(testListener.assertInvokedAndReset(), "trendcount", new Object[]{newTrendCount}, new Object[]{oldTrendCount});
    }

    private void tryInvalid(String stmtText, String expectedMsg)
    {
        try
        {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(expectedMsg, ex.getMessage());
        }
    }
}
