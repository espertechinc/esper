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
package com.espertech.esper.example.transaction;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;

public class TestRealtimeSummaryStmt extends TestStmtBase {
    private SupportUpdateListener listenerTotals;
    private SupportUpdateListener listenerByCustomer;
    private SupportUpdateListener listenerBySupplier;

    public void setUp() {
        super.setUp();

        // Establish feed for combined events, which contains the latency values
        new CombinedEventStmt(epService.getEPAdministrator());

        // Establish listeners for testing
        listenerTotals = new SupportUpdateListener();
        listenerByCustomer = new SupportUpdateListener();
        listenerBySupplier = new SupportUpdateListener();
        RealtimeSummaryStmt realtimeStmt = new RealtimeSummaryStmt(epService.getEPAdministrator());
        realtimeStmt.addTotalsListener(listenerTotals);
        realtimeStmt.addByCustomerListener(listenerByCustomer);
        realtimeStmt.addBySupplierListener(listenerBySupplier);

        // Use external clocking for the test
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
    }

    public void testFlow() {
        sendEvent(new CurrentTimeEvent(1000)); // Set the time to 1 seconds

        sendEvent(new TxnEventA("id1", 1000, "c1"));
        sendEvent(new TxnEventB("id1", 2000));
        sendEvent(new TxnEventC("id1", 3500, "s1"));
        assertTotals(2500L, 2500L, 2500D, 1500L, 1500L, 1500D, 1000L, 1000L, 1000D);
        assertTotalsByCustomer("c1", 2500L, 2500L, 2500D);
        assertTotalsBySupplier("s1", 2500L, 2500L, 2500D);

        sendEvent(new CurrentTimeEvent(10000)); // Set the time to 10 seconds

        sendEvent(new TxnEventB("id2", 10000));
        sendEvent(new TxnEventC("id2", 10600, "s2"));
        sendEvent(new TxnEventA("id2", 11200, "c2"));
        assertTotals(-600L, 2500L, (2500 - 600) / 2.0D, 600L, 1500L, (1500 + 600) / 2.0D, -1200L, 1000L, (1000 - 1200) / 2.0);
        assertTotalsByCustomer("c2", -600L, -600L, -600D);
        assertTotalsBySupplier("s2", -600L, -600L, -600D);

        sendEvent(new CurrentTimeEvent(20000)); // Set the time to 20 seconds

        sendEvent(new TxnEventC("id3", 20000, "s1"));
        sendEvent(new TxnEventA("id3", 20100, "c1"));
        sendEvent(new TxnEventB("id3", 20200));
        assertTotals(-600L, 2500L, (2500 - 600 - 100) / 3.0D, -200L, 1500L, (1500 + 600 - 200) / 3.0, -1200L, 1000L, (1000 - 1200 + 100) / 3.0);
        assertTotalsByCustomer("c1", -100L, 2500L, (2500 - 100) / 2.0);
        assertTotalsBySupplier("s1", -100L, 2500L, (2500 - 100) / 2.0);

        // Set the time to 30 minutes and 5 seconds later expelling latencies for "id1"
        int seconds = 30 * 60 + 5;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        assertTotals(-600L, -100L, (-600 - 100) / 2.0, -200L, 600L, (600 - 200) / 2.0, -1200L, 100L, (-1200 + 100) / 2.0);
        assertTotalsByCustomer("c1", -100L, -100L, -100D);
        assertTotalsBySupplier("s1", -100L, -100L, -100D);

        // Set the time to 30 minutes and 10 seconds later expelling latencies for "id2"
        seconds = 30 * 60 + 10;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        assertTotals(-100L, -100L, -100D, -200L, -200L, -200D, 100L, 100L, 100D);
        assertTotalsByCustomer("c2", null, null, null);
        assertTotalsBySupplier("s2", null, null, null);

        // Set the time to 30 minutes and 20 seconds later expelling remaining latencies "id3"
        seconds = 30 * 60 + 20;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        assertTotals(null, null, null, null, null, null, null, null, null);
        assertTotalsByCustomer("c1", null, null, null);
        assertTotalsBySupplier("s1", null, null, null);

        // Send some more events crossing supplier and customer ids
        seconds = 30 * 60 + 30;
        sendEvent(new TxnEventA("id4", seconds * 1000, "cA"));
        sendEvent(new TxnEventB("id4", seconds * 1000 + 500));
        sendEvent(new TxnEventC("id4", seconds * 1000 + 1000, "sB"));
        assertTotalsByCustomer("cA", 1000L, 1000L, 1000D);
        assertTotalsBySupplier("sB", 1000L, 1000L, 1000D);

        seconds = 30 * 60 + 40;
        sendEvent(new TxnEventA("id5", seconds * 1000, "cB"));
        sendEvent(new TxnEventB("id5", seconds * 1000 + 1500));
        sendEvent(new TxnEventC("id5", seconds * 1000 + 2000, "sA"));
        assertTotalsByCustomer("cB", 2000L, 2000L, 2000D);
        assertTotalsBySupplier("sA", 2000L, 2000L, 2000D);

        seconds = 30 * 60 + 50;
        sendEvent(new TxnEventA("id6", seconds * 1000, "cA"));
        sendEvent(new TxnEventB("id6", seconds * 1000 + 2500));
        sendEvent(new TxnEventC("id6", seconds * 1000 + 3000, "sA"));
        assertTotalsByCustomer("cA", 1000L, 3000L, 2000D);
        assertTotalsBySupplier("sA", 2000L, 3000L, 2500D);
    }

    private void assertTotals(Long minAC, Long maxAC, Double avgAC,
                              Long minBC, Long maxBC, Double avgBC,
                              Long minAB, Long maxAB, Double avgAB) {
        assertEquals(1, listenerTotals.getNewDataList().size());
        assertEquals(1, listenerTotals.getLastNewData().length);
        EventBean theEvent = listenerTotals.getLastNewData()[0];
        assertEquals(minAC, theEvent.get("minLatencyAC"));
        assertEquals(maxAC, theEvent.get("maxLatencyAC"));
        assertEquals(avgAC, theEvent.get("avgLatencyAC"));
        assertEquals(minBC, theEvent.get("minLatencyBC"));
        assertEquals(maxBC, theEvent.get("maxLatencyBC"));
        assertEquals(avgBC, theEvent.get("avgLatencyBC"));
        assertEquals(minAB, theEvent.get("minLatencyAB"));
        assertEquals(maxAB, theEvent.get("maxLatencyAB"));
        assertEquals(avgAB, theEvent.get("avgLatencyAB"));
        listenerTotals.reset();
    }

    private void assertTotalsByCustomer(String customerId, Long minAC, Long maxAC, Double avgAC) {
        assertEquals(1, listenerByCustomer.getNewDataList().size());
        assertEquals(1, listenerByCustomer.getLastNewData().length);
        EventBean theEvent = listenerByCustomer.getLastNewData()[0];
        assertEquals(customerId, theEvent.get("customerId"));
        assertEquals(minAC, theEvent.get("minLatency"));
        assertEquals(maxAC, theEvent.get("maxLatency"));
        assertEquals(avgAC, theEvent.get("avgLatency"));
        listenerByCustomer.reset();
    }

    private void assertTotalsBySupplier(String supplierId, Long minAC, Long maxAC, Double avgAC) {
        assertEquals(1, listenerBySupplier.getNewDataList().size());
        assertEquals(1, listenerBySupplier.getLastNewData().length);
        EventBean theEvent = listenerBySupplier.getLastNewData()[0];
        assertEquals(supplierId, theEvent.get("supplierId"));
        assertEquals(minAC, theEvent.get("minLatency"));
        assertEquals(maxAC, theEvent.get("maxLatency"));
        assertEquals(avgAC, theEvent.get("avgLatency"));
        listenerBySupplier.reset();
    }
}
