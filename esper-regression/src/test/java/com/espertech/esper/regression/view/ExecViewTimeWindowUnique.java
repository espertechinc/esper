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
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecViewTimeWindowUnique implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMonthScoped(epService);
        runAssertionWindowUnique(epService);
        runAssertionWindowUniqueMultiKey(epService);
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select rstream * from SupportBean#time(1 month)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendCurrentTime(epService, "2002-02-15T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        sendCurrentTimeWithMinus(epService, "2002-03-15T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-15T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E2"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    // Make sure the timer and dispatch works for externally timed events and views
    private void runAssertionWindowUnique(EPServiceProvider epService) {
        // Set up a time window with a unique view attached
        EPStatement windowUniqueView = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportMarketDataBean.class.getName() +
                        "#time(3.0)#unique(symbol)");
        SupportUpdateListener listener = new SupportUpdateListener();
        windowUniqueView.addListener(listener);

        sendTimer(epService, 0);

        sendEvent(epService, "IBM");

        assertNull(listener.getLastOldData());
        sendTimer(epService, 4000);
        assertEquals(1, listener.getLastOldData().length);
    }

    // Make sure the timer and dispatch works for externally timed events and views
    private void runAssertionWindowUniqueMultiKey(EPServiceProvider epService) {
        sendTimer(epService, 0);

        // Set up a time window with a unique view attached
        EPStatement windowUniqueView = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportMarketDataBean.class.getName() +
                        "#time(3.0)#unique(symbol, price)");
        SupportUpdateListener listener = new SupportUpdateListener();
        windowUniqueView.addListener(listener);
        String[] fields = new String[]{"symbol", "price", "volume"};

        sendEvent(epService, "IBM", 10, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", 10.0, 1L});

        sendEvent(epService, "IBM", 11, 2L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", 11.0, 2L});

        sendEvent(epService, "IBM", 10, 3L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 10.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 10.0, 1L});
        listener.reset();

        sendEvent(epService, "IBM", 11, 4L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 11.0, 4L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 11.0, 2L});
        listener.reset();

        sendTimer(epService, 2000);
        sendEvent(epService, null, 11, 5L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 11.0, 5L});

        sendTimer(epService, 3000);
        assertEquals(2, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 10.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 11.0, 4L});
        listener.reset();

        sendEvent(epService, null, 11, 6L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{null, 11.0, 6L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{null, 11.0, 5L});
        listener.reset();

        sendTimer(epService, 6000);
        assertEquals(1, listener.getLastOldData().length);
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{null, 11.0, 6L});
        listener.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEvent(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price, Long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, volume, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendTimer(EPServiceProvider epService, long time) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
