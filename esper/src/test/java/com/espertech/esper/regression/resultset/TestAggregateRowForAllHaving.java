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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import junit.framework.TestCase;

public class TestAggregateRowForAllHaving extends TestCase
{
    private final static String JOIN_KEY = "KEY";

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

    public void testSumOneView()
    {
        String viewExpr = "select irstream sum(longBoxed) as mySum " +
                          "from " + SupportBean.class.getName() + ".win:time(10 seconds) " +
                          "having sum(longBoxed) > 10";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssert(selectTestView);
    }

    public void testSumJoin()
    {
        String viewExpr = "select irstream sum(longBoxed) as mySum " +
                          "from " + SupportBeanString.class.getName() + ".win:time(10 seconds) as one, " +
                                    SupportBean.class.getName() + ".win:time(10 seconds) as two " +
                          "where one.theString = two.theString " +
                          "having sum(longBoxed) > 10";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        runAssert(selectTestView);
    }

    private void runAssert(EPStatement selectTestView)
    {
        // assert select result type
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("mySum"));

        sendTimerEvent(0);
        sendEvent(10);
        assertFalse(listener.isInvoked());

        sendTimerEvent(5000);
        sendEvent(15);
        assertEquals(25L, listener.getAndResetLastNewData()[0].get("mySum"));

        sendTimerEvent(8000);
        sendEvent(-5);
        assertEquals(20L, listener.getAndResetLastNewData()[0].get("mySum"));
        assertNull(listener.getLastOldData());

        sendTimerEvent(10000);
        assertEquals(20L, listener.getLastOldData()[0].get("mySum"));
        assertNull(listener.getAndResetLastNewData());
    }

    public void testAvgGroupWindow()
    {
        //String stmtText = "select istream avg(price) as aprice from "+ SupportMarketDataBean.class.getName()
        //        +".std:groupwin(symbol).win:length(1) having avg(price) <= 0";
        String stmtText = "select istream avg(price) as aprice from "+ SupportMarketDataBean.class.getName()
                +".std:unique(symbol) having avg(price) <= 0";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        sendEvent("A", -1);
        assertEquals(-1.0d, listener.getLastNewData()[0].get("aprice"));
        listener.reset();

        sendEvent("A", 5);
        assertFalse(listener.isInvoked());

        sendEvent("B", -6);
        assertEquals(-.5d, listener.getLastNewData()[0].get("aprice"));
        listener.reset();

        sendEvent("C", 2);
        assertFalse(listener.isInvoked());

        sendEvent("C", 3);
        assertFalse(listener.isInvoked());

        sendEvent("C", -2);
        assertEquals(-1d, listener.getLastNewData()[0].get("aprice"));
        listener.reset();
    }

    private Object sendEvent(String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, null, null);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private void sendEvent(long longBoxed, int intBoxed, short shortBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(long longBoxed)
    {
        sendEvent(longBoxed, 0, (short)0);
    }

    private void sendTimerEvent(long msec)
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
    }

    private static final Log log = LogFactory.getLog(TestAggregateRowForAllHaving.class);
}
