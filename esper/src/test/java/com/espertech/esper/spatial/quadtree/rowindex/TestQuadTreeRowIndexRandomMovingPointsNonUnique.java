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
package com.espertech.esper.spatial.quadtree.rowindex;

import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;
import com.espertech.esper.spatial.quadtree.core.SupportPointWithId;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.assertPointsById;
import static com.espertech.esper.spatial.quadtree.rowindex.SupportQuadTreeRowIndexUtil.addNonUnique;
import static com.espertech.esper.spatial.quadtree.rowindex.SupportQuadTreeRowIndexUtil.remove;

public class TestQuadTreeRowIndexRandomMovingPointsNonUnique extends TestCase {
    public void testNonUnique() {
        runAssertionNonUnique(1000, 5000, 5, 0, 0, 100, 100, 4, 20);
        runAssertionNonUnique(2, 1000, 1, 0, 0, 10, 10, 4, 20);
        runAssertionNonUnique(1000, 1000, 1, 0, 0, 10, 10, 4, 20);
        runAssertionNonUnique(1000, 1000, 1, 0, 0, 10, 10, 100, 20);
        runAssertionNonUnique(1000, 1000, 1, 0, 0, 10, 10, 4, 100);
    }

    public void testPerformance() {
        long start = System.currentTimeMillis();
        runAssertionNonUnique(1000, 1000, 10, 0, 0, 100, 100, 4, 20);
        long delta = System.currentTimeMillis() - start;
        assertTrue("Time taken: " + delta, delta < 1000); // rough and leaves room for GC etc, just a performance smoke test
    }

    private void runAssertionNonUnique(int numPoints, int numMoves, int queryFrameSize, int x, int y, int width, int height, int leafCapacity, int maxTreeHeight) {
        Random random = new Random();
        List<SupportPointWithId> points = generateCoordinates(random, numPoints, width, height);
        QuadTree<Object> quadTree = QuadTreeFactory.make(x, y, width, height, leafCapacity, maxTreeHeight);

        for (SupportPointWithId point : points) {
            addNonUnique(quadTree, point.getX(), point.getY(), point.getId());
        }

        for (int i = 0; i < numMoves; i++) {
            SupportPointWithId pointMoved = points.get(random.nextInt(points.size()));
            movePoint(pointMoved, quadTree, random, x, y, width, height);

            double startX = pointMoved.getX() - queryFrameSize;
            double startY = pointMoved.getY() - queryFrameSize;
            double widthQ = queryFrameSize * 2;
            double heightQ = queryFrameSize * 2;
            Collection<Object> values = QuadTreeRowIndexQuery.queryRange(quadTree, startX, startY, widthQ, heightQ);
            assertFalse(values.isEmpty());
            assertPointsById(points, values, startX, startY, widthQ, heightQ);
        }
    }

    private void movePoint(SupportPointWithId point, QuadTree<Object> quadTree, Random random, int x, int y, int width, int height) {
        int direction = random.nextInt(4);
        double newX = point.getX();
        double newY = point.getY();
        if (direction == 0 && newX > x) {
            newX--;
        }
        if (direction == 1 && newY > y) {
            newY--;
        }
        if (direction == 2 && newX < (x + width - 1)) {
            newX++;
        }
        if (direction == 3 && newY < (y + height - 1)) {
            newY++;
        }

        // Comment-me-in:
        // log.info("Moving " + point.getId() + " from " + printPoint(point.getX(), point.getY()) + " to " + printPoint(newX, newY));

        remove(quadTree, point.getX(), point.getY(), point.getId());
        point.setX(newX);
        point.setY(newY);
        addNonUnique(quadTree, point.getX(), point.getY(), point.getId());
    }

    private List<SupportPointWithId> generateCoordinates(Random random, int numPoints, int width, int height) {
        List<SupportPointWithId> result = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            result.add(new SupportPointWithId("P" + i, x, y));
        }
        return result;
    }
}
