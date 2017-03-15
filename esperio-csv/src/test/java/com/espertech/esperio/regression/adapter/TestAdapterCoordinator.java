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
package com.espertech.esperio.regression.adapter;


import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esperio.csv.AdapterCoordinator;
import com.espertech.esperio.csv.AdapterCoordinatorImpl;
import com.espertech.esperio.csv.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.csv.CSVInputAdapterSpec;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestAdapterCoordinator extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TestAdapterCoordinator.class);

    private SupportUpdateListener listener;
    private String eventTypeName;
    private EPServiceProvider epService;
    private long currentTime;
    private AdapterCoordinator coordinator;
    private CSVInputAdapterSpec timestampsLooping;
    private CSVInputAdapterSpec noTimestampsLooping;
    private CSVInputAdapterSpec noTimestampsNotLooping;
    private CSVInputAdapterSpec timestampsNotLooping;
    private String[] propertyOrderNoTimestamp;

    protected void setUp() {
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        propertyTypes.put("myInt", Integer.class);
        propertyTypes.put("myDouble", Double.class);
        propertyTypes.put("myString", String.class);

        eventTypeName = "mapEvent";
        Configuration configuration = new Configuration();
        configuration.addEventType(eventTypeName, propertyTypes);

        epService = EPServiceProviderManager.getProvider("Adapter", configuration);
        epService.initialize();
        EPAdministrator administrator = epService.getEPAdministrator();
        String statementText = "select * from mapEvent#length(5)";
        EPStatement statement = administrator.createEPL(statementText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        // Turn off external clocking
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));

        // Set the clock to 0
        currentTime = 0;
        sendTimeEvent(0);

        coordinator = new AdapterCoordinatorImpl(epService, true);

        propertyOrderNoTimestamp = new String[]{"myInt", "myDouble", "myString"};
        String[] propertyOrderTimestamp = new String[]{"timestamp", "myInt", "myDouble", "myString"};

        // A CSVPlayer for a file with timestamps, not looping
        timestampsNotLooping = new CSVInputAdapterSpec(new AdapterInputSource("/regression/timestampOne.csv"), eventTypeName);
        timestampsNotLooping.setUsingEngineThread(true);
        timestampsNotLooping.setPropertyOrder(propertyOrderTimestamp);
        timestampsNotLooping.setTimestampColumn("timestamp");

        // A CSVAdapter for a file with timestamps, looping
        timestampsLooping = new CSVInputAdapterSpec(new AdapterInputSource("/regression/timestampTwo.csv"), eventTypeName);
        timestampsLooping.setLooping(true);
        timestampsLooping.setUsingEngineThread(true);
        timestampsLooping.setPropertyOrder(propertyOrderTimestamp);
        timestampsLooping.setTimestampColumn("timestamp");

        // A CSVAdapter that sends 10 events per sec, not looping
        noTimestampsNotLooping = new CSVInputAdapterSpec(new AdapterInputSource("/regression/noTimestampOne.csv"), eventTypeName);
        noTimestampsNotLooping.setEventsPerSec(10);
        noTimestampsNotLooping.setPropertyOrder(propertyOrderNoTimestamp);
        noTimestampsNotLooping.setUsingEngineThread(true);

        // A CSVAdapter that sends 5 events per sec, looping
        noTimestampsLooping = new CSVInputAdapterSpec(new AdapterInputSource("/regression/noTimestampTwo.csv"), eventTypeName);
        noTimestampsLooping.setEventsPerSec(5);
        noTimestampsLooping.setLooping(true);
        noTimestampsLooping.setPropertyOrder(propertyOrderNoTimestamp);
        noTimestampsLooping.setUsingEngineThread(true);
    }

    public void testRun() {
        coordinator.coordinate(new CSVInputAdapter(timestampsNotLooping));
        coordinator.coordinate(new CSVInputAdapter(timestampsLooping));
        coordinator.coordinate(new CSVInputAdapter(noTimestampsNotLooping));
        coordinator.coordinate(new CSVInputAdapter(noTimestampsLooping));

        // Time is 0
        assertFalse(listener.getAndClearIsInvoked());
        coordinator.start();

        // Time is 50
        sendTimeEvent(50);

        // Time is 100
        sendTimeEvent(50);
        assertEvent(0, 1, 1.1, "timestampOne.one");
        assertEvent(1, 1, 1.1, "noTimestampOne.one");
        assertSizeAndReset(2);

        // Time is 150
        sendTimeEvent(50);
        assertFalse(listener.getAndClearIsInvoked());

        // Time is 200
        sendTimeEvent(50);
        assertEvent(0, 2, 2.2, "timestampTwo.two");
        assertEvent(1, 2, 2.2, "noTimestampOne.two");
        assertEvent(2, 2, 2.2, "noTimestampTwo.two");
        assertSizeAndReset(3);

        // Time is 250
        sendTimeEvent(50);

        // Time is 300
        sendTimeEvent(50);
        assertEvent(0, 3, 3.3, "timestampOne.three");
        assertEvent(1, 3, 3.3, "noTimestampOne.three");
        assertSizeAndReset(2);

        // Time is 350
        sendTimeEvent(50);
        assertFalse(listener.getAndClearIsInvoked());

        coordinator.pause();

        // Time is 400
        sendTimeEvent(50);
        assertFalse(listener.getAndClearIsInvoked());

        // Time is 450
        sendTimeEvent(50);
        assertFalse(listener.getAndClearIsInvoked());

        coordinator.resume();

        assertEvent(0, 4, 4.4, "timestampTwo.four");
        assertEvent(1, 4, 4.4, "noTimestampTwo.four");
        assertSizeAndReset(2);

        // Time is 500
        sendTimeEvent(50);
        assertEvent(0, 5, 5.5, "timestampOne.five");
        assertSizeAndReset(1);

        // Time is 600
        sendTimeEvent(100);
        assertEvent(0, 6, 6.6, "timestampTwo.six");
        assertEvent(1, 2, 2.2, "noTimestampTwo.two");
        assertSizeAndReset(2);

        // Time is 800
        sendTimeEvent(200);
        assertEvent(0, 2, 2.2, "timestampTwo.two");
        assertEvent(1, 4, 4.4, "noTimestampTwo.four");
        assertSizeAndReset(2);

        coordinator.stop();
        sendTimeEvent(1000);
        assertFalse(listener.getAndClearIsInvoked());
    }

    public void testRunTillNull() {
        coordinator.coordinate(new CSVInputAdapter(epService, timestampsNotLooping));
        coordinator.start();

        // Time is 100
        sendTimeEvent(100);
        log.debug(".testRunTillNull time==100");
        assertEvent(0, 1, 1.1, "timestampOne.one");
        assertSizeAndReset(1);

        // Time is 300
        sendTimeEvent(200);
        log.debug(".testRunTillNull time==300");
        assertEvent(0, 3, 3.3, "timestampOne.three");
        assertSizeAndReset(1);

        // Time is 500
        sendTimeEvent(200);
        log.debug(".testRunTillNull time==500");
        assertEvent(0, 5, 5.5, "timestampOne.five");
        assertSizeAndReset(1);

        // Time is 600
        sendTimeEvent(100);
        log.debug(".testRunTillNull time==600");
        assertFalse(listener.getAndClearIsInvoked());

        // Time is 700
        sendTimeEvent(100);
        log.debug(".testRunTillNull time==700");
        assertFalse(listener.getAndClearIsInvoked());

        // Time is 800
        sendTimeEvent(100);
        log.debug(".testRunTillNull time==800");
    }

    public void testNotUsingEngineThread() {
        coordinator = new AdapterCoordinatorImpl(epService, false);
        coordinator.coordinate(new CSVInputAdapter(epService, noTimestampsNotLooping));
        coordinator.coordinate(new CSVInputAdapter(epService, timestampsNotLooping));

        long startTime = System.currentTimeMillis();
        coordinator.start();
        long endTime = System.currentTimeMillis();

        // The last event should be sent after 500 ms
        assertTrue(endTime - startTime > 500);

        assertEquals(6, listener.getNewDataList().size());
        assertEvent(0, 1, 1.1, "noTimestampOne.one");
        assertEvent(1, 1, 1.1, "timestampOne.one");
        assertEvent(2, 2, 2.2, "noTimestampOne.two");
        assertEvent(3, 3, 3.3, "noTimestampOne.three");
        assertEvent(4, 3, 3.3, "timestampOne.three");
        assertEvent(5, 5, 5.5, "timestampOne.five");
    }

    public void testExternalTimer() {
        coordinator = new AdapterCoordinatorImpl(epService, false, true, false);
        coordinator.coordinate(new CSVInputAdapter(epService, noTimestampsNotLooping));
        coordinator.coordinate(new CSVInputAdapter(epService, timestampsNotLooping));

        long startTime = System.currentTimeMillis();
        coordinator.start();
        long endTime = System.currentTimeMillis();

        // Check that we haven't been kept waiting
        assertTrue(endTime - startTime < 50);

        assertEquals(6, listener.getNewDataList().size());
        assertEvent(0, 1, 1.1, "noTimestampOne.one");
        assertEvent(1, 1, 1.1, "timestampOne.one");
        assertEvent(2, 2, 2.2, "noTimestampOne.two");
        assertEvent(3, 3, 3.3, "noTimestampOne.three");
        assertEvent(4, 3, 3.3, "timestampOne.three");
        assertEvent(5, 5, 5.5, "timestampOne.five");
    }

    private void assertEvent(int howManyBack, Integer myInt, Double myDouble, String myString) {
        assertTrue(listener.isInvoked());
        assertTrue(howManyBack < listener.getNewDataList().size());
        EventBean[] data = listener.getNewDataList().get(howManyBack);
        assertEquals(1, data.length);
        EventBean theEvent = data[0];
        assertEquals(myInt, theEvent.get("myInt"));
        assertEquals(myDouble, theEvent.get("myDouble"));
        assertEquals(myString, theEvent.get("myString"));
    }


    private void sendTimeEvent(int timeIncrement) {
        currentTime += timeIncrement;
        CurrentTimeEvent theEvent = new CurrentTimeEvent(currentTime);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void assertSizeAndReset(int size) {
        List<EventBean[]> list = listener.getNewDataList();
        assertEquals(size, list.size());
        list.clear();
        listener.getAndClearIsInvoked();
    }

}
