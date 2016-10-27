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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewTimeBatch extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testMonthScoped() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime("2002-02-01T09:00:00.000");
        epService.getEPAdministrator().createEPL("select * from SupportBean.win:time_batch(1 month)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendCurrentTimeWithMinus("2002-03-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime("2002-03-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[] {"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        sendCurrentTimeWithMinus("2002-04-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime("2002-04-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[] {"E2"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendCurrentTime("2002-05-01T09:00:00.000");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[] {"E3"});
    }

    public void testStartEagerForceUpdate()
    {
        sendTimer(1000);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean.win:time_batch(1, \"START_EAGER,FORCE_UPDATE\")");
        stmt.addListener(listener);

        sendTimer(1999);
        assertFalse(listener.getAndClearIsInvoked());
        
        sendTimer(2000);
        assertTrue(listener.getAndClearIsInvoked());

        sendTimer(2999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(3000);
        assertTrue(listener.getAndClearIsInvoked());
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(4000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        sendTimer(5000);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), "theString".split(","), new Object[]{"E1"});

        sendTimer(5999);
        assertFalse(listener.getAndClearIsInvoked());

        sendTimer(6000);
        assertTrue(listener.getAndClearIsInvoked());

        sendTimer(7000);
        assertTrue(listener.getAndClearIsInvoked());
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendCurrentTime(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
