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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.MyFlushedSimpleViewFactory;
import com.espertech.esper.supportregression.client.MyTrendSpotterViewFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecClientViewPlugin implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("A", SupportMarketDataBean.class);
        configuration.addPlugInView("mynamespace", "flushedsimple", MyFlushedSimpleViewFactory.class.getName());
        configuration.addPlugInView("mynamespace", "invalid", String.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPlugInViewFlushed(epService);
        runAssertionPlugInViewTrend(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionPlugInViewFlushed(EPServiceProvider epService) {
        String text = "select * from A.mynamespace:flushedsimple(price)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        sendEvent(epService, 1);
        sendEvent(epService, 2);
        assertFalse(testListener.isInvoked());

        stmt.stop();
        assertEquals(2, testListener.getLastNewData().length);
    }

    private void runAssertionPlugInViewTrend(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInView("mynamespace", "trendspotter", MyTrendSpotterViewFactory.class.getName());
        String text = "select irstream * from A.mynamespace:trendspotter(price)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        sendEvent(epService, 10);
        assertReceived(testListener, 1L, null);

        sendEvent(epService, 11);
        assertReceived(testListener, 2L, 1L);

        sendEvent(epService, 12);
        assertReceived(testListener, 3L, 2L);

        sendEvent(epService, 11);
        assertReceived(testListener, 0L, 3L);

        sendEvent(epService, 12);
        assertReceived(testListener, 1L, 0L);

        sendEvent(epService, 0);
        assertReceived(testListener, 0L, 1L);

        sendEvent(epService, 0);
        assertReceived(testListener, 0L, 0L);

        sendEvent(epService, 1);
        assertReceived(testListener, 1L, 0L);

        sendEvent(epService, 1);
        assertReceived(testListener, 1L, 1L);

        sendEvent(epService, 2);
        assertReceived(testListener, 2L, 1L);

        sendEvent(epService, 2);
        assertReceived(testListener, 2L, 2L);

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from A.mynamespace:xxx()",
                "Error starting statement: View name 'mynamespace:xxx' is not a known view name [select * from A.mynamespace:xxx()]");
        tryInvalid(epService, "select * from A.mynamespace:invalid()", "Error starting statement: Error casting view factory instance to com.espertech.esper.view.ViewFactory interface for view 'invalid' [select * from A.mynamespace:invalid()]");
    }

    private void sendEvent(EPServiceProvider epService, double price) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("", price, null, null));
    }

    private void assertReceived(SupportUpdateListener testListener, Long newTrendCount, Long oldTrendCount) {
        EPAssertionUtil.assertPropsPerRow(testListener.assertInvokedAndReset(), "trendcount", new Object[]{newTrendCount}, new Object[]{oldTrendCount});
    }
}
