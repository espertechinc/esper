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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;

import java.util.HashMap;

public class TestTicksPerSecondStatement extends TestCase {

    private EPRuntime runtime;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration configuration = new Configuration();
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCommon().addEventType("MarketDataEvent", MarketDataEvent.class.getName());

        runtime = EPRuntimeProvider.getRuntime("TestTicksPerSecondStatement", configuration);
        runtime.initialize();

        listener = new SupportUpdateListener();
        TicksPerSecondStatement stmt = new TicksPerSecondStatement(runtime.getDeploymentService(), configuration);
        stmt.addListener(listener);
    }

    public void testFlow() {
        runtime.getEventService().advanceTime(1000); // Set the start time to 1 second

        sendEvent(new MarketDataEvent("CSC", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("IBM", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("GE", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("MS", FeedEnum.FEED_B));
        assertFalse(listener.isInvoked());

        runtime.getEventService().advanceTime(1500); // Now events arriving around 1.5 sec
        sendEvent(new MarketDataEvent("TEL", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("CSC", FeedEnum.FEED_B));
        assertFalse(listener.isInvoked());

        runtime.getEventService().advanceTime(2000); // Now events arriving around 2 sec
        sendEvent(new MarketDataEvent("TEL", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("IBM", FeedEnum.FEED_B));
        sendEvent(new MarketDataEvent("GE", FeedEnum.FEED_B));
        sendEvent(new MarketDataEvent("IOU", FeedEnum.FEED_B));
        assertCounts(4, 2);

        runtime.getEventService().advanceTime(2500); // Now events arriving around 2.5 sec
        sendEvent(new MarketDataEvent("TEL", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("GE", FeedEnum.FEED_B));
        sendEvent(new MarketDataEvent("MS", FeedEnum.FEED_B));
        assertFalse(listener.isInvoked());

        runtime.getEventService().advanceTime(3000);
        assertCounts(2, 5);

        runtime.getEventService().advanceTime(3500);
        sendEvent(new MarketDataEvent("TEL", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("IBM", FeedEnum.FEED_A));
        sendEvent(new MarketDataEvent("UUS", FeedEnum.FEED_A));
        assertFalse(listener.isInvoked());

        runtime.getEventService().advanceTime(4000);
        sendEvent(new MarketDataEvent("NBOT", FeedEnum.FEED_B));
        sendEvent(new MarketDataEvent("YAH", FeedEnum.FEED_B));
        assertCounts(3, 0);

        runtime.getEventService().advanceTime(4500);
        assertFalse(listener.isInvoked());

        runtime.getEventService().advanceTime(5000);
        assertCounts(0, 2);
    }

    private void assertCounts(long countFeedA, long countFeedB) {
        HashMap<FeedEnum, Long> countPerFeed = new HashMap<FeedEnum, Long>();
        countPerFeed.put((FeedEnum) listener.getLastNewData()[0].get("feed"), (Long) listener.getLastNewData()[0].get("cnt"));
        countPerFeed.put((FeedEnum) listener.getLastNewData()[1].get("feed"), (Long) listener.getLastNewData()[1].get("cnt"));
        assertEquals(2, listener.getLastNewData().length);
        listener.reset();

        assertEquals(countFeedA, (long) countPerFeed.get(FeedEnum.FEED_A)); // casting to long to avoid JUnit ambiguous assert
        assertEquals(countFeedB, (long) countPerFeed.get(FeedEnum.FEED_B));
    }

    private void sendEvent(Object theEvent) {
        runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
    }
}


