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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ExecViewLengthBatch implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportBean[] events = new SupportBean[10];
        for (int i = 0; i < events.length; i++) {
            events[i] = new SupportBean();
        }

        runAssertionLengthBatchSize2(epService, events);
        runAssertionLengthBatchSize1(epService, events);
        runAssertionLengthBatchSize3(epService, events);
        runAssertionLengthBatchSize3And2Staggered(epService, events);
        runAssertionInvalid(epService);
    }

    private void runAssertionLengthBatchSize2(EPServiceProvider epService, SupportBean[] events) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + "#length_batch(2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(events[0], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0]}, stmt.iterator());

        sendEvent(events[1], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0], events[1]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[2], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[2]}, stmt.iterator());

        sendEvent(events[3], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[2], events[3]}, new SupportBean[]{events[0], events[1]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[4], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[4]}, stmt.iterator());

        sendEvent(events[5], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[4], events[5]}, new SupportBean[]{events[2], events[3]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        stmt.destroy();
    }

    private void runAssertionLengthBatchSize1(EPServiceProvider epService, SupportBean[] events) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + "#length_batch(1)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(events[0], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[1], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[1]}, new SupportBean[]{events[0]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[2], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[2]}, new SupportBean[]{events[1]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        stmt.destroy();
    }

    private void runAssertionLengthBatchSize3(EPServiceProvider epService, SupportBean[] events) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + "#length_batch(3)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(events[0], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0]}, stmt.iterator());

        sendEvent(events[1], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0], events[1]}, stmt.iterator());

        sendEvent(events[2], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0], events[1], events[2]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[3], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[3]}, stmt.iterator());

        sendEvent(events[4], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[3], events[4]}, stmt.iterator());

        sendEvent(events[5], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[3], events[4], events[5]}, new SupportBean[]{events[0], events[1], events[2]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        stmt.destroy();
    }

    private void runAssertionLengthBatchSize3And2Staggered(EPServiceProvider epService, SupportBean[] events) {
        if (SupportConfigFactory.skipTest(ExecViewLengthBatch.class)) {
            return;
        }

        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() + "#length_batch(3)#length_batch(2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(events[0], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[1], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[2], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[0], events[1], events[2]}, null);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[3], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[4], epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        sendEvent(events[5], epService);
        EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), new SupportBean[]{events[3], events[4], events[5]}, new SupportBean[]{events[0], events[1], events[2]});
        EPAssertionUtil.assertEqualsExactOrderUnderlying(null, stmt.iterator());

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        try {
            epService.getEPAdministrator().createEPL(
                    "select * from " + SupportMarketDataBean.class.getName() + "#length_batch(0)");
            fail();
        } catch (Exception ex) {
            // expected
        }
    }

    private void sendEvent(SupportBean theEvent, EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
