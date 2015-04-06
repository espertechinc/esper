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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewLengthBatch extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private SupportBean events[];

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        events = new SupportBean[100];
        for (int i = 0; i < events.length; i++)
        {
            events[i] = new SupportBean();
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testLengthBatchSize2()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + ".win:length_batch(2)");
        stmt.addListener(listener);

        sendEvent(events[0]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0]}, stmt.iterator());

        sendEvent(events[1]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0], events[1]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[2]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[2]}, stmt.iterator());

        sendEvent(events[3]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[2], events[3]}, new SupportBean[]{events[0], events[1]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[4]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[4]}, stmt.iterator());

        sendEvent(events[5]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[4], events[5]}, new SupportBean[]{events[2], events[3]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());
    }

    public void testLengthBatchSize1()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + ".win:length_batch(1)");
        stmt.addListener(listener);

        sendEvent(events[0]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[1]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[1]}, new SupportBean[]{events[0]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[2]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[2]}, new SupportBean[]{events[1]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());
    }

    public void testLengthBatchSize3()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + ".win:length_batch(3)");
        stmt.addListener(listener);

        sendEvent(events[0]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0]}, stmt.iterator());

        sendEvent(events[1]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0], events[1]}, stmt.iterator());

        sendEvent(events[2]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0], events[1], events[2]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[3]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[3]}, stmt.iterator());

        sendEvent(events[4]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[3], events[4]}, stmt.iterator());

        sendEvent(events[5]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[3], events[4], events[5]}, new SupportBean[]{events[0], events[1], events[2]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());
    }

    public void testLengthBatchSize3And2Staggered()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + ".win:length_batch(3).win:length_batch(2)");
        stmt.addListener(listener);

        sendEvent(events[0]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[1]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[2]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0], events[1], events[2]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[3]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[4]);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[5]);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[3], events[4], events[5]}, new SupportBean[]{events[0], events[1], events[2]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());
    }

    public void testInvalid()
    {
        try
        {
            epService.getEPAdministrator().createEPL(
                "select * from " + SupportMarketDataBean.class.getName() + ".win:length_batch(0)");
            fail();
        }
        catch (Exception ex)
        {
            // expected
        }
    }

    private void sendEvent(SupportBean theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
