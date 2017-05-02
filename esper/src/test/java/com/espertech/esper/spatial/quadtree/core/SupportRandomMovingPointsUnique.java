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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.assertPointsById;

public class SupportRandomMovingPointsUnique {

    public static void runAssertion(SupportQuadTreeUtil.AdderUnique adder,
                                    SupportQuadTreeUtil.Remover remover,
                                    SupportQuadTreeUtil.Querier querier) {

        runAssertion(1000, 5000, 5, 0, 0, 100, 100, 4, 20, adder, remover, querier);
        runAssertion(1000, 1000, 1, 0, 0, 100, 100, 4, 20, adder, remover, querier);
        runAssertion(1000, 1000, 1, 0, 0, 100, 100, 100, 20, adder, remover, querier);
        runAssertion(1000, 1000, 1, 0, 0, 100, 100, 4, 100, adder, remover, querier);
        runAssertion(1000, 1000, 1, 10, 8000, 90, 2000, 4, 100, adder, remover, querier);
    }

    private static void runAssertion(int numPoints, int numMoves, int queryFrameSize, int x, int y, int width, int height, int leafCapacity, int maxTreeHeight,
                              SupportQuadTreeUtil.AdderUnique adder, SupportQuadTreeUtil.Remover remover, SupportQuadTreeUtil.Querier querier) {
        Random random = new Random();
        QuadTree<Object> quadTree = QuadTreeFactory.make(x, y, width, height, leafCapacity, maxTreeHeight);

        // generate
        Map<XYPoint, SupportPointWithId> points = generateIntegerCoordinates(random, numPoints, x, y, width, height);

        // add
        for (SupportPointWithId point : points.values()) {
            adder.addOrSet(quadTree, point);
        }

        // move points
        for (int i = 0; i < numMoves; i++) {
            XYPoint p = movePoint(points, quadTree, random, x, y, width, height, adder, remover);

            double qx = p.getX() - queryFrameSize;
            double qy = p.getY() - queryFrameSize;
            double qwidth = queryFrameSize * 2;
            double qheight = queryFrameSize * 2;
            Collection<Object> values = querier.query(quadTree, qx, qy, qwidth, qheight);
            assertPointsById(points.values(), values, qx, qy, qwidth, qheight);
        }
    }

    private static XYPoint movePoint(Map<XYPoint, SupportPointWithId> points, QuadTree<Object> quadTree, Random random, int x, int y, int width, int height, SupportQuadTreeUtil.AdderUnique adder, SupportQuadTreeUtil.Remover remover) {

        XYPoint[] coordinates = points.keySet().toArray(new XYPoint[points.size()]);
        XYPoint oldCoordinate;
        XYPoint newCoordinate;
        while(true) {

            oldCoordinate = coordinates[random.nextInt(coordinates.length)];
            int direction = random.nextInt(4);
            double newX = oldCoordinate.getX();
            double newY = oldCoordinate.getY();
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

            newCoordinate = new XYPoint(newX, newY);
            if (!points.containsKey(newCoordinate)) {
                break;
            }
        }

        SupportPointWithId moved = points.remove(oldCoordinate);
        remover.removeOrDelete(quadTree, moved);
        moved.setX(newCoordinate.getX());
        moved.setY(newCoordinate.getY());
        adder.addOrSet(quadTree, moved);
        points.put(newCoordinate, moved);

        // Comment-me-in:
        // log.info("Moving " + moved.getId() + " from " + printPoint(oldCoordinate.getX(), oldCoordinate.getY()) + " to " + printPoint(newCoordinate.getX(), newCoordinate.getY()));

        return newCoordinate;
    }

    private static Map<XYPoint, SupportPointWithId> generateIntegerCoordinates(Random random, int numPoints, int startX, int startY, int width, int height) {
        Map<XYPoint, SupportPointWithId> result = new HashMap<>();
        int pointNum = 0;
        while(result.size() < numPoints) {
            int x = startX + random.nextInt(width);
            int y = startY + random.nextInt(height);
            XYPoint p = new XYPoint(x, y);
            if (result.containsKey(p)) {
                continue;
            }
            result.put(p, new SupportPointWithId("P" + pointNum, x, y));
            pointNum++;
        }
        return result;
    }
}
