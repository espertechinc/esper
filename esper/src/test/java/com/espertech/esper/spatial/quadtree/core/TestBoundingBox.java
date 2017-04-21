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

import junit.framework.TestCase;

public class TestBoundingBox extends TestCase {
    public void testContainsPoint() {
        BoundingBox bb = new BoundingBox(10, 20, 40, 60);

        assertTrue(bb.containsPoint(10, 20));
        assertTrue(bb.containsPoint(39.9999, 59.9999));

        assertFalse(bb.containsPoint(40, 60));
        assertFalse(bb.containsPoint(10, 100));
        assertFalse(bb.containsPoint(100, 10));
    }

    public void testIntersectsBox() {
        // AWT Sample:
        // Rectangle r = new Rectangle(10, 20, 30, 40);
        // assertTrue(r.contains(39.9999, 59.999));
        // assertFalse(r.contains(40, 60));

        BoundingBox bb = new BoundingBox(10, 20, 40, 60);

        assertFalse(bb.intersectsBox(0, 0, 9.999, 19.999));
        assertTrue(bb.intersectsBox(0, 0, 10, 20));
        assertFalse(bb.intersectsBox(1, 2, 8.999, 17.999));
        assertTrue(bb.intersectsBox(1, 2, 9, 18));

        assertTrue(bb.intersectsBox(40, 60, 1, 2));
        assertFalse(bb.intersectsBox(40.0001, 60.0001, 1, 2));

        assertTrue(bb.intersectsBox(10, 20, 1, 1));
        assertTrue(bb.intersectsBox(5, 5, 60, 60));
    }

    public void testQuadrant() {
        BoundingBox bb = new BoundingBox(10, 20, 40, 60);

        double w = (bb.getMaxX() - bb.getMinX()) / 2d;
        double h = (bb.getMaxY() - bb.getMinY()) / 2d;

        BoundingBox bbNW = new BoundingBox(bb.getMinX(), bb.getMinY(), bb.getMinX() + w, bb.getMinY() + h);
        BoundingBox bbNE = new BoundingBox(bb.getMinX() + w, bb.getMinY(), bb.getMaxX(), bb.getMinY() + h);
        BoundingBox bbSW = new BoundingBox(bb.getMinX(), bb.getMinY() + h, bb.getMinX() + w, bb.getMaxY());
        BoundingBox bbSE = new BoundingBox(bb.getMinX() + w, bb.getMinY() + h, bb.getMaxX(), bb.getMaxY());

        runAssertion(10, 20, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NW);
        runAssertion(9, 19, bb, bbNW, bbNE, bbSW, bbSE, null);
        runAssertion(40, 60, bb, bbNW, bbNE, bbSW, bbSE, null);
        runAssertion(39.9999, 59.999999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SE);
        runAssertion(39.9999, 20, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NE);
        runAssertion(10, 59.999999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SW);
        runAssertion(24.9999, 39.9999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NW);
        runAssertion(25, 40, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SE);
        runAssertion(24.9999, 40, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SW);
        runAssertion(25, 39.9999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NE);
    }

    public void testTreeForDepth() {
        BoundingBox bb = new BoundingBox(0, 0, 100, 100);
        BoundingBox.BoundingBoxNode node = bb.treeForDepth(3);
        BoundingBox swNwNw = node.se.nw.nw.bb;
        assertTrue(swNwNw.equals(new BoundingBox(50, 50, 50+100/2/2/2.0, 50+100/2/2/2.0)));
    }

    public void testTreeForPath() {
        BoundingBox bb = new BoundingBox(0, 0, 100, 100);
        BoundingBox.BoundingBoxNode node = bb.treeForPath("se,nw,ne,sw".split(","));
        BoundingBox inner = node.se.nw.ne.sw.bb;
        BoundingBox.BoundingBoxNode tree = bb.treeForDepth(4);
        assertTrue(inner.equals(tree.se.nw.ne.sw.bb));

        assertEquals(node.nw, node.getQuadrant(QuadrantEnum.NW));
        assertEquals(node.ne, node.getQuadrant(QuadrantEnum.NE));
        assertEquals(node.sw, node.getQuadrant(QuadrantEnum.SW));
        assertEquals(node.se, node.getQuadrant(QuadrantEnum.SE));
    }

    private void runAssertion(double x, double y, BoundingBox bb, BoundingBox bbNW, BoundingBox bbNE, BoundingBox bbSW, BoundingBox bbSE, QuadrantEnum expected) {
        if (!bb.containsPoint(x, y)) {
            assertNull(expected);
            assertFalse(bbNW.containsPoint(x, y));
            assertFalse(bbNE.containsPoint(x, y));
            assertFalse(bbSW.containsPoint(x, y));
            assertFalse(bbSE.containsPoint(x, y));
            return;
        }
        QuadrantEnum received = bb.getQuadrant(x, y);
        assertEquals(expected, received);
        assertEquals(expected == QuadrantEnum.NW, bbNW.containsPoint(x, y));
        assertEquals(expected == QuadrantEnum.NE, bbNE.containsPoint(x, y));
        assertEquals(expected == QuadrantEnum.SW, bbSW.containsPoint(x, y));
        assertEquals(expected == QuadrantEnum.SE, bbSE.containsPoint(x, y));

        BoundingBox[] subdivided = bb.subdivide();
        assertEquals(expected == QuadrantEnum.NW, subdivided[0].containsPoint(x, y));
        assertEquals(expected == QuadrantEnum.NE, subdivided[1].containsPoint(x, y));
        assertEquals(expected == QuadrantEnum.SW, subdivided[2].containsPoint(x, y));
        assertEquals(expected == QuadrantEnum.SE, subdivided[3].containsPoint(x, y));
    }
}
