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
package com.espertech.esper.regression.resultset.outputlimit;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecOutputLimitFirstHaving implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        configuration.getEngineDefaults().getLogging().setEnableTimerDebug(false);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);

        runAssertionHavingNoAvgOutputFirstEvents(epService);
        runAssertionHavingNoAvgOutputFirstMinutes(epService);
        runAssertionHavingAvgOutputFirstEveryTwoMinutes(epService);
    }

    private void runAssertionHavingNoAvgOutputFirstEvents(EPServiceProvider epService) {
        String query = "select doublePrimitive from SupportBean having doublePrimitive > 1 output first every 2 events";
        EPStatement statement = epService.getEPAdministrator().createEPL(query);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        tryAssertion2Events(epService, listener);
        statement.destroy();

        // test joined
        query = "select doublePrimitive from SupportBean#lastevent,SupportBean_ST0#lastevent st0 having doublePrimitive > 1 output first every 2 events";
        statement = epService.getEPAdministrator().createEPL(query);
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID", 1));
        statement.addListener(listener);
        tryAssertion2Events(epService, listener);
    }

    private void runAssertionHavingNoAvgOutputFirstMinutes(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = "val0".split(",");
        String query = "select sum(doublePrimitive) as val0 from SupportBean#length(5) having sum(doublePrimitive) > 100 output first every 2 seconds";
        EPStatement statement = epService.getEPAdministrator().createEPL(query);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendBeanEvent(epService, 10);
        sendBeanEvent(epService, 80);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        sendBeanEvent(epService, 11);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{101d});

        sendBeanEvent(epService, 1);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2999));
        sendBeanEvent(epService, 1);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3000));
        sendBeanEvent(epService, 1);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, 100);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{114d});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(4999));
        sendBeanEvent(epService, 0);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        sendBeanEvent(epService, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{102d});
    }

    private void runAssertionHavingAvgOutputFirstEveryTwoMinutes(EPServiceProvider epService) {
        String query = "select doublePrimitive, avg(doublePrimitive) from SupportBean having doublePrimitive > 2*avg(doublePrimitive) output first every 2 minutes";
        EPStatement statement = epService.getEPAdministrator().createEPL(query);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendBeanEvent(epService, 1);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, 2);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, 9);
        assertTrue(listener.isInvoked());
    }


    private void tryAssertion2Events(EPServiceProvider epService, SupportUpdateListener listener) {

        sendBeanEvent(epService, 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 2);
        assertTrue(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 9);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 2);
        assertTrue(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 2);
        assertTrue(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 2);
        assertFalse(listener.getAndClearIsInvoked());

        sendBeanEvent(epService, 2);
        assertTrue(listener.getAndClearIsInvoked());
    }

    private void sendBeanEvent(EPServiceProvider epService, double doublePrimitive) {
        SupportBean b = new SupportBean();
        b.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(b);
    }
}

