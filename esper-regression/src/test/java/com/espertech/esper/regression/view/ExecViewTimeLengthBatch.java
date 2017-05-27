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
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecViewTimeLengthBatch implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        SupportMarketDataBean[] events = new SupportMarketDataBean[100];
        for (int i = 0; i < events.length; i++) {
            events[i] = new SupportMarketDataBean("S" + Integer.toString(i), "id_" + Integer.toString(i), i);
        }

        runAssertionTimeLengthBatch(epService, events);
        runAssertionTimeLengthBatchForceOutput(epService, events);
        runAssertionTimeLengthBatchForceOutputSum(epService, events);
        runAssertionForceOutputStartEagerSum(epService, events);
        runAssertionForceOutputStartNoEagerSum(epService);
        runAssertionPreviousAndPrior(epService, events);
        runAssertionGroupBySumStartEager(epService);
    }

    private void runAssertionTimeLengthBatch(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(10 sec, 3)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // Send 3 events in batch
        engine.sendEvent(events[0]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[1]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[2]);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send another 3 events in batch
        engine.sendEvent(events[3]);
        engine.sendEvent(events[4]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[5]);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, listener.getNewDataListFlattened());
        listener.reset();

        // Expire the last 3 events by moving time
        sendTimer(epService, startTime + 9999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 10000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 10001);
        assertFalse(listener.isInvoked());

        // Send an event, let the timer send the batch
        sendTimer(epService, startTime + 10100);
        engine.sendEvent(events[6]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 19999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 20000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 20001);
        assertFalse(listener.isInvoked());

        // Send two events, let the timer send the batch
        sendTimer(epService, startTime + 29998);
        engine.sendEvent(events[7]);
        engine.sendEvent(events[8]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 29999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 30000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send three events, the the 3 events batch
        sendTimer(epService, startTime + 30001);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[9]);
        engine.sendEvent(events[10]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[11]);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send 1 event, let the timer to do the batch
        sendTimer(epService, startTime + 39000 + 9999);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[12]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 10000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000 + 10001);
        assertFalse(listener.isInvoked());

        // Send no events, let the timer to do the batch
        sendTimer(epService, startTime + 39000 + 19999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 20000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000 + 20001);
        assertFalse(listener.isInvoked());

        // Send no events, let the timer to do NO batch
        sendTimer(epService, startTime + 39000 + 29999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 30000);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 30001);
        assertFalse(listener.isInvoked());

        // Send 1 more event
        sendTimer(epService, startTime + 90000);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[13]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 99999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 100000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[13]}, listener.getNewDataListFlattened());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionTimeLengthBatchForceOutput(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);

        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream * from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(10 sec, 3, 'FORCE_UPDATE')");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // Send 3 events in batch
        engine.sendEvent(events[0]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[1]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[2]);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send another 3 events in batch
        engine.sendEvent(events[3]);
        engine.sendEvent(events[4]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[5]);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, listener.getNewDataListFlattened());
        listener.reset();

        // Expire the last 3 events by moving time
        sendTimer(epService, startTime + 9999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 10000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 10001);
        assertFalse(listener.isInvoked());

        // Send an event, let the timer send the batch
        sendTimer(epService, startTime + 10100);
        engine.sendEvent(events[6]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 19999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 20000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 20001);
        assertFalse(listener.isInvoked());

        // Send two events, let the timer send the batch
        sendTimer(epService, startTime + 29998);
        engine.sendEvent(events[7]);
        engine.sendEvent(events[8]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 29999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 30000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send three events, the the 3 events batch
        sendTimer(epService, startTime + 30001);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[9]);
        engine.sendEvent(events[10]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[11]);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send 1 event, let the timer to do the batch
        sendTimer(epService, startTime + 39000 + 9999);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[12]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 10000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000 + 10001);
        assertFalse(listener.isInvoked());

        // Send no events, let the timer to do the batch
        sendTimer(epService, startTime + 39000 + 19999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 20000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000 + 20001);
        assertFalse(listener.isInvoked());

        // Send no events, let the timer do a batch
        sendTimer(epService, startTime + 39000 + 29999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 30000);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000 + 30001);
        assertFalse(listener.isInvoked());

        // Send no events, let the timer do a batch
        sendTimer(epService, startTime + 39000 + 39999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 39000 + 40000);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        sendTimer(epService, startTime + 39000 + 40001);
        assertFalse(listener.isInvoked());

        // Send 1 more event
        sendTimer(epService, startTime + 80000);
        assertFalse(listener.isInvoked());

        engine.sendEvent(events[13]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 88999);   // 10 sec from last batch
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 89000);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[13]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send 3 more events
        sendTimer(epService, startTime + 90000);
        engine.sendEvent(events[14]);
        engine.sendEvent(events[15]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 92000);
        engine.sendEvent(events[16]);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[13]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[14], events[15], events[16]}, listener.getNewDataListFlattened());
        listener.reset();

        // Send no events, let the timer do a batch
        sendTimer(epService, startTime + 101999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 102000);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[14], events[15], events[16]}, listener.getOldDataListFlattened());
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, listener.getNewDataListFlattened());
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionTimeLengthBatchForceOutputSum(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);

        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select sum(price) from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(10 sec, 3, 'FORCE_UPDATE')");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // Send 1 events in batch
        engine.sendEvent(events[10]);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 10000);
        assertEquals(10.0, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        sendTimer(epService, startTime + 20000);
        assertEquals(null, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        sendTimer(epService, startTime + 30000);
        assertEquals(null, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        sendTimer(epService, startTime + 40000);
        assertEquals(null, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionForceOutputStartEagerSum(EPServiceProvider epService, SupportMarketDataBean[] events) {
        final long startTime = 1000;
        sendTimer(epService, startTime);

        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select sum(price) from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(10 sec, 3, 'force_update, start_eager')");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 9999);
        assertFalse(listener.isInvoked());

        // Send batch off
        sendTimer(epService, startTime + 10000);
        assertEquals(null, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        // Send batch off
        sendTimer(epService, startTime + 20000);
        assertEquals(null, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        engine.sendEvent(events[11]);
        engine.sendEvent(events[12]);
        sendTimer(epService, startTime + 30000);
        assertEquals(23.0, listener.getLastNewData()[0].get("sum(price)"));
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionForceOutputStartNoEagerSum(EPServiceProvider epService) {
        final long startTime = 1000;
        sendTimer(epService, startTime);

        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select sum(price) from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(10 sec, 3, 'force_update')");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // No batch as we are not start eager
        sendTimer(epService, startTime + 10000);
        assertFalse(listener.isInvoked());

        // No batch as we are not start eager
        sendTimer(epService, startTime + 20000);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionPreviousAndPrior(EPServiceProvider epService, SupportMarketDataBean[] premades) {
        final long startTime = 1000;
        sendTimer(epService, startTime);
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select price, prev(1, price) as prevPrice, prior(1, price) as priorPrice from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(10 sec, 3)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPRuntime engine = epService.getEPRuntime();

        // Send 3 events in batch
        engine.sendEvent(premades[0]);
        engine.sendEvent(premades[1]);
        assertFalse(listener.isInvoked());

        engine.sendEvent(premades[2]);
        assertEquals(1, listener.getNewDataList().size());
        EventBean[] events = listener.getLastNewData();
        assertData(events[0], 0, null, null);
        assertData(events[1], 1.0, 0.0, 0.0);
        assertData(events[2], 2.0, 1.0, 1.0);
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionGroupBySumStartEager(EPServiceProvider epService) {
        final long startTime = 1000;
        sendTimer(epService, startTime);

        EPRuntime engine = epService.getEPRuntime();
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select symbol, sum(price) as s from " + SupportMarketDataBean.class.getName() +
                        "#time_length_batch(5, 10, \"START_EAGER\") group by symbol order by symbol asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, startTime + 4000);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 6000);
        assertEquals(1, listener.getNewDataList().size());
        EventBean[] events = listener.getLastNewData();
        assertNull(events);
        listener.reset();

        sendTimer(epService, startTime + 7000);
        engine.sendEvent(new SupportMarketDataBean("S1", "e1", 10d));

        sendTimer(epService, startTime + 8000);
        engine.sendEvent(new SupportMarketDataBean("S2", "e2", 77d));

        sendTimer(epService, startTime + 9000);
        engine.sendEvent(new SupportMarketDataBean("S1", "e3", 1d));

        sendTimer(epService, startTime + 10000);
        assertFalse(listener.isInvoked());

        sendTimer(epService, startTime + 11000);
        assertEquals(1, listener.getNewDataList().size());
        events = listener.getLastNewData();
        assertEquals(2, events.length);
        assertEquals("S1", events[0].get("symbol"));
        assertEquals(11d, events[0].get("s"));
        assertEquals("S2", events[1].get("symbol"));
        assertEquals(77d, events[1].get("s"));
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
}
