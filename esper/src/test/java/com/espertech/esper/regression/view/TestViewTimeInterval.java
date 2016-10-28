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
import com.espertech.esper.support.bean.SupportBeanTimestamp;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewTimeInterval extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testTimeWindowPreparedStmt()
    {
        sendTimer(0);
        String text = "select rstream theString from SupportBean#time(?)";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(text);

        prepared.setObject(1, 4);
        EPStatement stmtOne = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        prepared.setObject(1, 3);
        EPStatement stmtTwo = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(listenerOne, listenerTwo);
    }

    public void testTimeWindowVariableStmt()
    {
        sendTimer(0);
        String text = "select rstream theString from SupportBean#time(TIME_WIN)";
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        epService.getEPAdministrator().getConfiguration().addVariable("TIME_WIN", int.class, 4);
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        epService.getEPRuntime().setVariableValue("TIME_WIN", 3);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(listenerOne, listenerTwo);
    }

    public void testTimeWindowTimePeriod()
    {
        sendTimer(0);

        String text = "select rstream theString from SupportBean#time(4 sec)";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        text = "select rstream theString from SupportBean#time(3000 milliseconds)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(listenerOne, listenerTwo);
    }

    public void testTimeWindowVariableTimePeriodStmt()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("TIME_WIN", double.class, 4000);
        sendTimer(0);
        
        String text = "select rstream theString from SupportBean#time(TIME_WIN milliseconds)";
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        text = "select rstream theString from SupportBean#time(TIME_WIN minutes)";
        epService.getEPRuntime().setVariableValue("TIME_WIN", 0.05);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(listenerOne, listenerTwo);
    }

    public void testTimeWindow()
    {
        tryTimeWindow("30000");
        tryTimeWindow("30E6 milliseconds");
        tryTimeWindow("30000 seconds");
        tryTimeWindow("500 minutes");
        tryTimeWindow("8.33333333333333333333 hours");
        tryTimeWindow("0.34722222222222222222222222222222 days");
        tryTimeWindow("0.1 hour 490 min 240 sec");
    }

    public void testTimeBatchNoRefPoint()
    {
        // Set up a time window with a unique view attached
        EPStatement view = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                "#time_batch(10 minutes)");
        testListener = new SupportUpdateListener();
        view.addListener(testListener);

        sendTimer(0);

        sendEvent();
        testListener.reset();

        sendTimerAssertNotInvoked(10*60*1000 - 1);
        sendTimerAssertInvoked(10*60*1000);
    }

    public void testTimeBatchRefPoint()
    {
        // Set up a time window with a unique view attached
        EPStatement view = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                "#time_batch(10 minutes, 10L)");
        testListener = new SupportUpdateListener();
        view.addListener(testListener);

        sendTimer(10);

        sendEvent();
        testListener.reset();

        sendTimerAssertNotInvoked(10*60*1000 - 1 + 10);
        sendTimerAssertInvoked(10*60*1000 + 10);
    }

    public void testExternallyTimedMonthScoped() {
        testListener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanTimestamp.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream * from SupportBean#ext_timed(longPrimitive, 1 month)");
        stmt.addListener(testListener);

        sendExtTimeEvent(DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"), "E1");
        sendExtTimeEvent(DateTime.parseDefaultMSec("2002-03-01T09:00:00.000") - 1, "E2");
        assertFalse(testListener.isInvoked());

        sendExtTimeEvent(DateTime.parseDefaultMSec("2002-03-01T09:00:00.000"), "E3");
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});
    }

    public void testExternallyTimedBatchMonthScoped() {
        testListener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanTimestamp.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#ext_timed_batch(longPrimitive, 1 month)");
        stmt.addListener(testListener);

        sendExtTimeEvent(DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"), "E1");
        sendExtTimeEvent(DateTime.parseDefaultMSec("2002-03-01T09:00:00.000") - 1, "E2");
        assertFalse(testListener.isInvoked());

        sendExtTimeEvent(DateTime.parseDefaultMSec("2002-03-01T09:00:00.000"), "E3");
        EPAssertionUtil.assertPropsPerRow(testListener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}});
    }

    public void testExternallyTimed()
    {
        // Set up a time window with a unique view attached
        EPStatement view = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() +
                "#ext_timed(longPrimitive, 10 minutes)");
        testListener = new SupportUpdateListener();
        view.addListener(testListener);

        sendExtTimeEvent(0);

        testListener.reset();
        sendExtTimeEvent(10*60*1000-1);
        assertNull(testListener.getOldDataList().get(0));

        testListener.reset();
        sendExtTimeEvent(10*60*1000+1);
        assertEquals(1, testListener.getOldDataList().get(0).length);
    }

    private void tryTimeWindow(String intervalSpec)
    {
        // Set up a time window with a unique view attached
        EPStatement view = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() +
                "#time(" + intervalSpec + ")");
        testListener = new SupportUpdateListener();
        view.addListener(testListener);

        sendTimer(0);

        sendEvent();
        testListener.reset();

        sendTimerAssertNotInvoked(29999*1000);
        sendTimerAssertInvoked(30000*1000);
    }

    private void sendTimerAssertNotInvoked(long timeInMSec)
    {
        sendTimer(timeInMSec);
        assertFalse(testListener.isInvoked());
        testListener.reset();
    }

    private void sendTimerAssertInvoked(long timeInMSec)
    {
        sendTimer(timeInMSec);
        assertTrue(testListener.isInvoked());
        testListener.reset();
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent()
    {
        SupportBean theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvent(String theString)
    {
        SupportBean theEvent = new SupportBean(theString, 1);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendExtTimeEvent(long longPrimitive)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendExtTimeEvent(long longPrimitive, String theString)
    {
        SupportBean theEvent = new SupportBean(theString, 0);
        theEvent.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void runAssertion(SupportUpdateListener listenerOne, SupportUpdateListener listenerTwo)
    {
        sendTimer(1000);
        sendEvent("E1");

        sendTimer(2000);
        sendEvent("E2");

        sendTimer(3000);
        sendEvent("E3");

        assertFalse(listenerOne.isInvoked());
        assertFalse(listenerTwo.isInvoked());

        sendTimer(4000);
        assertEquals("E1", listenerTwo.assertOneGetNewAndReset().get("theString"));
        assertFalse(listenerTwo.isInvoked());

        sendTimer(5000);
        assertEquals("E1", listenerOne.assertOneGetNewAndReset().get("theString"));
        assertFalse(listenerOne.isInvoked());
    }

    private void sendCurrentTime(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
