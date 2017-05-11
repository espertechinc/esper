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

import com.espertech.esper.client.EPException;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeFactory;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;
import junit.framework.TestCase;

import java.util.List;

import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.navigateBranch;
import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.navigateLeaf;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.MXCIFQuadTreeFilterIndexDelete.delete;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.MXCIFQuadTreeFilterIndexSet.set;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.assertCollect;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.assertCollectAll;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.compare;

public class TestMXCIFQuadTreeFilterIndexScenarios extends TestCase {

    public void testSubdivideAddMany() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 2, 3);
        set(0, 0, 1, 1, "R1", tree);
        set(1, 2, 1, 1, "R2", tree);
        set(3, 2, 1, 1, "R3", tree);
        assertEquals(3, navigateLeaf(tree, "nw,nw").getCount());

        delete(1, 2, 1, 1, tree);
        delete(0, 0, 1, 1, tree);
        delete(3, 2, 1, 1, tree);
        assertCollectAll(tree, "");
    }

    public void testDimension() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(1000, 100000, 9000, 900000);

        try {
            set(10, 90, 1, 1, "R1", tree);
            fail();
        }
        catch (EPException ex) {
            assertEquals(ex.getMessage(), "Rectangle (10.0,90.0,1.0,1.0) not in {minX=1000.0, minY=100000.0, maxX=10000.0, maxY=1000000.0}");
        }

        try {
            set(10999999, 90, 1, 1, "R2", tree);
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        set(5000, 800000, 1, 1, "R3", tree);

        assertCollect(tree, 0, 0, 10000000, 10000000, "R3");
        assertCollect(tree, 4000, 790000, 1200, 11000, "R3");
        assertCollect(tree, 4000, 790000, 900, 9000, "");
        assertCollectAll(tree, "R3");
    }

    public void testSuperslim() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 1, 100);
        set(10, 90, 0.1, 0.2, "R1", tree);
        set(10, 95, 0.3, 0.4, "R2", tree);
        MXCIFQuadTreeNodeLeaf<Object> ne = navigateLeaf(tree, "sw,sw,sw,ne");
        compare(10, 90, 0.1, 0.2, "R1", (XYWHRectangleWValue) ne.getData());
        MXCIFQuadTreeNodeLeaf<Object> se = navigateLeaf(tree, "sw,sw,sw,se");
        compare(10, 95, 0.3, 0.4, "R2", (XYWHRectangleWValue) se.getData());
    }

    public void testSubdivideMultiChild() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 4, 3);
        set(60, 11, 1, 1, "R1", tree);
        set(60, 40, 1, 1, "R2", tree);
        set(70, 30, 1, 1, "R3", tree);
        set(60, 10, 1, 1, "R4", tree);
        set(90, 45, 1, 1, "R5", tree);

        navigateLeaf(tree, "nw");
        navigateLeaf(tree, "se");
        navigateLeaf(tree, "sw");
        MXCIFQuadTreeNodeBranch<Object> ne = navigateBranch(tree, "ne");
        assertEquals(2, ne.getLevel());

        MXCIFQuadTreeNodeLeaf<Object> nw = navigateLeaf(ne, "nw");
        List<XYWHRectangleWValue> collection = (List<XYWHRectangleWValue>) nw.getData();
        compare(60, 11, 1, 1, "R1", collection.get(0));
        compare(60, 10, 1, 1, "R4", collection.get(1));
        assertEquals(2, nw.getCount());

        MXCIFQuadTreeNodeLeaf<Object> se = navigateLeaf(ne, "se");
        compare(90, 45, 1, 1, "R5", (XYWHRectangleWValue) se.getData());
        assertEquals(1, se.getCount());

        MXCIFQuadTreeNodeLeaf<Object> sw = navigateLeaf(ne, "sw");
        collection = (List<XYWHRectangleWValue>) sw.getData();
        compare(60, 40, 1, 1, "R2", collection.get(0));
        compare(70, 30, 1, 1, "R3", collection.get(1));
        assertEquals(2, sw.getCount());

        delete(60, 11, 1, 1, tree);
        delete(60, 40, 1, 1, tree);

        MXCIFQuadTreeNodeLeaf<Object> root = navigateLeaf(tree, "");
        collection = (List<XYWHRectangleWValue>) root.getData();
        assertEquals(3, root.getCount());
        assertEquals(3, collection.size());
        compare(60, 10, 1, 1, "R4", collection.get(0));
        compare(70, 30, 1, 1, "R3", collection.get(1));
        compare(90, 45, 1, 1, "R5", collection.get(2));
    }

    public void testRemoveNonExistent() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 20, 20);
        delete(10, 61, 1, 1, tree);
        set(10, 60, 1, 1, "R1", tree);
        delete(10, 61, 1, 1, tree);
        set(10, 80, 1, 1, "R2", tree);
        set(20, 70, 1, 1, "R3", tree);
        set(10, 80, 1, 1, "R4", tree);
        assertCollectAll(tree, "R1,R3,R4");

        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "R1,R3,R4");

        delete(10, 61, 1, 1, tree);
        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "R1,R3,R4");

        delete(9, 60, 1, 1, tree);
        delete(10, 80, 1, 1, tree);
        assertEquals(2, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "R1,R3");

        delete(9, 60, 1, 1, tree);
        delete(10, 80, 1, 1, tree);
        delete(10, 60, 1, 1, tree);
        assertEquals(1, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "R3");

        delete(20, 70, 1, 1, tree);
        assertEquals(0, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "");
    }

    public void testSubdivideSingleMerge() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 3, 2);
        set(65, 75, 1, 1, "R1", tree);
        set(81, 60, 1, 1, "R2", tree);
        set(80, 60, 1, 1, "R3", tree);
        set(80, 61, 1, 1, "R4", tree);
        assertCollect(tree, 60, 60, 20.5, 20.5, "R1,R3,R4");
        assertCollectAll(tree, "R1,R2,R3,R4");

        assertFalse(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertEquals(4, navigateLeaf(tree, "se").getCount());
        List<XYWHRectangleWValue> collection = (List<XYWHRectangleWValue>) navigateLeaf(tree, "se").getData();
        assertEquals(4, collection.size());
        compare(65, 75, 1, 1, "R1", collection.get(0));
        compare(81, 60, 1, 1, "R2", collection.get(1));
        compare(80, 60, 1, 1, "R3", collection.get(2));
        compare(80, 61, 1, 1, "R4", collection.get(3));

        set(66, 78, 1, 1, "R5", tree);
        delete(65, 75, 1, 1, tree);
        delete(80, 60, 1, 1, tree);

        assertEquals(3, navigateLeaf(tree, "se").getCount());
        assertCollectAll(tree, "R2,R4,R5");
        assertEquals(3, collection.size());
        compare(81, 60, 1, 1, "R2", collection.get(0));
        compare(80, 61, 1, 1, "R4", collection.get(1));
        compare(66, 78, 1, 1, "R5", collection.get(2));

        delete(66, 78, 1, 1, tree);

        assertCollectAll(tree, "R2,R4");
        assertEquals(2, navigateLeaf(tree, "").getCount());
        collection = (List<XYWHRectangleWValue>) navigateLeaf(tree, "").getData();
        assertEquals(2, collection.size());
        compare(81, 60, 1, 1, "R2", collection.get(0));
        compare(80, 61, 1, 1, "R4", collection.get(1));
    }

    public void testSubdivideMerge() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 3, 2);
        assertEquals(1, tree.getRoot().getLevel());
        set(10, 10, 0.01, 0.01, "R1", tree);
        set(9.9, 10, 0.01, 0.01,"R2", tree);
        set(10, 9.9, 0.01, 0.01,"R3", tree);
        set(10, 10, 0.01, 0.01,"R4", tree);
        set(10, 9.9, 0.01, 0.01,"R5", tree);
        set(9.9, 10, 0.01, 0.01,"R6", tree);
        assertTrue(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertCollect(tree, 9, 10, 1, 1, "R4,R6");
        assertCollect(tree, 10, 9, 1, 1, "R4,R5");
        assertCollect(tree, 10, 10, 1, 1, "R4");
        assertCollect(tree, 9, 9, 2, 2, "R4,R5,R6");
        assertCollectAll(tree, "R4,R5,R6");

        set(10, 10, 0.01, 0.01,"R7", tree);
        assertTrue(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);

        set(9.9, 9.9, 0.01, 0.01,"R8", tree);

        assertFalse(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertEquals(1, tree.getRoot().getLevel());
        assertEquals(4, navigateLeaf(tree, "nw").getCount());
        List<XYWHRectangleWValue> collection = (List<XYWHRectangleWValue>) navigateLeaf(tree, "nw").getData();
        assertEquals(4, collection.size());
        compare(10, 10, 0.01, 0.01, "R7", collection.get(0));
        compare(9.9, 10, 0.01, 0.01, "R6", collection.get(1));
        compare(10, 9.9, 0.01, 0.01, "R5", collection.get(2));
        compare(9.9, 9.9, 0.01, 0.01, "R8", collection.get(3));
        assertCollect(tree, 9, 10, 1, 1, "R6,R7");
        assertCollect(tree, 10, 9, 1, 1, "R5,R7");
        assertCollect(tree, 10, 10, 1, 1, "R7");
        assertCollect(tree, 9, 9, 2, 2, "R5,R6,R7,R8");
        assertCollectAll(tree, "R5,R6,R7,R8");

        set(9.9, 10, 0.01, 0.01, "R9", tree);
        set(10, 9.9, 0.01, 0.01, "R10", tree);
        set(10, 10, 0.01, 0.01, "R11", tree);
        set(10, 10, 0.01, 0.01, "R12", tree);
        set(10, 10, 0.01, 0.01, "R13", tree);

        assertEquals(4, navigateLeaf(tree, "nw").getCount());
        assertEquals(2, navigateLeaf(tree, "nw").getLevel());
        assertEquals(4, collection.size());
        compare(10, 10, 0.01, 0.01, "R13", collection.get(0));
        compare(9.9, 10, 0.01, 0.01, "R9", collection.get(1));
        compare(10, 9.9, 0.01, 0.01, "R10", collection.get(2));
        compare(9.9, 9.9, 0.01, 0.01, "R8", collection.get(3));
        assertCollect(tree, 9, 10, 1, 1, "R9,R13");
        assertCollect(tree, 10, 9, 1, 1, "R10,R13");
        assertCollect(tree, 10, 10, 1, 1, "R13");
        assertCollect(tree, 9, 9, 2, 2, "R8,R9,R10,R13");

        delete(9.9, 10, 0.01, 0.01, tree);
        delete(10, 9.9, 0.01, 0.01, tree);
        delete(10, 9.9, 0.01, 0.01, tree);
        delete(10, 9.9, 0.01, 0.01, tree);
        assertCollect(tree, 9, 10, 1, 1, "R13");
        assertCollect(tree, 10, 9, 1, 1, "R13");
        assertCollect(tree, 10, 10, 1, 1, "R13");
        assertCollect(tree, 9, 9, 2, 2, "R8,R13");
        assertCollectAll(tree, "R8,R13");

        assertEquals(2, navigateLeaf(tree, "").getCount());
        collection = (List<XYWHRectangleWValue>) navigateLeaf(tree, "").getData();
        assertEquals(2, collection.size());
        compare(10, 10, 0.01, 0.01, "R13", collection.get(0));
        compare(9.9, 9.9, 0.01, 0.01, "R8", collection.get(1));

        delete(9.9, 9.9, 0.01, 0.01, tree);
        delete(10, 10, 0.01, 0.01, tree);
        assertTrue(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertEquals(0, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "");
    }
}
