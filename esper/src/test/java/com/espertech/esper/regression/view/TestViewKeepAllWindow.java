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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestViewKeepAllWindow extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testIterator()
    {
        String viewExpr = "select symbol, price from " + SupportMarketDataBean.class.getName() + "#keepall()";
        EPStatement statement = epService.getEPAdministrator().createEPL(viewExpr);
        statement.addListener(listener);

        sendEvent("ABC", 20);
        sendEvent("DEF", 100);

        // check iterator results
        Iterator<EventBean> events = statement.iterator();
        EventBean theEvent = events.next();
        assertEquals("ABC", theEvent.get("symbol"));
        assertEquals(20d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("DEF", theEvent.get("symbol"));
        assertEquals(100d, theEvent.get("price"));
        assertFalse(events.hasNext());

        sendEvent("EFG", 50);

        // check iterator results
        events = statement.iterator();
        theEvent = events.next();
        assertEquals("ABC", theEvent.get("symbol"));
        assertEquals(20d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("DEF", theEvent.get("symbol"));
        assertEquals(100d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("EFG", theEvent.get("symbol"));
        assertEquals(50d, theEvent.get("price"));
    }

    public void testWindowStats()
    {
        String viewExpr = "select irstream symbol, count(*) as cnt, sum(price) as mysum from " + SupportMarketDataBean.class.getName() +
                "#keepall() group by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(viewExpr);
        statement.addListener(listener);
        listener.reset();

        sendEvent("S1", 100);
        String[] fields = new String[] {"symbol", "cnt", "mysum"};
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S1", 1L, 100d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S1", 0L, null});
        listener.reset();

        sendEvent("S2", 50);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S2", 1L, 50d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S2", 0L, null});
        listener.reset();

        sendEvent("S1", 5);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S1", 2L, 105d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S1", 1L, 100d});
        listener.reset();

        sendEvent("S2", -1);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"S2", 2L, 49d});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"S2", 1L, 50d});
        listener.reset();
    }

    private void sendEvent(String symbol, double price)
    {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
