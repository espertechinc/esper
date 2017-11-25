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
package com.espertech.esper.example.marketdatafeed;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import junit.framework.TestCase;

public class TestTicksFalloffStatement extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.addEventType("MarketDataEvent", MarketDataEvent.class.getName());

        epService = EPServiceProviderManager.getProvider("TestTicksPerSecondStatement", configuration);
        epService.initialize();
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        new TicksPerSecondStatement(epService.getEPAdministrator());
        TicksFalloffStatement stmt = new TicksFalloffStatement(epService.getEPAdministrator());
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Use external clocking for the test
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testFlow() {

        sendEvents(1000, 50, 150); // Set time to 1 second, send 100 feed A and 150 feed B
        sendEvents(1500, 50, 50);
        sendEvents(2000, 60, 130);
        sendEvents(2500, 40, 70);
        sendEvents(3000, 50, 150);
        sendEvents(3500, 50, 50);
        sendEvents(4000, 50, 150);
        sendEvents(4500, 50, 50);
        sendEvents(5000, 50, 150);
        sendEvents(5500, 50, 50);
        sendEvents(6000, 50, 24);
        assertFalse(listener.isInvoked());
        sendEvents(6500, 50, 50);
        sendEvents(7000, 50, 150);
        assertReceived(FeedEnum.FEED_B, (200 * 5 + 74) / 6, 74);
        sendEvents(7500, 50, 50);
        sendEvents(8000, 50, 150);
        sendEvents(8500, 50, 50);
        sendEvents(9000, 60, 150);
        sendEvents(9500, 40, 50);
        sendEvents(10000, 50, 150);
        sendEvents(10500, 70, 50);
        sendEvents(11000, 30, 150);
        sendEvents(11500, 50, 50);
        sendEvents(12000, 40, 150);
        assertFalse(listener.isInvoked());
        sendEvents(12500, 30, 150);
        sendEvents(13000, 50, 150);
        assertReceived(FeedEnum.FEED_A, (100 * 9 + 70) / 10, 70);
    }

    private void assertReceived(FeedEnum feedEnum, double average, long count) {
        assertTrue(listener.isInvoked());
        assertEquals(1, listener.getLastNewData().length);
        EventBean theEvent = listener.getLastNewData()[0];
        assertEquals(feedEnum, theEvent.get("feed"));
        assertEquals(average, theEvent.get("avgCnt"));
        assertEquals(count, theEvent.get("feedCnt"));
        listener.reset();
    }

    private void sendEvents(long timestamp, int numFeedA, int numFeedB) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(timestamp));
        send(FeedEnum.FEED_A, numFeedA);
        send(FeedEnum.FEED_B, numFeedB);
    }

    private void send(FeedEnum feedEnum, int numEvents) {
        for (int i = 0; i < numEvents; i++) {
            epService.getEPRuntime().sendEvent(new MarketDataEvent("CSC", feedEnum));
        }
    }

}
