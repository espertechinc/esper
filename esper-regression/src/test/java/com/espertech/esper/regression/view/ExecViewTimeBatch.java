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

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecViewTimeBatch implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionMonthScoped(epService);
        runAssertionStartEagerForceUpdate(epService);
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(1 month)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        sendCurrentTimeWithMinus(epService, "2002-04-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-04-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E2"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendCurrentTime(epService, "2002-05-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E3"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStartEagerForceUpdate(EPServiceProvider epService) {
        sendTimer(epService, 1000);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#time_batch(1, \"START_EAGER,FORCE_UPDATE\")");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 1999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 2000);
        assertTrue(listener.getAndClearIsInvoked());

        sendTimer(epService, 2999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 3000);
        assertTrue(listener.getAndClearIsInvoked());
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 4000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        sendTimer(epService, 5000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), "theString".split(","), new Object[]{"E1"});

        sendTimer(epService, 5999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(epService, 6000);
        assertTrue(listener.getAndClearIsInvoked());

        sendTimer(epService, 7000);
        assertTrue(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
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
