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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.regression.spatial.ExecSpatialPointRegionQuadTreeEventIndex;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialPoint;

import java.util.*;

import static org.junit.Assert.*;

public class SupportSpatialUtil {
    public static void assertRectanglesSingleValue(EPServiceProvider epService, SupportUpdateListener listener, List<BoundingBox> rectangles, String... matches) {
        for (int i = 0; i < rectangles.size(); i++) {
            BoundingBox box = rectangles.get(i);
            sendRectangle(epService, "R" + box.toString(), box.getMinX(), box.getMinY(), box.getMaxX() - box.getMinX(), box.getMaxY() - box.getMinY());
            String c0 = listener.assertOneGetNewAndReset().get("c0").toString();
            assertEquals("for box " + i, matches[i], c0);
        }
    }

    public static void assertRectanglesManyRow(EPServiceProvider epService, SupportUpdateListener listener, List<BoundingBox> rectangles, String... matches) {
        for (int i = 0; i < rectangles.size(); i++) {
            BoundingBox box = rectangles.get(i);
            sendRectangle(epService, "R" + box.toString(), box.getMinX(), box.getMinY(), box.getMaxX() - box.getMinX(), box.getMaxY() - box.getMinY());
            if (matches[i] == null) {
                if (listener.isInvoked()) {
                    fail("Unexpected output for box " + i + ": " + sortJoinProperty(listener.getAndResetLastNewData(), "c0"));
                }
            } else {
                if (!listener.isInvoked()) {
                    fail("No output for box " + i);
                }
                assertEquals(matches[i], sortJoinProperty(listener.getAndResetLastNewData(), "c0"));
            }
        }
    }

    public static void sendPoint(EPServiceProvider epService, String id, double x, double y) {
        epService.getEPRuntime().sendEvent(new SupportSpatialPoint(id, x, y));
    }

    public static void sendPoint(EPServiceProvider epService, String id, double x, double y, String category) {
        epService.getEPRuntime().sendEvent(new SupportSpatialPoint(id, x, y, category));
    }

    public static void sendRectangle(EPServiceProvider epService, String id, double x, double y, double width, double height) {
        epService.getEPRuntime().sendEvent(new SupportSpatialAABB(id, x, y, width, height));
    }

    public static void sendAssert(EPServiceProvider epService, SupportUpdateListener listener, double px, double py, double x, double y, double width, double height, boolean expected) {
        sendAssertWNull(epService, listener, px, py, x, y, width, height, expected);
    }

    public static void sendAssertWNull(EPServiceProvider epService, SupportUpdateListener listener, Double px, Double py, Double x, Double y, Double width, Double height, Boolean expected) {
        epService.getEPRuntime().sendEvent(new ExecSpatialPointRegionQuadTreeEventIndex.MyEventRectangleWithOffset("E", px, py, x, y, width, height));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    public static String sortJoinProperty(EventBean[] events, String propertyName) {
        TreeMap<Integer, String> sorted = new TreeMap<>();
        for (EventBean event : events) {
            String value = event.get(propertyName).toString();
            int num = Integer.parseInt(value.substring(1));
            sorted.put(num, value);
        }
        StringJoiner joiner = new StringJoiner(",");
        for (String data : sorted.values()) {
            joiner.add(data);
        }
        return joiner.toString();
    }

    public static void sendSpatialPoints(EPServiceProvider epService, int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialPoint("P_" + x + "_" + y, (double) x, (double) y));
            }
        }
    }

    public static Object[][] getExpected(List<SupportSpatialPoint> points, double x, double y, double width, double height) {
        Set<String> expected = new TreeSet<>();
        BoundingBox boundingBox = new BoundingBox(x, y, x + width, y + height);
        for (SupportSpatialPoint p : points) {
            if (boundingBox.containsPoint(p.getPx(), p.getPy())) {
                if (expected.contains(p.getId())) {
                    fail();
                }
                expected.add(p.getId());
            }
        }
        Object[][] rows = new Object[expected.size()][];
        int index = 0;
        for (String id : expected) {
            rows[index++] = new Object[]{id};
        }
        return rows;
    }

    public static void sendAssertSpatialAABB(EPServiceProvider epService, SupportUpdateListener listener, int numX, int numY, long deltaMSec) {
        long start = System.currentTimeMillis();
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialAABB("", x, y, 0.1, 0.1));
                listener.assertOneGetNewAndReset();
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("Delta: " + delta, delta < deltaMSec);
    }
}
