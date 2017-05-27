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

import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanTimestamp;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecViewTimeInterval implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        runAssertionTimeWindowPreparedStmt(epService);
        runAssertionTimeWindowVariableStmt(epService);
        runAssertionTimeWindowTimePeriod(epService);
        runAssertionTimeWindowVariableTimePeriodStmt(epService);
        runAssertionTimeWindow(epService);
        runAssertionTimeBatchNoRefPoint(epService);
        runAssertionTimeBatchRefPoint(epService);
        runAssertionExternallyTimedMonthScoped(epService);
        runAssertionExternallyTimedBatchMonthScoped(epService);
        runAssertionExternallyTimed(epService);
    }

    private void runAssertionTimeWindowPreparedStmt(EPServiceProvider epService) {
        sendTimer(epService, 0);
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

        runAssertion(epService, listenerOne, listenerTwo);

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionTimeWindowVariableStmt(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String text = "select rstream theString from SupportBean#time(TIME_WIN_ONE)";
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        epService.getEPAdministrator().getConfiguration().addVariable("TIME_WIN_ONE", int.class, 4);
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        epService.getEPRuntime().setVariableValue("TIME_WIN_ONE", 3);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(epService, listenerOne, listenerTwo);

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionTimeWindowTimePeriod(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String text = "select rstream theString from SupportBean#time(4 sec)";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        text = "select rstream theString from SupportBean#time(3000 milliseconds)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(epService, listenerOne, listenerTwo);

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionTimeWindowVariableTimePeriodStmt(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addVariable("TIME_WIN_TWO", double.class, 4000);
        sendTimer(epService, 0);

        String text = "select rstream theString from SupportBean#time(TIME_WIN_TWO milliseconds)";
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        text = "select rstream theString from SupportBean#time(TIME_WIN_TWO minutes)";
        epService.getEPRuntime().setVariableValue("TIME_WIN_TWO", 0.05);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        runAssertion(epService, listenerOne, listenerTwo);

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionTimeWindow(EPServiceProvider epService) {
        tryTimeWindow(epService, "30000");
        tryTimeWindow(epService, "30E6 milliseconds");
        tryTimeWindow(epService, "30000 seconds");
        tryTimeWindow(epService, "500 minutes");
        tryTimeWindow(epService, "8.33333333333333333333 hours");
        tryTimeWindow(epService, "0.34722222222222222222222222222222 days");
        tryTimeWindow(epService, "0.1 hour 490 min 240 sec");
    }

    private void runAssertionTimeBatchNoRefPoint(EPServiceProvider epService) {
        // Set up a time window with a unique view attached
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                        "#time_batch(10 minutes)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 0);

        sendEvent(epService);
        listener.reset();

        sendTimerAssertNotInvoked(epService, listener, 10 * 60 * 1000 - 1);
        sendTimerAssertInvoked(epService, listener, 10 * 60 * 1000);

        stmt.destroy();
    }

    private void runAssertionTimeBatchRefPoint(EPServiceProvider epService) {
        // Set up a time window with a unique view attached
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                        "#time_batch(10 minutes, 10L)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 10);

        sendEvent(epService);
        listener.reset();

        sendTimerAssertNotInvoked(epService, listener, 10 * 60 * 1000 - 1 + 10);
        sendTimerAssertInvoked(epService, listener, 10 * 60 * 1000 + 10);

        stmt.destroy();
    }

    private void runAssertionExternallyTimedMonthScoped(EPServiceProvider epService) {
        SupportUpdateListener testListener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanTimestamp.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream * from SupportBean#ext_timed(longPrimitive, 1 month)");
        stmt.addListener(testListener);

        sendExtTimeEvent(epService, DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"), "E1");
        sendExtTimeEvent(epService, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000") - 1, "E2");
        assertFalse(testListener.isInvoked());

        sendExtTimeEvent(epService, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000"), "E3");
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        stmt.destroy();
    }

    private void runAssertionExternallyTimedBatchMonthScoped(EPServiceProvider epService) {
        SupportUpdateListener testListener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanTimestamp.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#ext_timed_batch(longPrimitive, 1 month)");
        stmt.addListener(testListener);

        sendExtTimeEvent(epService, DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"), "E1");
        sendExtTimeEvent(epService, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000") - 1, "E2");
        assertFalse(testListener.isInvoked());

        sendExtTimeEvent(epService, DateTime.parseDefaultMSec("2002-03-01T09:00:00.000"), "E3");
        EPAssertionUtil.assertPropsPerRow(testListener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}});

        stmt.destroy();
    }

    private void runAssertionExternallyTimed(EPServiceProvider epService) {
        // Set up a time window with a unique view attached
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() +
                        "#ext_timed(longPrimitive, 10 minutes)");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        sendExtTimeEvent(epService, 0);

        testListener.reset();
        sendExtTimeEvent(epService, 10 * 60 * 1000 - 1);
        assertNull(testListener.getOldDataList().get(0));

        testListener.reset();
        sendExtTimeEvent(epService, 10 * 60 * 1000 + 1);
        assertEquals(1, testListener.getOldDataList().get(0).length);

        stmt.destroy();
    }

    private void tryTimeWindow(EPServiceProvider epService, String intervalSpec) {
        // Set up a time window with a unique view attached
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportBean.class.getName() +
                        "#time(" + intervalSpec + ")");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 0);

        sendEvent(epService);
        listener.reset();

        sendTimerAssertNotInvoked(epService, listener, 29999 * 1000);
        sendTimerAssertInvoked(epService, listener, 30000 * 1000);

        stmt.destroy();
    }

    private void sendTimerAssertNotInvoked(EPServiceProvider epService, SupportUpdateListener listener, long timeInMSec) {
        sendTimer(epService, timeInMSec);
        assertFalse(listener.isInvoked());
        listener.reset();
    }

    private void sendTimerAssertInvoked(EPServiceProvider epService, SupportUpdateListener listener, long timeInMSec) {
        sendTimer(epService, timeInMSec);
        assertTrue(listener.isInvoked());
        listener.reset();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService) {
        SupportBean theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, String theString) {
        SupportBean theEvent = new SupportBean(theString, 1);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendExtTimeEvent(EPServiceProvider epService, long longPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendExtTimeEvent(EPServiceProvider epService, long longPrimitive, String theString) {
        SupportBean theEvent = new SupportBean(theString, 0);
        theEvent.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void runAssertion(EPServiceProvider epService, SupportUpdateListener listenerOne, SupportUpdateListener listenerTwo) {
        sendTimer(epService, 1000);
        sendEvent(epService, "E1");

        sendTimer(epService, 2000);
        sendEvent(epService, "E2");

        sendTimer(epService, 3000);
        sendEvent(epService, "E3");

        assertFalse(listenerOne.isInvoked());
        assertFalse(listenerTwo.isInvoked());

        sendTimer(epService, 4000);
        assertEquals("E1", listenerTwo.assertOneGetNewAndReset().get("theString"));
        assertFalse(listenerTwo.isInvoked());

        sendTimer(epService, 5000);
        assertEquals("E1", listenerOne.assertOneGetNewAndReset().get("theString"));
        assertFalse(listenerOne.isInvoked());
    }
}
