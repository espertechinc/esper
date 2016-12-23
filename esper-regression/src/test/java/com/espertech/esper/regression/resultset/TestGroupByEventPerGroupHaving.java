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
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestGroupByEventPerGroupHaving extends TestCase
{
    private static String SYMBOL_DELL = "DELL";
    private static String SYMBOL_IBM = "IBM";

    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        testListener = null;
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testHavingCount()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String text = "select * from SupportBean(intPrimitive = 3)#length(10) as e1 group by theString having count(*) > 2";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(text);
        selectTestView.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 3));
        assertFalse(testListener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 3));
        assertTrue(testListener.isInvoked());
    }

    public void testSumJoin()
    {
        String viewExpr = "select irstream symbol, sum(price) as mySum " +
                          "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                          " " + SupportMarketDataBean.class.getName() + "#length(3) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE')" +
                          "       and one.theString = two.symbol " +
                          "group by symbol " +
                          "having sum(price) >= 100";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        runAssertion();
    }

    public void testSumOneView()
    {
        String viewExpr = "select irstream symbol, sum(price) as mySum " +
                          "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol " +
                          "having sum(price) >= 100";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        runAssertion();
    }

    private void runAssertion()
    {
        sendEvent(SYMBOL_DELL, 10);
        assertFalse(testListener.isInvoked());

        sendEvent(SYMBOL_DELL, 60);
        assertFalse(testListener.isInvoked());

        sendEvent(SYMBOL_DELL, 30);
        assertNewEvent(SYMBOL_DELL, 100);

        sendEvent(SYMBOL_IBM, 30);
        assertOldEvent(SYMBOL_DELL, 100);

        sendEvent(SYMBOL_IBM, 80);
        assertNewEvent(SYMBOL_IBM, 110);
    }

    private void assertNewEvent(String symbol, double newSum)
    {
        EventBean[] oldData = testListener.getLastOldData();
        EventBean[] newData = testListener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals(newSum, newData[0].get("mySum"));
        assertEquals(symbol, newData[0].get("symbol"));

        testListener.reset();
        assertFalse(testListener.isInvoked());
    }

    private void assertOldEvent(String symbol, double newSum)
    {
        EventBean[] oldData = testListener.getLastOldData();
        EventBean[] newData = testListener.getLastNewData();

        assertNull(newData);
        assertEquals(1, oldData.length);

        assertEquals(newSum, oldData[0].get("mySum"));
        assertEquals(symbol, oldData[0].get("symbol"));

        testListener.reset();
        assertFalse(testListener.isInvoked());
    }

    private void sendEvent(String symbol, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestGroupByEventPerGroupHaving.class);
}
