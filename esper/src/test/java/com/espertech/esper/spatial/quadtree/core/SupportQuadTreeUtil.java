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
package com.espertech.esper.spatial.quadtree.core;

import com.espertech.esper.spatial.quadtree.mxcif.SupportRectangleWithId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SupportQuadTreeUtil {
    private static final Logger log = LoggerFactory.getLogger(SupportQuadTreeUtil.class);

    public interface Factory<L> {
        L make(SupportQuadTreeConfig config);
    }

    public interface AdderUnique<L> {
        void addOrSet(L tree, SupportRectangleWithId value);
    }

    public interface AdderNonUnique<L> {
        void add(L tree, SupportRectangleWithId value);
    }

    public interface Remover<L> {
        void removeOrDelete(L tree, SupportRectangleWithId value);
    }

    public interface Querier<L> {
        Collection<Object> query(L tree, double x, double y, double width, double height);
    }

    public interface Generator {
        boolean unique();
        List<SupportRectangleWithId> generate(Random random, int numPoints, double x, double y, double width, double height);
    }

    public static void assertIds(Collection<SupportRectangleWithId> rects, Collection<Object> received, double x, double y, double width, double height, boolean pointInsideChecking) {
        BoundingBox boundingBox = new BoundingBox(x, y, x+width, y+height);
        List<String> expected = new ArrayList<>();
        for (SupportRectangleWithId p : rects) {
            if (pointInsideChecking) {
                if (boundingBox.containsPoint(p.getX(), p.getY())) {
                    expected.add(p.getId());
                }
            }
            else {
                if (boundingBox.intersectsBoxIncludingEnd(p.getX(), p.getY(), p.getW(), p.getH())) {
                    expected.add(p.getId());
                }
            }
        }
        compare(received, expected);
    }

    private static void compare(Collection<Object> receivedObjects, List<String> expected) {
        if (expected.isEmpty()) {
            assertNull(receivedObjects);
            return;
        }
        if (receivedObjects == null) {
            fail("Did not receive expected " + expected);
        }
        List<String> received = new ArrayList<>();
        for (Object item : receivedObjects) {
            received.add(item.toString());
        }
        Collections.sort(received);
        Collections.sort(expected);
        String receivedText = received.toString();
        String expectedText = expected.toString();
        if (!expectedText.equals(receivedText)) {
            log.info("Expected:" + expectedText);
            log.info("Received:" + receivedText);
        }
        assertEquals(expectedText, receivedText);
    }

    public static <L> void randomQuery(L quadTree, List<SupportRectangleWithId> rectangles, Random random, double x, double y, double width, double height, SupportQuadTreeUtil.Querier<L> querier, boolean pointInsideChecking) {
        double bbWidth = random.nextDouble() * width * 1.5;
        double bbHeight = random.nextDouble() * height * 1.5;
        double bbMinX = random.nextDouble() * width + x * 0.8;
        double bbMinY = random.nextDouble() * height + y * 0.8;
        Collection<Object> actual = querier.query(quadTree, bbMinX, bbMinY, bbWidth, bbHeight);
        assertIds(rectangles, actual, bbMinX, bbMinY, bbWidth, bbHeight, pointInsideChecking);
    }
}
