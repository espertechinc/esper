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
package com.espertech.esper.spatial.quadtree.mxciffilterindex;

import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeFactory;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.mxciffilterindex.MXCIFQuadTreeFilterIndexDelete.delete;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.MXCIFQuadTreeFilterIndexGet.get;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.MXCIFQuadTreeFilterIndexSet.set;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.assertCollect;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.assertCollectAll;

public class TestMXCIFQuadTreeFilterIndexSimple extends TestCase {

    private MXCIFQuadTree<Object> tree;

    public void tearDown() {
        tree = null;
    }

    public void testGetSetRemove() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100);
        assertNull(get(10, 20, 30, 40, tree));
        assertCollectAll(tree, "");

        set(10, 20, 30, 40, "R0", tree);
        assertEquals("R0", get(10, 20, 30, 40, tree));
        assertCollectAll(tree, "R0");

        delete(10, 20, 30, 40, tree);
        assertNull(get(10, 20, 30, 40, tree));
        assertCollectAll(tree, "");
    }

    public void testPoints() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 10, 10);

        set(8.0, 4.0, 1, 1, "R0", tree);
        assertCollectAll(tree, "R0");

        set(8.0, 1.0, 1, 1, "R1", tree);
        assertCollectAll(tree, "R0,R1");

        set(8.0, 2.0, 1, 1, "R2", tree);
        assertCollectAll(tree, "R0,R1,R2");

        set(4.0, 4.0, 1, 1, "R3", tree);
        assertCollectAll(tree, "R0,R1,R2,R3");

        set(1.0, 9.0, 1, 1, "R4", tree);
        assertCollectAll(tree, "R0,R1,R2,R3,R4");

        set(8.0, 3.0, 1, 1, "R5", tree);
        assertCollectAll(tree, "R0,R1,R2,R3,R4,R5");

        set(0.0, 6.0, 1, 1, "R6", tree);
        assertCollectAll(tree, "R0,R1,R2,R3,R4,R5,R6");

        set(5.0, 1.0, 1, 1, "R7", tree);
        assertCollectAll(tree, "R0,R1,R2,R3,R4,R5,R6,R7");

        set(5.0, 8.0, 1, 1, "R8", tree);
        assertCollectAll(tree, "R0,R1,R2,R3,R4,R5,R6,R7,R8");

        set(7.0, 6.0, 1, 1, "R9", tree);
        assertCollectAll(tree, "R0,R1,R2,R3,R4,R5,R6,R7,R8,R9");
    }

    public void testSetRemoveTwiceSamePoint() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100);

        set(5, 8, 1, 2, "R1", tree);
        set(5, 8, 1, 2, "R2", tree);
        assertCollectAll(tree, "R2");

        delete(5, 8, 1, 2, tree);
        assertCollectAll(tree, "");

        delete(5, 8, 1, 2, tree);
        assertCollectAll(tree, "");
    }

    public void testFewValues() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100);

        set(73.32704983331149, 23.46990952575032, 1, 1, "R0", tree);
        set(53.09747562396894, 17.100976152185034, 1, 1, "R1", tree);
        set(56.75757294858788, 25.508506696809608, 1, 1, "R2", tree);
        set(83.66639067675291, 76.53772974832937, 1, 1, "R3", tree);
        set(51.01654641861326, 43.49009281983866, 1, 1, "R4", tree);

        double beginX = 50.45945198254618;
        double endX = 88.31594559038719;

        double beginY = 4.577595744501329;
        double endY = 22.93393078279351;

        assertCollect(tree, beginX, beginY, endX - beginX, endY - beginY, "R1");
        assertCollectAll(tree, "R0,R1,R2,R3,R4");

        assertEquals("R0", get(73.32704983331149, 23.46990952575032, 1, 1, tree));
        assertEquals("R1", get(53.09747562396894, 17.100976152185034, 1, 1, tree));
        assertEquals("R2", get(56.75757294858788, 25.508506696809608, 1, 1, tree));
        assertEquals("R3", get(83.66639067675291, 76.53772974832937, 1, 1, tree));
        assertEquals("R4", get(51.01654641861326, 43.49009281983866, 1, 1, tree));
    }
}
