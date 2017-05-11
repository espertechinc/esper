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

import java.util.Collection;
import java.util.List;
import java.util.Random;

import static com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.assertIds;
import static junit.framework.TestCase.assertTrue;

public class SupportExecNonUniqueRandomMovingRectangles {
    public static <L> void runAssertion(SupportQuadTreeToolNonUnique<L> tools) {

        SupportQuadTreeConfig[] configs = new SupportQuadTreeConfig[] {
            new SupportQuadTreeConfig(0, 0, 100, 100, 4, 20),
            new SupportQuadTreeConfig(0, 0, 10, 10, 4, 20),
            new SupportQuadTreeConfig(0, 0, 10, 10, 100, 20),
            new SupportQuadTreeConfig(0, 0, 10, 10, 4, 100),
        };

        for (SupportQuadTreeConfig config : configs) {
            runAssertion(1000, 2000, 5, config, tools);
            runAssertion(2, 1000, 1, config, tools);
            runAssertion(1000, 1000, 1, config, tools);
        }

        // test performance
        long start = System.currentTimeMillis();
        runAssertion(1000, 1000, 10, new SupportQuadTreeConfig(0, 0, 100, 100, 4, 20), tools);
        long delta = System.currentTimeMillis() - start;
        assertTrue("Time taken: " + delta, delta < 1000); // rough and leaves room for GC etc, just a performance smoke test
    }

    private static <L> void runAssertion(int numPoints, int numMoves, int queryFrameSize, SupportQuadTreeConfig config, SupportQuadTreeToolNonUnique<L> tools) {
        Random random = new Random();
        L quadTree = tools.factory.make(config);
        List<SupportRectangleWithId> points = tools.generator.generate(random, numPoints, config.getX(), config.getY(), config.getWidth(), config.getHeight());

        for (SupportRectangleWithId point : points) {
            tools.adderNonUnique.add(quadTree, point);
        }

        for (int i = 0; i < numMoves; i++) {
            SupportRectangleWithId moved = points.get(random.nextInt(points.size()));
            move(moved, quadTree, random, config, tools);

            double startX = moved.getX() - queryFrameSize;
            double startY = moved.getY() - queryFrameSize;
            double widthQ = queryFrameSize * 2;
            double heightQ = queryFrameSize * 2;
            Collection<Object> values = tools.querier.query(quadTree, startX, startY, widthQ, heightQ);
            assertIds(points, values, startX, startY, widthQ, heightQ, tools.pointInsideChecking);
        }
    }

    private static <L> void move(SupportRectangleWithId rectangle, L quadTree, Random random, SupportQuadTreeConfig config, SupportQuadTreeToolNonUnique<L> tools) {
        tools.remover.removeOrDelete(quadTree, rectangle);

        double newX;
        double newY;
        while(true) {
            int direction = random.nextInt(4);
            newX = rectangle.getX();
            newY = rectangle.getY();
            if (direction == 0) {
                newX--;
            }
            else if (direction == 1) {
                newY--;
            }
            else if (direction == 2) {
                newX++;
            }
            else if (direction == 3) {
                newY++;
            }

            if (tools.pointInsideChecking) {
                if (BoundingBox.containsPoint(config.getX(), config.getY(), config.getWidth(), config.getHeight(), newX, newY)) {
                    break;
                }
            }
            else {
                if (BoundingBox.intersectsBoxIncludingEnd(config.getX(), config.getY(), config.getMaxX(), config.getMaxY(), newX, newY, rectangle.getW(), rectangle.getH())) {
                    break;
                }
            }
        }

        // Comment-me-in:
        // log.info("Moving " + point.getId() + " from " + printPoint(point.getX(), point.getY()) + " to " + printPoint(newX, newY));

        rectangle.setX(newX);
        rectangle.setY(newY);
        tools.adderNonUnique.add(quadTree, rectangle);
    }
}
