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
package com.espertech.esper.spatial.quadtree.filterindex;

import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.filterindex.SupportQuadTreeFilterIndexUtil.*;

public class TestQuadTreeFilterIndexSimple extends TestCase {

    private QuadTree<Object> tree;

    public void tearDown() {
        tree = null;
    }

    public void testGetSetRemove() {
        tree = QuadTreeFactory.make(0, 0, 100, 100);
        assertNull(QuadTreeFilterIndexGet.get(10, 20, tree));
        assertCollectAll(tree, "");

        QuadTreeFilterIndexSet.set(10, 20, "P0", tree);
        assertEquals("P0", QuadTreeFilterIndexGet.get(10, 20, tree));
        assertCollectAll(tree, "P0");

        QuadTreeFilterIndexDelete.delete(10, 20, tree);
        assertNull(QuadTreeFilterIndexGet.get(10, 20, tree));
        assertCollectAll(tree, "");
    }

    public void testPoints() {
        tree = QuadTreeFactory.make(0, 0, 10, 10);

        set(tree, 8.0, 4.0, "P0");
        assertCollectAll(tree, "P0");

        set(tree, 8.0, 1.0, "P1");
        assertCollectAll(tree, "P0,P1");

        set(tree, 8.0, 2.0, "P2");
        assertCollectAll(tree, "P0,P1,P2");

        set(tree, 4.0, 4.0, "P3");
        assertCollectAll(tree, "P0,P1,P2,P3");

        set(tree, 1.0, 9.0, "P4");
        assertCollectAll(tree, "P0,P1,P2,P3,P4");

        set(tree, 8.0, 3.0, "P5");
        assertCollectAll(tree, "P0,P1,P2,P3,P4,P5");

        set(tree, 0.0, 6.0, "P6");
        assertCollectAll(tree, "P0,P1,P2,P3,P4,P5,P6");

        set(tree, 5.0, 1.0, "P7");
        assertCollectAll(tree, "P0,P1,P2,P3,P4,P5,P6,P7");

        set(tree, 5.0, 8.0, "P8");
        assertCollectAll(tree, "P0,P1,P2,P3,P4,P5,P6,P7,P8");

        set(tree, 7.0, 6.0, "P9");
        assertCollectAll(tree, "P0,P1,P2,P3,P4,P5,P6,P7,P8,P9");
    }

    public void testSetRemoveTwiceSamePoint() {
        tree = QuadTreeFactory.make(0, 0, 100, 100);

        set(tree, 5, 8, "P1");
        set(tree, 5, 8, "P2");
        assertCollectAll(tree, "P2");

        SupportQuadTreeFilterIndexUtil.delete(tree, 5, 8);
        assertCollectAll(tree, "");

        SupportQuadTreeFilterIndexUtil.delete(tree, 5, 8);
        assertCollectAll(tree, "");
    }

    public void testFewValues() {
        tree = QuadTreeFactory.make(0, 0, 100, 100);

        set(tree, 73.32704983331149, 23.46990952575032, "P0");
        set(tree, 53.09747562396894, 17.100976152185034, "P1");
        set(tree, 56.75757294858788, 25.508506696809608, "P2");
        set(tree, 83.66639067675291, 76.53772974832937, "P3");
        set(tree, 51.01654641861326, 43.49009281983866, "P4");

        double beginX = 50.45945198254618;
        double endX = 88.31594559038719;

        double beginY = 4.577595744501329;
        double endY = 22.93393078279351;

        assertCollect(tree, beginX, beginY, endX - beginX, endY - beginY, "P1");
        assertCollectAll(tree, "P0,P1,P2,P3,P4");

        assertEquals("P0", QuadTreeFilterIndexGet.get(73.32704983331149, 23.46990952575032, tree));
        assertEquals("P1", QuadTreeFilterIndexGet.get(53.09747562396894, 17.100976152185034, tree));
        assertEquals("P2", QuadTreeFilterIndexGet.get(56.75757294858788, 25.508506696809608, tree));
        assertEquals("P3", QuadTreeFilterIndexGet.get(83.66639067675291, 76.53772974832937, tree));
        assertEquals("P4", QuadTreeFilterIndexGet.get(51.01654641861326, 43.49009281983866, tree));
    }
}
