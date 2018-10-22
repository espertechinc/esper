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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;

public class TestAssetZoneMoveStmt extends TestCase {
    private EPRuntime runtime;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = new Configuration();
        config.getCommon().addEventType("LocationReport", LocationReport.class);

        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();
        runtime.getEventService().clockExternal();

        listener = new SupportUpdateListener();
    }

    public void tearDown() throws Exception {
        runtime.destroy();
    }

    public void testStmt() {
        LRMovingZoneStmt.createStmt(runtime, 60, listener);

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
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "Part.zone".split(","), new Object[][]{{2}, {3}});
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
        runtime.getEventService().sendEventBean(report, "LocationReport");
    }

    private void sendTimer(long timeInSeconds) {
        runtime.getEventService().advanceTime(timeInSeconds * 1000);
    }
}
