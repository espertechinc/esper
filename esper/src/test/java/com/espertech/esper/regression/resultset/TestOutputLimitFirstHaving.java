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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestOutputLimitFirstHaving extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        config.getEngineDefaults().getLogging().setEnableTimerDebug(false);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testHavingNoAvgOutputFirstEvents() {
        String query = "select doublePrimitive from SupportBean having doublePrimitive > 1 output first every 2 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(query);
        statement.addListener(listener);
        runAssertion2Events();
        statement.destroy();

        // test joined
        query = "select doublePrimitive from SupportBean#lastevent,SupportBean_ST0#lastevent st0 having doublePrimitive > 1 output first every 2 events";
        statement = epService.getEPAdministrator().createEPL(query);
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID", 1));
        statement.addListener(listener);
        runAssertion2Events();
    }

    public void testHavingNoAvgOutputFirstMinutes() {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = "val0".split(",");
        String query = "select sum(doublePrimitive) as val0 from SupportBean#length(5) having sum(doublePrimitive) > 100 output first every 2 seconds";
        EPStatement statement = epService.getEPAdministrator().createEPL(query);
        statement.addListener(listener);

        sendBeanEvent(10);
        sendBeanEvent(80);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        sendBeanEvent(11);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{101d});

        sendBeanEvent(1);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2999));
        sendBeanEvent(1);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3000));
        sendBeanEvent(1);
        assertFalse(listener.isInvoked());

        sendBeanEvent(100);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{114d});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(4999));
        sendBeanEvent(0);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        sendBeanEvent(0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{102d});
    }

    public void testHavingAvgOutputFirstEveryTwoMinutes()
    {
        String query = "select doublePrimitive, avg(doublePrimitive) from SupportBean having doublePrimitive > 2*avg(doublePrimitive) output first every 2 minutes";
        EPStatement statement = epService.getEPAdministrator().createEPL(query);
        statement.addListener(listener);

        sendBeanEvent(1);
        assertFalse(listener.isInvoked());

        sendBeanEvent(2);
        assertFalse(listener.isInvoked());
    
        sendBeanEvent(9);
        assertTrue(listener.isInvoked());
     }


    private void runAssertion2Events() {

        sendBeanEvent(1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(2);
        assertTrue(listener.getAndClearIsInvoked());

        sendBeanEvent(9);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(2);
        assertTrue(listener.getAndClearIsInvoked());

        sendBeanEvent(1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(2);
        assertTrue(listener.getAndClearIsInvoked());

        sendBeanEvent(2);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(2);
        assertTrue(listener.getAndClearIsInvoked());
    }

    private void sendBeanEvent(double doublePrimitive) {
        SupportBean b = new SupportBean();
        b.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(b);
    }
}

