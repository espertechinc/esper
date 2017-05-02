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

import java.util.*;

import static junit.framework.TestCase.*;

public class SupportRandomIntPointsInSquareUnique {

    public static void testRandomIntPoints(SupportQuadTreeUtil.AdderUnique adder, SupportQuadTreeUtil.Remover remover, SupportQuadTreeUtil.Querier querier) {
        runAssertionPointsUnique(1000, null, null, adder, remover, querier);
        runAssertionPointsUnique(1000, 1000, 20, adder, remover, querier);
        runAssertionPointsUnique(1000, 2, 50, adder, remover, querier);
    }

    private static void runAssertionPointsUnique(int size, Integer leafCapacity, Integer maxTreeHeight, SupportQuadTreeUtil.AdderUnique adder, SupportQuadTreeUtil.Remover remover, SupportQuadTreeUtil.Querier querier) {
        Random random = new Random();
        List<SupportPointWithId> points = generateCoordinates(random, size);
        QuadTree<Object> quadTree;
        if (leafCapacity == null && maxTreeHeight == null) {
            quadTree = QuadTreeFactory.make(0, 0, size, size);
        }
        else {
            quadTree = QuadTreeFactory.make(0, 0, size, size, leafCapacity, maxTreeHeight);
        }

        // add
        for (SupportPointWithId p : points) {
            adder.addOrSet(quadTree, p);
        }

        // find all individually
        for (SupportPointWithId p : points) {
            Collection<Object> values = querier.query(quadTree, p.getX(), p.getY(), 1, 1);
            assertTrue("Failed to find " + p.toString(), values != null && !values.isEmpty());
            assertEquals(1, values.size());
            assertEquals(p.getId(), values.iterator().next());
        }

        // get all content
        Collection<Object> all = querier.query(quadTree, 0, 0, size, size);
        assertEquals(points.size(), all.size());
        assertEquals(points.size(), new HashSet<>(all).size());
        for (Object value : all) {
            assertTrue(value instanceof String);
        }

        // remove all
        for (SupportPointWithId p : points) {
            remover.removeOrDelete(quadTree, p);
        }

        Collection<Object> values = querier.query(quadTree, 0, 0, size, size);
        assertNull(values);
    }

    private static List<SupportPointWithId> generateCoordinates(Random random, int size) {
        Map<XYPoint, SupportPointWithId> points = new HashMap<>();
        int pointNum = 0;
        while (points.size() < size) {
            float x = random.nextInt(size);
            float y = random.nextInt(size);
            XYPoint p = new XYPoint(x, y);
            if (points.containsKey(p)) {
                continue;
            }
            points.put(p, new SupportPointWithId("P" + pointNum, x, y));
            pointNum++;
        }
        return new LinkedList<>(points.values());
    }

}
