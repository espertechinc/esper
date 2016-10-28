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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.client.SupportConfigFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import junit.framework.TestCase;

public class TestGroupByEventPerRowHaving extends TestCase
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
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testGroupByHaving() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionGroupByHaving(false);
        runAssertionGroupByHaving(true);
    }

    private void runAssertionGroupByHaving(boolean join) {
        String epl = !join ?
                "select * from SupportBean#length_batch(3) group by theString having count(*) > 1" :
                "select theString, intPrimitive from SupportBean_S0#lastevent(), SupportBean#length_batch(3) group by theString having count(*) > 1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));

        EventBean[] received = testListener.getNewDataListFlattened();
        EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive".split(","),
                new Object[][] {{"E2", 20}, {"E2", 21}});
        testListener.reset();
        stmt.destroy();
    }

    public void testSumOneView()
    {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = "select irstream symbol, volume, sum(price) as mySum " +
                          "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol " +
                          "having sum(price) >= 50";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        runAssertion(selectTestView);
    }

    public void testSumJoin()
    {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = "select irstream symbol, volume, sum(price) as mySum " +
                          "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                                    SupportMarketDataBean.class.getName() + "#length(3) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                          "  and one.theString = two.symbol " +
                          "group by symbol " +
                          "having sum(price) >= 50";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        runAssertion(selectTestView);
    }

    private void runAssertion(EPStatement selectTestView)
    {
        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));

        String[] fields = "symbol,volume,mySum".split(",");
        sendEvent(SYMBOL_DELL, 10000, 49);
        assertFalse(testListener.isInvoked());

        sendEvent(SYMBOL_DELL, 20000, 54);
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), fields, new Object[]{SYMBOL_DELL, 20000L, 103d});

        sendEvent(SYMBOL_IBM, 1000, 10);
        assertFalse(testListener.isInvoked());

        sendEvent(SYMBOL_IBM, 5000, 20);
        EPAssertionUtil.assertProps(testListener.assertOneGetOldAndReset(), fields, new Object[]{SYMBOL_DELL, 10000L, 54d});

        sendEvent(SYMBOL_IBM, 6000, 5);
        assertFalse(testListener.isInvoked());
    }

    private void sendEvent(String symbol, long volume, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestGroupByEventPerRowHaving.class);
}
