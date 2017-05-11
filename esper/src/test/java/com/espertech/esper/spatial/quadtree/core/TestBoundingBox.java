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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class TestBoundingBox extends TestCase {

    public void testFrom() {
        BoundingBox bb = BoundingBox.from(10, 20, 4, 15);
        assertEquals(10d, bb.getMinX());
        assertEquals(20d, bb.getMinY());
        assertEquals(14d, bb.getMaxX());
        assertEquals(35d, bb.getMaxY());
    }

    public void testContainsPoint() {
        BoundingBox bb = new BoundingBox(10, 20, 40, 60);

        assertTrue(bb.containsPoint(10, 20));
        assertTrue(bb.containsPoint(39.9999, 59.9999));

        assertFalse(bb.containsPoint(40, 60));
        assertFalse(bb.containsPoint(10, 100));
        assertFalse(bb.containsPoint(100, 10));
    }

    public void testIntersectsBoxIncludingEnd() {

        Rectangle2D.Double ref = rect(1, 2, 4, 6);
        assertIntersectsIncludingEnd(true, rect(1, 2, 4, 6), ref);
        assertIntersectsIncludingEnd(true, rect(2, 3, 1, 1), ref);
        assertIntersectsIncludingEnd(true, rect(0, 0, 10, 10), ref);

        // nw
        assertIntersectsIncludingEnd(true, rect(0, 0, 1.00001, 3), ref);
        assertIntersectsIncludingEnd(true, rect(0, 0, 1, 2), ref);
        assertIntersectsIncludingEnd(false, rect(0, 0, 0.99999, 2), ref);

        // ne
        assertIntersectsIncludingEnd(true, rect(4.99999, 0, 1, 3), ref);
        assertIntersectsIncludingEnd(true, rect(5, 0, 1, 3), ref);
        assertIntersectsIncludingEnd(false, rect(5.00001, 0, 1, 3), ref);

        // sw
        assertIntersectsIncludingEnd(true, rect(0, 7.9999, 1.5, 1), ref);
        assertIntersectsIncludingEnd(true, rect(0, 8, 1.5, 1), ref);
        assertIntersectsIncludingEnd(false, rect(0, 8.00001, 1.5, 1), ref);

        // se
        assertIntersectsIncludingEnd(true, rect(0, 0, 3, 2.00001), ref);
        assertIntersectsIncludingEnd(true, rect(0, 0, 3, 2), ref);
        assertIntersectsIncludingEnd(false, rect(0, 0, 3, 1.99999), ref);
    }

    private void assertIntersectsIncludingEnd(boolean expected, Rectangle2D.Double one, Rectangle2D.Double two) {
        BoundingBox bbOne = BoundingBox.from(one.getX(), one.getY(), one.width, one.getHeight());
        BoundingBox bbTwo = BoundingBox.from(two.getX(), two.getY(), two.width, two.getHeight());

        assertEquals(expected, bbOne.intersectsBoxIncludingEnd(two.getX(), two.getY(), two.getWidth(), two.getHeight()));
        assertEquals(expected, bbTwo.intersectsBoxIncludingEnd(one.getX(), one.getY(), one.getWidth(), one.getHeight()));
    }

    public void testQuadrant() {
        BoundingBox bb = new BoundingBox(10, 20, 40, 60);

        double w = (bb.getMaxX() - bb.getMinX()) / 2d;
        double h = (bb.getMaxY() - bb.getMinY()) / 2d;

        BoundingBox bbNW = new BoundingBox(bb.getMinX(), bb.getMinY(), bb.getMinX() + w, bb.getMinY() + h);
        BoundingBox bbNE = new BoundingBox(bb.getMinX() + w, bb.getMinY(), bb.getMaxX(), bb.getMinY() + h);
        BoundingBox bbSW = new BoundingBox(bb.getMinX(), bb.getMinY() + h, bb.getMinX() + w, bb.getMaxY());
        BoundingBox bbSE = new BoundingBox(bb.getMinX() + w, bb.getMinY() + h, bb.getMaxX(), bb.getMaxY());

        runAssertionQuadrant(10, 20, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NW);
        runAssertionQuadrant(9, 19, bb, bbNW, bbNE, bbSW, bbSE, null);
        runAssertionQuadrant(40, 60, bb, bbNW, bbNE, bbSW, bbSE, null);
        runAssertionQuadrant(39.9999, 59.999999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SE);
        runAssertionQuadrant(39.9999, 20, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NE);
        runAssertionQuadrant(10, 59.999999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SW);
        runAssertionQuadrant(24.9999, 39.9999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NW);
        runAssertionQuadrant(25, 40, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SE);
        runAssertionQuadrant(24.9999, 40, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.SW);
        runAssertionQuadrant(25, 39.9999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantEnum.NE);
    }

    public void testQuadrantIfFits() {
        BoundingBox bb = new BoundingBox(10, 20, 40, 60);

        double w = (bb.getMaxX() - bb.getMinX()) / 2d;
        double h = (bb.getMaxY() - bb.getMinY()) / 2d;

        BoundingBox bbNW = new BoundingBox(bb.getMinX(), bb.getMinY(), bb.getMinX() + w, bb.getMinY() + h);
        BoundingBox bbNE = new BoundingBox(bb.getMinX() + w, bb.getMinY(), bb.getMaxX(), bb.getMinY() + h);
        BoundingBox bbSW = new BoundingBox(bb.getMinX(), bb.getMinY() + h, bb.getMinX() + w, bb.getMaxY());
        BoundingBox bbSE = new BoundingBox(bb.getMinX() + w, bb.getMinY() + h, bb.getMaxX(), bb.getMaxY());

        assertEquals(QuadrantAppliesEnum.NW, bb.getQuadrantApplies(10, 20, 1, 1));
        assertEquals(QuadrantAppliesEnum.SOME, bb.getQuadrantApplies(10, 20, 40, 60));
        assertEquals(QuadrantAppliesEnum.NONE, bb.getQuadrantApplies(0, 0, 1, 1));

        // within single
        runAssertionQuadrantAppliesOne(11, 21, 1, 1, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NW);
        runAssertionQuadrantAppliesOne(26, 21, 1, 1, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NE);
        runAssertionQuadrantAppliesOne(11, 50, 1, 1, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.SW);
        runAssertionQuadrantAppliesOne(26, 50, 1, 1, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.SE);

        // NW approach
        runAssertionQuadrantAppliesNone(0, 0, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(0, 0, 9.9999, 19.9999, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(0, 0, 10, 19.9999, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(0, 0, 9.9999, 20, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesOne(0, 0, 10, 20, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NW);
        runAssertionQuadrantAppliesOne(0, 0, 10+14.999, 20+19.999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NW);
        runAssertionQuadrantAppliesMulti(0, 0, 10+15, 20+19.9999, bb, bbNW, bbNE, bbSW, bbSE, true, true, false, false);
        runAssertionQuadrantAppliesMulti(0, 0, 10+14.999, 20+20, bb, bbNW, bbNE, bbSW, bbSE, true, false, true, false);
        runAssertionQuadrantAppliesMulti(0, 0, 10+15, 20+20, bb, bbNW, bbNE, bbSW, bbSE, true, true, true, true);

        // NE approach
        runAssertionQuadrantAppliesNone(45, 0, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(40.001, 0, 0, 19.9999, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(40.001, 0, 0, 20, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(40, 0, 0, 19.9999, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesOne(40, 0, 0, 20, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NE);
        runAssertionQuadrantAppliesOne(40-14.999, 0, 0, 20+19.999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NE);
        runAssertionQuadrantAppliesMulti(40-15, 0, 0, 20+19.999, bb, bbNW, bbNE, bbSW, bbSE, true, true, false, false);
        runAssertionQuadrantAppliesMulti(40-14.999, 0, 0, 20+20, bb, bbNW, bbNE, bbSW, bbSE, false, true, false, true);
        runAssertionQuadrantAppliesMulti(40-15, 0, 0, 20+20, bb, bbNW, bbNE, bbSW, bbSE, true, true, true, true);

        // SW approach
        runAssertionQuadrantAppliesNone(0, 70, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(0, 60.0001, 9.9999, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(0, 60.0001, 10, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(0, 60, 9.9999, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesOne(0, 60, 10, 0, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.SW);
        runAssertionQuadrantAppliesOne(0, 40.001, 10+14.999, 5, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.SW);
        runAssertionQuadrantAppliesMulti(0, 40, 10+14.999, 5, bb, bbNW, bbNE, bbSW, bbSE, true, false, true, false);
        runAssertionQuadrantAppliesMulti(0, 40.001, 10+15, 5, bb, bbNW, bbNE, bbSW, bbSE, false, false, true, true);
        runAssertionQuadrantAppliesMulti(0, 40, 10+15, 5, bb, bbNW, bbNE, bbSW, bbSE, true, true, true, true);

        // SE approach
        runAssertionQuadrantAppliesNone(50, 70, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(40.001, 60.001, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(40, 60.001, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesNone(40.001, 60, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesOne(40, 60, 0, 0, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.SE);
        runAssertionQuadrantAppliesOne(40-14.9999, 60-19.999, 100, 100, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.SE);
        runAssertionQuadrantAppliesMulti(40-14.9999, 60-20, 100, 100, bb, bbNW, bbNE, bbSW, bbSE, false, true, false, true);
        runAssertionQuadrantAppliesMulti(40-15, 60-19.999, 100, 100, bb, bbNW, bbNE, bbSW, bbSE, false, false, true, true);
        runAssertionQuadrantAppliesMulti(40-15, 60-20, 100, 100, bb, bbNW, bbNE, bbSW, bbSE, true, true, true, true);

        // contains-all
        runAssertionQuadrantAppliesAll(0, 0, 100, 100, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesAll(10, 20, 40, 60, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesAll(24, 39, 2, 2, bb, bbNW, bbNE, bbSW, bbSE);
        runAssertionQuadrantAppliesAll(25, 40, 0, 0, bb, bbNW, bbNE, bbSW, bbSE);

        // start-within
        runAssertionQuadrantAppliesOne(10, 20, 14.9999, 19.999, bb, bbNW, bbNE, bbSW, bbSE, QuadrantAppliesEnum.NW);
        runAssertionQuadrantAppliesMulti(10, 20, 15, 19.999, bb, bbNW, bbNE, bbSW, bbSE, true, true, false, false);
        runAssertionQuadrantAppliesMulti(10, 20, 14.9999, 20, bb, bbNW, bbNE, bbSW, bbSE, true, false, true, false);
        runAssertionQuadrantAppliesMulti(10, 20, 15, 20, bb, bbNW, bbNE, bbSW, bbSE, true, true, true, true);

        // try random
        Random random = new Random();
        for (int i = 0; i < 1000000; i++) {
            double width = random.nextDouble() * 50;
            double height = random.nextDouble() * 70;
            double x = random.nextDouble() * 50;
            double y = random.nextDouble() * 70 + 10;
            QuadrantAppliesEnum result = bb.getQuadrantApplies(x, y, width, height);
            boolean nw = bbNW.intersectsBoxIncludingEnd(x, y, width, height);
            boolean ne = bbNE.intersectsBoxIncludingEnd(x, y, width, height);
            boolean sw = bbSW.intersectsBoxIncludingEnd(x, y, width, height);
            boolean se = bbSE.intersectsBoxIncludingEnd(x, y, width, height);
            if (result == QuadrantAppliesEnum.NONE && (nw | ne | sw | se)) {
                fail();
            }
            else if (result == QuadrantAppliesEnum.SOME) {
                assertTrue((nw ? 1 : 0) + (ne ? 1 : 0) + (sw ? 1 : 0) + (se ? 1 : 0) > 1);
            }
            else {
                assertEquals(result == QuadrantAppliesEnum.NW, nw);
                assertEquals(result == QuadrantAppliesEnum.NE, ne);
                assertEquals(result == QuadrantAppliesEnum.SW, sw);
                assertEquals(result == QuadrantAppliesEnum.SE, se);
            }
        }
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

    private void runAssertionQuadrant(double x, double y, BoundingBox bb, BoundingBox bbNW, BoundingBox bbNE, BoundingBox bbSW, BoundingBox bbSE, QuadrantEnum expected) {
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

    private void runAssertionQuadrantAppliesMulti(double x, double y, double width, double height, BoundingBox bb, BoundingBox bbNW, BoundingBox bbNE, BoundingBox bbSW, BoundingBox bbSE, boolean intersectsNW, boolean intersectsNE, boolean intersectsSW, boolean intersectsSE) {
        assertEquals(QuadrantAppliesEnum.SOME, bb.getQuadrantApplies(x, y, width, height));
        assertEquals(intersectsNW, bbNW.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(intersectsNE, bbNE.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(intersectsSW, bbSW.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(intersectsSE, bbSE.intersectsBoxIncludingEnd(x, y, width, height));
    }

    private void runAssertionQuadrantAppliesNone(double x, double y, double width, double height, BoundingBox bb, BoundingBox bbNW, BoundingBox bbNE, BoundingBox bbSW, BoundingBox bbSE) {
        assertEquals(QuadrantAppliesEnum.NONE, bb.getQuadrantApplies(x, y, width, height));
        assertFalse(bb.intersectsBoxIncludingEnd(x, y, width, height));
        assertFalse(bbNW.intersectsBoxIncludingEnd(x, y, width, height));
        assertFalse(bbNE.intersectsBoxIncludingEnd(x, y, width, height));
        assertFalse(bbSW.intersectsBoxIncludingEnd(x, y, width, height));
        assertFalse(bbSE.intersectsBoxIncludingEnd(x, y, width, height));
    }

    private void runAssertionQuadrantAppliesAll(double x, double y, double width, double height, BoundingBox bb, BoundingBox bbNW, BoundingBox bbNE, BoundingBox bbSW, BoundingBox bbSE) {
        assertEquals(QuadrantAppliesEnum.SOME, bb.getQuadrantApplies(x, y, width, height));
        assertTrue(bb.intersectsBoxIncludingEnd(x, y, width, height));
        assertTrue(bbNW.intersectsBoxIncludingEnd(x, y, width, height));
        assertTrue(bbNE.intersectsBoxIncludingEnd(x, y, width, height));
        assertTrue(bbSW.intersectsBoxIncludingEnd(x, y, width, height));
        assertTrue(bbSE.intersectsBoxIncludingEnd(x, y, width, height));
    }

    private void runAssertionQuadrantAppliesOne(double x, double y, double width, double height, BoundingBox bb, BoundingBox bbNW, BoundingBox bbNE, BoundingBox bbSW, BoundingBox bbSE, QuadrantAppliesEnum expected) {
        assertEquals(expected, bb.getQuadrantApplies(x, y, width, height));
        assertTrue(bb.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(expected == QuadrantAppliesEnum.NW, bbNW.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(expected == QuadrantAppliesEnum.NE, bbNE.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(expected == QuadrantAppliesEnum.SW, bbSW.intersectsBoxIncludingEnd(x, y, width, height));
        assertEquals(expected == QuadrantAppliesEnum.SE, bbSE.intersectsBoxIncludingEnd(x, y, width, height));
    }

    private Rectangle2D.Double rect(double x, double y, double width, double height) {
        return new Rectangle.Double(x, y, width, height);
    }
}
