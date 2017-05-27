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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecViewTimeAccum implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        SupportMarketDataBean[] events = new SupportMarketDataBean[100];
        for (int i = 0; i < events.length; i++) {
            int group = i % 10;
            events[i] = new SupportMarketDataBean("S" + Integer.toString(group), "id_" + Integer.toString(i), i);
        }

        runAssertionMonthScoped(epService);
        runAssertionTimeAccum(epService, events);
        runAssertionTimeAccumRStream(epService, events);
        runAssertionPreviousAndPrior(epService, events);
        runAssertionSum(epService, events);
        runAssertionGroupedWindow(epService, events);
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream * from SupportBean#time_accum(1 month)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}});

        stmt.destroy();
    }

    private void runAssertionTimeAccum(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportMarketDataBean.class.getName() +
                        "#time_accum(10 sec)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        sendTimer(epService, startTime + 10000);
        assertFalse(listener.isInvoked());

        // 1st at 10 sec
        engine.sendEvent(events[0]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[0]);

        // 2nd event at 14 sec
        sendTimer(epService, startTime + 14000);
        engine.sendEvent(events[1]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[1]);

        // 3nd event at 14 sec
        sendTimer(epService, startTime + 14000);
        engine.sendEvent(events[2]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[2]);

        // 3rd event at 23 sec
        sendTimer(epService, startTime + 23000);
        engine.sendEvent(events[3]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[3]);

        // no event till 33 sec
        sendTimer(epService, startTime + 32999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 33000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(4, listener.getLastOldData().length);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2], events[3]}, listener.getOldDataListFlattened());
        listener.reset();

        // no events till 50 sec
        sendTimer(epService, startTime + 50000);
        assertFalse(listener.isInvoked());

        // next two events at 55 sec
        sendTimer(epService, startTime + 55000);
        engine.sendEvent(events[4]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[4]);
        engine.sendEvent(events[5]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[5]);

        // no event till 65 sec
        sendTimer(epService, startTime + 64999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 65000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(2, listener.getLastOldData().length);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[4], events[5]}, listener.getOldDataListFlattened());
        listener.reset();

        // next window
        engine.sendEvent(events[6]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[6]);

        sendTimer(epService, startTime + 74999);
        engine.sendEvent(events[7]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[7]);

        sendTimer(epService, startTime + 74999 + 10000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(2, listener.getLastOldData().length);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6], events[7]}, listener.getOldDataListFlattened());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionTimeAccumRStream(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select rstream * from " + SupportMarketDataBean.class.getName() +
                        "#time_accum(10 sec)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        sendTimer(epService, startTime + 10000);
        assertFalse(listener.isInvoked());

        // some events at 10 sec
        engine.sendEvent(events[0]);
        engine.sendEvent(events[1]);
        engine.sendEvent(events[2]);
        assertFalse(listener.isInvoked());

        // flush out of the window
        sendTimer(epService, startTime + 20000);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, listener.getNewDataListFlattened());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionPreviousAndPrior(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream price, prev(1, price) as prevPrice, prior(1, price) as priorPrice from " + SupportMarketDataBean.class.getName() +
                        "#time_accum(10 sec)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // 1st event
        sendTimer(epService, startTime + 20000);
        engine.sendEvent(events[5]);
        assertData(listener.assertOneGetNewAndReset(), 5d, null, null);

        // 2nd event
        sendTimer(epService, startTime + 25000);
        engine.sendEvent(events[6]);
        assertData(listener.assertOneGetNewAndReset(), 6d, 5d, 5d);

        // 3nd event
        sendTimer(epService, startTime + 34000);
        engine.sendEvent(events[7]);
        assertData(listener.assertOneGetNewAndReset(), 7d, 6d, 6d);

        sendTimer(epService, startTime + 43999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 44000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(3, listener.getLastOldData().length);
        assertData(listener.getLastOldData()[0], 5d, null, null);
        assertData(listener.getLastOldData()[1], 6d, null, 5d);
        assertData(listener.getLastOldData()[2], 7d, null, 6d);
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionSum(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream sum(price) as sumPrice from " + SupportMarketDataBean.class.getName() +
                        "#time_accum(10 sec)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // 1st event
        sendTimer(epService, startTime + 20000);
        engine.sendEvent(events[5]);
        assertData(listener.getLastNewData()[0], 5d);
        assertData(listener.getLastOldData()[0], null);
        listener.reset();

        // 2nd event
        sendTimer(epService, startTime + 25000);
        engine.sendEvent(events[6]);
        assertData(listener.getLastNewData()[0], 11d);
        assertData(listener.getLastOldData()[0], 5d);
        listener.reset();

        sendTimer(epService, startTime + 34999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 35000);
        assertData(listener.getLastNewData()[0], null);
        assertData(listener.getLastOldData()[0], 11d);
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionGroupedWindow(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportMarketDataBean.class.getName() +
                        "#groupwin(symbol)#time_accum(10 sec)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // 1st S1 event
        sendTimer(epService, startTime + 10000);
        engine.sendEvent(events[1]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[1]);

        // 1st S2 event
        sendTimer(epService, startTime + 12000);
        engine.sendEvent(events[2]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[2]);

        // 2nd S1 event
        sendTimer(epService, startTime + 15000);
        engine.sendEvent(events[11]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[11]);

        // 2nd S2 event
        sendTimer(epService, startTime + 18000);
        engine.sendEvent(events[12]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[12]);

        // 3rd S1 event
        sendTimer(epService, startTime + 21000);
        engine.sendEvent(events[21]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[21]);

        sendTimer(epService, startTime + 28000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(2, listener.getLastOldData().length);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[2], events[12]}, listener.getOldDataListFlattened());
        listener.reset();

        // 3rd S2 event
        sendTimer(epService, startTime + 29000);
        engine.sendEvent(events[32]);
        assertSame(listener.assertOneGetNewAndReset().getUnderlying(), events[32]);

        sendTimer(epService, startTime + 31000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(3, listener.getLastOldData().length);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[1], events[11], events[21]}, listener.getOldDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000);
        assertNull(listener.getLastNewData());
        assertEquals(1, listener.getLastOldData().length);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[32]}, listener.getOldDataListFlattened());
        listener.reset();

        stmt.destroy();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void assertData(EventBean theEvent, double price, Double prevPrice, Double priorPrice) {
        assertEquals(price, theEvent.get("price"));
        assertEquals(prevPrice, theEvent.get("prevPrice"));
        assertEquals(priorPrice, theEvent.get("priorPrice"));
    }

    private void assertData(EventBean theEvent, Double sumPrice) {
        assertEquals(sumPrice, theEvent.get("sumPrice"));
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
