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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecQuerytypeWTimeBatch implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MarketData", SupportMarketDataBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTimeBatchRowForAllNoJoin(epService);
        runAssertionTimeBatchRowForAllJoin(epService);
        runAssertionTimeBatchRowPerEventNoJoin(epService);
        runAssertionTimeBatchRowPerEventJoin(epService);
        runAssertionTimeBatchRowPerGroupNoJoin(epService);
        runAssertionTimeBatchRowPerGroupJoin(epService);
        runAssertionTimeBatchAggrGroupedNoJoin(epService);
        runAssertionTimeBatchAggrGroupedJoin(epService);
    }

    private void runAssertionTimeBatchRowForAllNoJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream sum(price) as sumPrice from MarketData#time_batch(1 sec)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // send first batch
        sendMDEvent(epService, "DELL", 10, 0L);
        sendMDEvent(epService, "IBM", 15, 0L);
        sendMDEvent(epService, "DELL", 20, 0L);
        sendTimer(epService, 1000);

        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], 45d);

        // send second batch
        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);

        newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], 20d);

        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(1, oldEvents.length);
        assertEvent(oldEvents[0], 45d);

        stmt.destroy();
    }

    private void runAssertionTimeBatchRowForAllJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream sum(price) as sumPrice from MarketData#time_batch(1 sec) as S0, SupportBean#keepall as S1 where S0.symbol = S1.theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportEvent(epService, "DELL");
        sendSupportEvent(epService, "IBM");

        // send first batch
        sendMDEvent(epService, "DELL", 10, 0L);
        sendMDEvent(epService, "IBM", 15, 0L);
        sendMDEvent(epService, "DELL", 20, 0L);
        sendTimer(epService, 1000);

        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], 45d);

        // send second batch
        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);

        newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], 20d);

        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(1, oldEvents.length);
        assertEvent(oldEvents[0], 45d);

        stmt.destroy();
    }

    private void runAssertionTimeBatchRowPerEventNoJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream symbol, sum(price) as sumPrice from MarketData#time_batch(1 sec)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // send first batch
        sendMDEvent(epService, "DELL", 10, 0L);
        sendMDEvent(epService, "IBM", 15, 0L);
        sendMDEvent(epService, "DELL", 20, 0L);
        sendTimer(epService, 1000);

        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(3, newEvents.length);
        assertEvent(newEvents[0], "DELL", 45d);
        assertEvent(newEvents[1], "IBM", 45d);
        assertEvent(newEvents[2], "DELL", 45d);

        // send second batch
        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);

        newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], "IBM", 20d);

        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(3, oldEvents.length);
        assertEvent(oldEvents[0], "DELL", 20d);
        assertEvent(oldEvents[1], "IBM", 20d);
        assertEvent(oldEvents[2], "DELL", 20d);

        stmt.destroy();
    }

    private void runAssertionTimeBatchRowPerEventJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream symbol, sum(price) as sumPrice from MarketData#time_batch(1 sec) as S0, SupportBean#keepall as S1 where S0.symbol = S1.theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportEvent(epService, "DELL");
        sendSupportEvent(epService, "IBM");

        // send first batch
        sendMDEvent(epService, "DELL", 10, 0L);
        sendMDEvent(epService, "IBM", 15, 0L);
        sendMDEvent(epService, "DELL", 20, 0L);
        sendTimer(epService, 1000);

        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(3, newEvents.length);
        assertEvent(newEvents[0], "DELL", 45d);
        assertEvent(newEvents[1], "IBM", 45d);
        assertEvent(newEvents[2], "DELL", 45d);

        // send second batch
        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);

        newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], "IBM", 20d);

        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(3, oldEvents.length);
        assertEvent(oldEvents[0], "DELL", 20d);
        assertEvent(oldEvents[1], "IBM", 20d);
        assertEvent(oldEvents[2], "DELL", 20d);

        stmt.destroy();
    }

    private void runAssertionTimeBatchRowPerGroupNoJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream symbol, sum(price) as sumPrice from MarketData#time_batch(1 sec) group by symbol order by symbol asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // send first batch
        sendMDEvent(epService, "DELL", 10, 0L);
        sendMDEvent(epService, "IBM", 15, 0L);
        sendMDEvent(epService, "DELL", 20, 0L);
        sendTimer(epService, 1000);

        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(2, newEvents.length);
        assertEvent(newEvents[0], "DELL", 30d);
        assertEvent(newEvents[1], "IBM", 15d);

        // send second batch
        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);

        newEvents = listener.getLastNewData();
        assertEquals(2, newEvents.length);
        assertEvent(newEvents[0], "DELL", null);
        assertEvent(newEvents[1], "IBM", 20d);

        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(2, oldEvents.length);
        assertEvent(oldEvents[0], "DELL", 30d);
        assertEvent(oldEvents[1], "IBM", 15d);

        stmt.destroy();
    }

    private void runAssertionTimeBatchRowPerGroupJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream symbol, sum(price) as sumPrice " +
                " from MarketData#time_batch(1 sec) as S0, SupportBean#keepall as S1" +
                " where S0.symbol = S1.theString " +
                " group by symbol";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportEvent(epService, "DELL");
        sendSupportEvent(epService, "IBM");

        // send first batch
        sendMDEvent(epService, "DELL", 10, 0L);
        sendMDEvent(epService, "IBM", 15, 0L);
        sendMDEvent(epService, "DELL", 20, 0L);
        sendTimer(epService, 1000);

        String[] fields = "symbol,sumPrice".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"DELL", 30d}, {"IBM", 15d}});

        // send second batch
        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastNewData(), fields, new Object[][]{{"DELL", null}, {"IBM", 20d}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastOldData(), fields, new Object[][]{{"DELL", 30d}, {"IBM", 15d}});

        stmt.destroy();
    }

    private void runAssertionTimeBatchAggrGroupedNoJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream symbol, sum(price) as sumPrice, volume from MarketData#time_batch(1 sec) group by symbol";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMDEvent(epService, "DELL", 10, 200L);
        sendMDEvent(epService, "IBM", 15, 500L);
        sendMDEvent(epService, "DELL", 20, 250L);

        sendTimer(epService, 1000);
        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(3, newEvents.length);
        assertEvent(newEvents[0], "DELL", 30d, 200L);
        assertEvent(newEvents[1], "IBM", 15d, 500L);
        assertEvent(newEvents[2], "DELL", 30d, 250L);

        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);
        newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], "IBM", 20d, 600L);
        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(3, oldEvents.length);
        assertEvent(oldEvents[0], "DELL", null, 200L);
        assertEvent(oldEvents[1], "IBM", 20d, 500L);
        assertEvent(oldEvents[2], "DELL", null, 250L);

        stmt.destroy();
    }

    private void runAssertionTimeBatchAggrGroupedJoin(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select irstream symbol, sum(price) as sumPrice, volume " +
                "from MarketData#time_batch(1 sec) as S0, SupportBean#keepall as S1" +
                " where S0.symbol = S1.theString " +
                " group by symbol";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportEvent(epService, "DELL");
        sendSupportEvent(epService, "IBM");

        sendMDEvent(epService, "DELL", 10, 200L);
        sendMDEvent(epService, "IBM", 15, 500L);
        sendMDEvent(epService, "DELL", 20, 250L);

        sendTimer(epService, 1000);
        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(3, newEvents.length);
        assertEvent(newEvents[0], "DELL", 30d, 200L);
        assertEvent(newEvents[1], "IBM", 15d, 500L);
        assertEvent(newEvents[2], "DELL", 30d, 250L);

        sendMDEvent(epService, "IBM", 20, 600L);
        sendTimer(epService, 2000);
        newEvents = listener.getLastNewData();
        assertEquals(1, newEvents.length);
        assertEvent(newEvents[0], "IBM", 20d, 600L);
        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(3, oldEvents.length);
        assertEvent(oldEvents[0], "DELL", null, 200L);
        assertEvent(oldEvents[1], "IBM", 20d, 500L);
        assertEvent(oldEvents[2], "DELL", null, 250L);

        stmt.destroy();
    }

    private void sendSupportEvent(EPServiceProvider epService, String theString) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, -1));
    }

    private void sendMDEvent(EPServiceProvider epService, String symbol, double price, Long volume) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean(symbol, price, volume, null));
    }

    private void assertEvent(EventBean theEvent, String symbol, Double sumPrice, Long volume) {
        assertEquals(symbol, theEvent.get("symbol"));
        assertEquals(sumPrice, theEvent.get("sumPrice"));
        assertEquals(volume, theEvent.get("volume"));
    }

    private void assertEvent(EventBean theEvent, String symbol, Double sumPrice) {
        assertEquals(symbol, theEvent.get("symbol"));
        assertEquals(sumPrice, theEvent.get("sumPrice"));
    }

    private void assertEvent(EventBean theEvent, Double sumPrice) {
        assertEquals(sumPrice, theEvent.get("sumPrice"));
    }

    private void sendTimer(EPServiceProvider epService, long time) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
