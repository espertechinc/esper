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
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import junit.framework.TestCase;

public class TestAssetZoneMoveStmt extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = new Configuration();
        config.addEventType("LocationReport", LocationReport.class);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));

        listener = new SupportUpdateListener();
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testStmt() {
        LRMovingZoneStmt.createStmt(epService, 60, listener);

        sendTimer(0);   // time in seconds
        sendEvents(new String[]{"A1", "A2", "A3"}, new int[]{1, 1, 1});
        sendTimer(60);
        assertFalse(listener.isInvoked());

        sendTimer(70);
        sendEvent("A1", 2);
        sendTimer(80);
        sendEvent("A2", 2);
        sendTimer(90);
        sendEvent("A3", 2);
        sendTimer(100);
        sendTimer(180);
        assertFalse(listener.isInvoked());

        sendEvent("A1", 3);
        sendTimer(190);
        assertFalse(listener.isInvoked());
        sendTimer(239);
        assertFalse(listener.isInvoked());
        sendTimer(240);
        assertEquals(1, listener.getNewDataList().size());
        EventBean events[] = listener.getNewDataList().get(0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "Part.zone".split(","), new Object[][] {{2}, {3}});
        listener.reset();

        sendEvents(new String[]{"A2", "A3"}, new int[]{3, 3});
        sendTimer(300);
        assertFalse(listener.isInvoked());

        sendEvents(new String[]{"A2", "A3"}, new int[]{4, 4});
        sendTimer(350);
        sendEvents(new String[]{"A1"}, new int[]{4});
        sendTimer(360);
        assertFalse(listener.isInvoked());
    }

    private void sendEvents(String[] assetId, int[] zone) {
        assertEquals(assetId.length, zone.length);
        for (int i = 0; i < assetId.length; i++) {
            sendEvent(assetId[i], zone[i]);
        }
    }

    private void sendEvent(String assetId, int zone) {
        LocationReport report = new LocationReport(assetId, zone);
        epService.getEPRuntime().sendEvent(report);
    }

    private void sendTimer(long timeInSeconds) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInSeconds * 1000);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
