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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

public class TestCountAll extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private EPStatement selectTestView;

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

    public void testSize()
    {
        String statementText = "select irstream size from " + SupportMarketDataBean.class.getName() + ".std:size()";
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);

        sendEvent("DELL", 1L);
        assertSize(1, 0);

        sendEvent("DELL", 1L);
        assertSize(2, 1);

        selectTestView.destroy();
        statementText = "select size, symbol, feed from " + SupportMarketDataBean.class.getName() + ".std:size(symbol, feed)";
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);
        String[] fields = "size,symbol,feed".split(",");

        sendEvent("DELL", 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, "DELL", "f1"});

        sendEvent("DELL", 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, "DELL", "f1"});
    }

    public void testCountPlusStar()
    {
        // Test for ESPER-118
        String statementText = "select *, count(*) as cnt from " + SupportMarketDataBean.class.getName();
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);

        sendEvent("S0", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1L, listener.getLastNewData()[0].get("cnt"));
        assertEquals("S0", listener.getLastNewData()[0].get("symbol"));

        sendEvent("S1", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2L, listener.getLastNewData()[0].get("cnt"));
        assertEquals("S1", listener.getLastNewData()[0].get("symbol"));

        sendEvent("S2", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3L, listener.getLastNewData()[0].get("cnt"));
        assertEquals("S2", listener.getLastNewData()[0].get("symbol"));
    }

    public void testCount()
    {
    	String statementText = "select count(*) as cnt from " + SupportMarketDataBean.class.getName() + ".win:time(1)";
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);
       
        sendEvent("DELL", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1L, listener.getLastNewData()[0].get("cnt"));
        
        sendEvent("DELL", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2L, listener.getLastNewData()[0].get("cnt"));
        
        sendEvent("DELL", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3L, listener.getLastNewData()[0].get("cnt"));

        // test invalid distinct
        SupportMessageAssertUtil.tryInvalid(epService, "select count(distinct *) from " + SupportMarketDataBean.class.getName(),
                "Error starting statement: Failed to validate select-clause expression 'count(distinct *)': Invalid use of the 'distinct' keyword with count and wildcard");
    }

    public void testCountHaving()
    {
        String theEvent = SupportBean.class.getName();
        String statementText = "select irstream sum(intPrimitive) as mysum from " + theEvent + " having sum(intPrimitive) = 2";
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);

        sendEvent();
        assertFalse(listener.getAndClearIsInvoked());
        sendEvent();
        assertEquals(2, listener.assertOneGetNewAndReset().get("mysum"));
        sendEvent();
        assertEquals(2, listener.assertOneGetOldAndReset().get("mysum"));
    }

    public void testSumHaving()
    {
        String theEvent = SupportBean.class.getName();
        String statementText = "select irstream count(*) as mysum from " + theEvent + " having count(*) = 2";
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);

        sendEvent();
        assertFalse(listener.getAndClearIsInvoked());
        sendEvent();
        assertEquals(2L, listener.assertOneGetNewAndReset().get("mysum"));
        sendEvent();
        assertEquals(2L, listener.assertOneGetOldAndReset().get("mysum"));
    }

    private void sendEvent(String symbol, Long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "f1");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent()
    {
        SupportBean bean = new SupportBean("", 1);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertSize(long newSize, long oldSize)
    {
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "size", new Object[]{newSize}, new Object[]{oldSize});
    }
}
