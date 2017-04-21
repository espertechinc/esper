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
package com.espertech.esper.spatial.quadtree.user;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class TestQuadTreeRandomDoublePointsWRandomQuery extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TestQuadTreeRandomDoublePointsWRandomQuery.class);

    public void testRun() {
        runAssertion(100, 100, 0, 0, 100, 100, 4, 20);
        runAssertion(100, 100, 50, 80, 20, 900, 4, 20);
        runAssertion(100, 100, 50, 80, 20, 900, 2, 80);
        runAssertion(1000, 100, 50, 800000, 2000, 900, 1000, 80);
    }

    private void runAssertion(int numPoints, int numQueries, double x, double y, double width, double height, int leafCapacity, int maxHeight) {
        QuadTree<Object> quadTree = QuadTreeFactory.make(x, y, width, height, leafCapacity, maxHeight);
        Random random = new Random();
        List<SupportPointWithId> points = randomPoints(random, numPoints, x, y, width, height);
        log.info("Loading " + points.size() + " points");
        for (SupportPointWithId point : points) {
            SupportQuadTreeUtil.addNonUnique(quadTree, point.getX(), point.getY(), point.getId());
        }

        log.info("Performing " + numQueries + " queries");
        for (int i = 0; i < numQueries; i++) {
            randomQuery(quadTree, random, points, x, y, width, height);
        }

        while(!points.isEmpty()) {
            SupportPointWithId remove = randomRemove(random, points);
            SupportQuadTreeUtil.remove(quadTree, remove.getX(), remove.getY(), remove.getId());
            // Comment-me-in log.info("Removed point, now at " + points.size() + " points and performing " + numQueries + " queries");

            for (int i = 0; i < numQueries; i++) {
                randomQuery(quadTree, random, points, x, y, width, height);
            }
        }
    }

    private SupportPointWithId randomRemove(Random random, List<SupportPointWithId> points) {
        int index = random.nextInt(points.size());
        return points.remove(index);
    }

    private void randomQuery(QuadTree<Object> quadTree, Random random, List<SupportPointWithId> points, double x, double y, double width, double height) {
        double bbWidth = random.nextDouble() * width * 1.5;
        double bbHeight = random.nextDouble() * height * 1.5;
        double bbMinX = random.nextDouble() * width + x * 0.8;
        double bbMinY = random.nextDouble() * height + y * 0.8;
        double bbMaxX = bbMinX + bbWidth;
        double bbMaxY = bbMinY + bbHeight;

        // get expected
        BoundingBox boundingBox = new BoundingBox(bbMinX, bbMinY, bbMaxX, bbMaxY);
        List<String> expected = new ArrayList<>();
        for (SupportPointWithId point : points) {
            if (boundingBox.containsPoint(point.getX(), point.getY())) {
                expected.add(point.getId());
            }
        }

        // get actual
        Collection<Object> actual = QuadTreeToolQuery.queryRange(quadTree, bbMinX, bbMinY, bbWidth, bbHeight);
        if (actual == null && expected.isEmpty()) {
            return;
        }
        EPAssertionUtil.assertEqualsAnyOrder(expected.toArray(), actual.toArray());
    }

    private List<SupportPointWithId> randomPoints(Random random, int numPoints, double x, double y, double width, double height) {
        List<SupportPointWithId> points = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double px = random.nextDouble() * width + x;
            double py = random.nextDouble() * height + y;
            points.add(new SupportPointWithId("P" + i, px, py));
        }
        return points;
    }
}
