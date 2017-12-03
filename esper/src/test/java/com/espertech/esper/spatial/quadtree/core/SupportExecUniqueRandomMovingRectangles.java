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
import com.espertech.esper.type.XYPoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.*;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertTrue;

public class SupportExecUniqueRandomMovingRectangles {

    public static <L> void runAssertion(SupportQuadTreeToolUnique<L> tools,
                                        double rectangleWidth,
                                        double rectangleHeight) {

        assertNull(tools.generator);

        SupportQuadTreeConfig[] configs = new SupportQuadTreeConfig[] {
                new SupportQuadTreeConfig(0, 0, 100, 100, 4, 20),
                new SupportQuadTreeConfig(0, 0, 100, 100, 100, 20),
                new SupportQuadTreeConfig(0, 0, 100, 100, 4, 100),
                new SupportQuadTreeConfig(10, 8000, 90, 2000, 4, 100)
        };

        for (SupportQuadTreeConfig config : configs) {
            runAssertion(1000, 1000, 5, config, tools, rectangleWidth, rectangleHeight);
            runAssertion(1000, 1000, 1, config, tools, rectangleWidth, rectangleHeight);
        }
    }

    private static <L> void runAssertion(int numPoints, int numMoves, int queryFrameSize, SupportQuadTreeConfig config,
                                         SupportQuadTreeToolUnique<L> tools,
                                         double rectangleWidth, double rectangleHeight) {
        Random random = new Random();
        L quadTree = tools.factory.make(config);

        // generate
        Map<XYPoint, SupportRectangleWithId> points = generateIntegerCoordinates(random, numPoints, config, rectangleWidth, rectangleHeight);

        // add
        for (SupportRectangleWithId point : points.values()) {
            tools.adderUnique.addOrSet(quadTree, point);
        }

        // move points
        for (int i = 0; i < numMoves; i++) {
            XYPoint p = movePoint(points, quadTree, random, config, tools.adderUnique, tools.remover);

            double qx = p.getX() - queryFrameSize;
            double qy = p.getY() - queryFrameSize;
            double qwidth = queryFrameSize * 2;
            double qheight = queryFrameSize * 2;
            Collection<Object> values = tools.querier.query(quadTree, qx, qy, qwidth, qheight);
            assertIds(points.values(), values, qx, qy, qwidth, qheight, tools.pointInsideChecking);
        }
    }

    private static <L> XYPoint movePoint(Map<XYPoint, SupportRectangleWithId> points, L quadTree, Random random, SupportQuadTreeConfig config, AdderUnique<L> adder, Remover<L> remover) {

        XYPoint[] coordinates = points.keySet().toArray(new XYPoint[points.size()]);
        XYPoint oldCoordinate;
        XYPoint newCoordinate;
        while(true) {

            oldCoordinate = coordinates[random.nextInt(coordinates.length)];
            int direction = random.nextInt(4);
            double newX = oldCoordinate.getX();
            double newY = oldCoordinate.getY();
            if (direction == 0 && newX > config.getX()) {
                newX--;
            }
            if (direction == 1 && newY > config.getY()) {
                newY--;
            }
            if (direction == 2 && newX < (config.getX() + config.getWidth() - 1)) {
                newX++;
            }
            if (direction == 3 && newY < (config.getY() + config.getHeight() - 1)) {
                newY++;
            }

            newCoordinate = new XYPoint(newX, newY);
            if (!points.containsKey(newCoordinate)) {
                break;
            }
        }

        SupportRectangleWithId moved = points.remove(oldCoordinate);
        remover.removeOrDelete(quadTree, moved);
        moved.setX(newCoordinate.getX());
        moved.setY(newCoordinate.getY());
        adder.addOrSet(quadTree, moved);
        points.put(newCoordinate, moved);

        // Comment-me-in:
        // log.info("Moving " + moved.getId() + " from " + printPoint(oldCoordinate.getX(), oldCoordinate.getY()) + " to " + printPoint(newCoordinate.getX(), newCoordinate.getY()));

        return newCoordinate;
    }

    private static Map<XYPoint, SupportRectangleWithId> generateIntegerCoordinates(Random random, int numPoints, SupportQuadTreeConfig config, double rectangleWidth, double rectangleHeight) {
        Map<XYPoint, SupportRectangleWithId> result = new HashMap<>();
        int pointNum = 0;
        while(result.size() < numPoints) {
            int x = ((int) config.getX()) + random.nextInt((int) config.getWidth());
            int y = ((int) config.getY()) + random.nextInt((int) config.getHeight());
            XYPoint p = new XYPoint(x, y);
            if (result.containsKey(p)) {
                continue;
            }
            result.put(p, new SupportRectangleWithId("P" + pointNum, x, y, rectangleWidth, rectangleHeight));
            pointNum++;
        }
        return result;
    }
}
