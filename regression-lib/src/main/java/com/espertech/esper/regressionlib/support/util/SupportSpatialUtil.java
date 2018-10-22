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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.support.bean.SupportEventRectangleWithOffset;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialAABB;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialEventRectangle;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialPoint;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SupportSpatialUtil {
    private static final Logger log = LoggerFactory.getLogger(SupportSpatialUtil.class);

    public static List<SupportSpatialEventRectangle> randomRectangles(Random random, int numPoints, double x, double y, double width, double height) {
        List<SupportSpatialEventRectangle> rectangles = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double rx = random.nextDouble() * width + x;
            double ry = random.nextDouble() * height + y;
            double rw = random.nextDouble() * width * 0.3;
            double rh = random.nextDouble() * height * 0.3;
            rectangles.add(new SupportSpatialEventRectangle("R" + i, rx, ry, rw, rh));
        }
        return rectangles;
    }


    public static void assertAllRectangles(RegressionEnvironment env, Collection<SupportSpatialEventRectangle> expected, double x, double y, double width, double height) {
        env.sendEventBean(new SupportSpatialAABB("", x, y, width, height));
        EventBean[] events = env.listener("out").getAndResetLastNewData();
        if (events == null || events.length == 0) {
            TestCase.assertTrue(expected.isEmpty());
            return;
        }
        assertEquals(expected.size(), events.length);
        Set<String> received = new HashSet<>();
        for (EventBean event : events) {
            received.add(event.get("c0").toString());
        }
        assertEquals(expected.size(), received.size());
        for (SupportSpatialEventRectangle r : expected) {
            TestCase.assertTrue(received.contains(r.getId()));
        }
    }

    public static void assertBBTreeRectangles(RegressionEnvironment env, BoundingBox.BoundingBoxNode bbtree, List<SupportSpatialEventRectangle> rectangles) {
        assertBBRectangles(env, bbtree.bb, rectangles);
        if (bbtree.nw != null) {
            assertBBTreeRectangles(env, bbtree.nw, rectangles);
        }
        if (bbtree.ne != null) {
            assertBBTreeRectangles(env, bbtree.ne, rectangles);
        }
        if (bbtree.sw != null) {
            assertBBTreeRectangles(env, bbtree.sw, rectangles);
        }
        if (bbtree.se != null) {
            assertBBTreeRectangles(env, bbtree.se, rectangles);
        }
    }

    public static void addSendRectangle(RegressionEnvironment env, List<SupportSpatialEventRectangle> rectangles, String id, double x, double y, double width, double height) {
        SupportSpatialEventRectangle rectangle = new SupportSpatialEventRectangle(id, x, y, width, height);
        rectangles.add(rectangle);
        env.sendEventBean(rectangle);
    }

    public static List<SupportSpatialPoint> randomPoints(Random random, int numPoints, double x, double y, double width, double height) {
        List<SupportSpatialPoint> points = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double px = random.nextDouble() * width + x;
            double py = random.nextDouble() * height + y;
            points.add(new SupportSpatialPoint("P" + i, px, py));
        }
        return points;
    }

    public static void assertBBTreePoints(RegressionEnvironment env, BoundingBox.BoundingBoxNode bbtree, List<SupportSpatialPoint> points) {
        assertBBPoints(env, bbtree.bb, points);
        if (bbtree.nw != null) {
            assertBBTreePoints(env, bbtree.nw, points);
        }
        if (bbtree.ne != null) {
            assertBBTreePoints(env, bbtree.ne, points);
        }
        if (bbtree.sw != null) {
            assertBBTreePoints(env, bbtree.sw, points);
        }
        if (bbtree.se != null) {
            assertBBTreePoints(env, bbtree.se, points);
        }
    }

    public static void addSendPoint(RegressionEnvironment env, List<SupportSpatialPoint> points, String id, double x, double y) {
        SupportSpatialPoint point = new SupportSpatialPoint(id, x, y);
        points.add(point);
        env.sendEventBean(point);
    }

    public static void assertAllPoints(RegressionEnvironment env, Collection<SupportSpatialPoint> expected, double x, double y, double width, double height) {
        env.sendEventBean(new SupportSpatialAABB("", x, y, width, height));
        EventBean[] events = env.listener("out").getAndResetLastNewData();
        if (events == null || events.length == 0) {
            TestCase.assertTrue(expected.isEmpty());
            return;
        }
        assertEquals(expected.size(), events.length);
        Set<String> received = new HashSet<>();
        for (EventBean event : events) {
            received.add(event.get("c0").toString());
        }
        assertEquals(expected.size(), received.size());
        for (SupportSpatialPoint p : expected) {
            TestCase.assertTrue(received.contains(p.getId()));
        }
    }

    public static void sendAddPoint(RegressionEnvironment env, List<SupportSpatialPoint> points, String id, double x, double y) {
        SupportSpatialPoint point = new SupportSpatialPoint(id, x, y);
        points.add(point);
        env.sendEventBean(point);
    }

    public static void assertBBPoints(RegressionEnvironment env, BoundingBox bb, List<SupportSpatialPoint> points) {
        env.sendEventBean(new SupportSpatialAABB("", bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY()));
        String received = sortJoinProperty(env.listener("out").getAndResetLastNewData(), "c0");
        String expected = sortGetExpectedPoints(bb, points);
        assertEquals(expected, received);
    }

    public static String sortGetExpectedPoints(BoundingBox bb, List<SupportSpatialPoint> points) {
        StringJoiner joiner = new StringJoiner(",");
        for (SupportSpatialPoint point : points) {
            if (bb.containsPoint(point.getPx(), point.getPy())) {
                joiner.add(point.getId());
            }
        }
        return joiner.toString();
    }

    public static void sendAddRectangle(RegressionEnvironment env, List<SupportSpatialEventRectangle> rectangles, String id, double x, double y, double width, double height) {
        SupportSpatialEventRectangle rectangle = new SupportSpatialEventRectangle(id, x, y, width, height);
        rectangles.add(rectangle);
        env.sendEventBean(rectangle);
    }

    public static void assertBBRectangles(RegressionEnvironment env, BoundingBox bb, List<SupportSpatialEventRectangle> rectangles) {
        env.sendEventBean(new SupportSpatialAABB("", bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY()));
        String received = sortJoinProperty(env.listener("out").getAndResetLastNewData(), "c0");
        String expected = sortGetExpectedRectangles(bb, rectangles);
        if (!received.equals(expected)) {
            log.error("Expected: " + expected);
            log.error("Received: " + received);
        }
        assertEquals(expected, received);
    }

    public static void sendEventRectangle(RegressionEnvironment env, String id, double x, double y, double width, double height) {
        env.sendEventBean(new SupportSpatialEventRectangle(id, x, y, width, height));
    }

    public static void assertRectanglesSingleValue(RegressionEnvironment env, SupportListener listener, List<BoundingBox> rectangles, String... matches) {
        for (int i = 0; i < rectangles.size(); i++) {
            BoundingBox box = rectangles.get(i);
            sendRectangle(env, "R" + box.toString(), box.getMinX(), box.getMinY(), box.getMaxX() - box.getMinX(), box.getMaxY() - box.getMinY());
            String c0 = listener.assertOneGetNewAndReset().get("c0").toString();
            assertEquals("for box " + i, matches[i], c0);
        }
    }

    public static void assertRectanglesManyRow(RegressionEnvironment env, SupportListener listener, List<BoundingBox> rectangles, String... matches) {
        for (int i = 0; i < rectangles.size(); i++) {
            BoundingBox box = rectangles.get(i);
            sendRectangle(env, "R" + box.toString(), box.getMinX(), box.getMinY(), box.getMaxX() - box.getMinX(), box.getMaxY() - box.getMinY());
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

    public static void sendPoint(RegressionEnvironment env, String id, double x, double y) {
        env.sendEventBean(new SupportSpatialPoint(id, x, y));
    }

    public static void sendPoint(RegressionEnvironment env, String id, double x, double y, String category) {
        env.sendEventBean(new SupportSpatialPoint(id, x, y, category));
    }

    public static void sendRectangle(RegressionEnvironment env, String id, double x, double y, double width, double height) {
        env.sendEventBean(new SupportSpatialAABB(id, x, y, width, height));
    }

    public static void sendAssert(RegressionEnvironment env, SupportListener listener, double px, double py, double x, double y, double width, double height, boolean expected) {
        sendAssertWNull(env, listener, px, py, x, y, width, height, expected);
    }

    public static void sendAssertWNull(RegressionEnvironment env, SupportListener listener, Double px, Double py, Double x, Double y, Double width, Double height, Boolean expected) {
        env.sendEventBean(new SupportEventRectangleWithOffset("E", px, py, x, y, width, height));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    public static String sortJoinProperty(EventBean[] events, String propertyName) {
        TreeMap<Integer, String> sorted = new TreeMap<>();
        if (events != null) {
            for (EventBean event : events) {
                String value = event.get(propertyName).toString();
                int num = Integer.parseInt(value.substring(1));
                sorted.put(num, value);
            }
        }
        StringJoiner joiner = new StringJoiner(",");
        for (String data : sorted.values()) {
            joiner.add(data);
        }
        return joiner.toString();
    }

    public static void sendSpatialPoints(RegressionEnvironment env, int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                env.sendEventBean(new SupportSpatialPoint("P_" + x + "_" + y, (double) x, (double) y));
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

    public static void sendAssertSpatialAABB(RegressionEnvironment env, SupportListener listener, int numX, int numY, long deltaMSec) {
        long start = System.currentTimeMillis();
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                env.sendEventBean(new SupportSpatialAABB("", x, y, 0.1, 0.1));
                listener.assertOneGetNewAndReset();
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("Delta: " + delta, delta < deltaMSec);
    }

    public static String sortGetExpectedRectangles(BoundingBox bb, List<SupportSpatialEventRectangle> rectangles) {
        StringJoiner joiner = new StringJoiner(",");
        for (SupportSpatialEventRectangle rect : rectangles) {
            if (bb.intersectsBoxIncludingEnd(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight())) {
                joiner.add(rect.getId());
            }
        }
        return joiner.toString();
    }

    public static String buildDeleteQueryWithInClause(String infraName, String field, List<String> idList) {
        StringBuilder query = new StringBuilder();
        query.append("delete from ").append(infraName).append(" where ").append(field).append(" in (");
        String delimiter = "";
        for (String id : idList) {
            query.append(delimiter).append('\'').append(id).append("\'");
            delimiter = ",";
        }
        query.append(")");
        return query.toString();
    }
}
