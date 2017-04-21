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

import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TestQuadTreeRandomIntPointsInSquareUnique extends TestCase {
    public void testRandomIntPoints() {
        runAssertionPointsUnique(1000, null, null);
        runAssertionPointsUnique(1000, 1000, 20);
        runAssertionPointsUnique(1000, 2, 50);
    }

    private void runAssertionPointsUnique(int size, Integer leafCapacity, Integer maxTreeHeight) {
        Random random = new Random();
        Set<SupportPoint> points = generateCoordinates(random, size);
        QuadTree<Object> quadTree;
        if (leafCapacity == null && maxTreeHeight == null) {
            quadTree = QuadTreeFactory.make(0, 0, size, size);
        }
        else {
            quadTree = QuadTreeFactory.make(0, 0, size, size, leafCapacity, maxTreeHeight);
        }

        // add
        int count = 0;
        for (SupportPoint p : points) {
            boolean r = SupportQuadTreeUtil.addUnique(quadTree, p.getX(), p.getY(), "P" + count);
            assertTrue("Failed to add " + p.toString(), r);
            count++;
        }

        // find all individually
        for (SupportPoint p : points) {
            Collection<Object> values = QuadTreeToolQuery.queryRange(quadTree, p.getX(), p.getY(), 1, 1);
            assertTrue("Failed to find " + p.toString(), values != null && !values.isEmpty());
        }

        // get all content
        Collection<Object> all = QuadTreeToolQuery.queryRange(quadTree, 0, 0, size, size);
        assertEquals(points.size(), all.size());
        assertEquals(points.size(), new HashSet<>(all).size());
        for (Object value : all) {
            assertTrue(value instanceof String);
        }

        // remove all
        count = 0;
        for (SupportPoint p : points) {
            SupportQuadTreeUtil.remove(quadTree, p.getX(), p.getY(), "P" + count);
            count++;
        }

        assertNull(QuadTreeToolQuery.queryRange(quadTree, 0, 0, size, size));
    }

    private static Set<SupportPoint> generateCoordinates(Random random, int size) {
        Set<SupportPoint> set = new HashSet<>();
        while (set.size() < size) {
            float x = random.nextInt(size);
            float y = random.nextInt(size);
            set.add(new SupportPoint(x, y));
        }
        return set;
    }
}
