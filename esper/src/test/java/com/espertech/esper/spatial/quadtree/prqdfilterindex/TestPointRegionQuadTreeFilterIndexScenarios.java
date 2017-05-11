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
package com.espertech.esper.spatial.quadtree.prqdfilterindex;

import com.espertech.esper.client.EPException;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeLeaf;
import junit.framework.TestCase;

import java.util.List;

import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.navigateBranch;
import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.navigateLeaf;
import static com.espertech.esper.spatial.quadtree.prqdfilterindex.SupportPointRegionQuadTreeFilterIndexUtil.*;

public class TestPointRegionQuadTreeFilterIndexScenarios extends TestCase {

    public void testSubdivideAddMany() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 2, 3);
        set(tree, 0, 0, "P1");
        set(tree, 1, 2, "P2");
        set(tree, 3, 2, "P3");
        assertEquals(3, navigateLeaf(tree, "nw,nw").getCount());

        delete(tree, 1, 2);
        delete(tree, 0, 0);
        delete(tree, 3, 2);
        assertCollectAll(tree, "");
    }

    public void testDimension() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(1000, 100000, 9000, 900000);

        try {
            set(tree, 10, 90, "P1");
            fail();
        }
        catch (EPException ex) {
            assertEquals(ex.getMessage(), "Point (10.0,90.0) not in {minX=1000.0, minY=100000.0, maxX=10000.0, maxY=1000000.0}");
        }

        try {
            set(tree, 10999999, 90, "P2");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        set(tree, 5000, 800000, "P3");

        assertCollect(tree, 0, 0, 10000000, 10000000, "P3");
        assertCollect(tree, 4000, 790000, 1200, 11000, "P3");
        assertCollect(tree, 4000, 790000, 900, 9000, "");
        assertCollectAll(tree, "P3");
    }

    public void testSuperslim() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 1, 100);
        set(tree, 10, 90, "P1");
        set(tree, 10, 95, "P2");
        PointRegionQuadTreeNodeLeaf<Object> ne = navigateLeaf(tree, "sw,sw,sw,ne");
        compare(10, 90, "P1", (XYPointWValue) ne.getPoints());
        PointRegionQuadTreeNodeLeaf<Object> se = navigateLeaf(tree, "sw,sw,sw,se");
        compare(10, 95, "P2", (XYPointWValue) se.getPoints());
    }

    public void testSubdivideMultiChild() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 4, 3);
        set(tree, 60, 11, "P1");
        set(tree, 60, 40, "P2");
        set(tree, 70, 30, "P3");
        set(tree, 60, 10, "P4");
        set(tree, 90, 45, "P5");

        navigateLeaf(tree, "nw");
        navigateLeaf(tree, "se");
        navigateLeaf(tree, "sw");
        PointRegionQuadTreeNodeBranch<Object> ne = navigateBranch(tree, "ne");
        assertEquals(2, ne.getLevel());

        PointRegionQuadTreeNodeLeaf<Object> nw = navigateLeaf(ne, "nw");
        List<XYPointWValue> collection = (List<XYPointWValue>) nw.getPoints();
        compare(60, 11, "P1", collection.get(0));
        compare(60, 10, "P4", collection.get(1));
        assertEquals(2, nw.getCount());

        PointRegionQuadTreeNodeLeaf<Object> se = navigateLeaf(ne, "se");
        compare(90, 45, "P5", (XYPointWValue) se.getPoints());
        assertEquals(1, se.getCount());

        PointRegionQuadTreeNodeLeaf<Object> sw = navigateLeaf(ne, "sw");
        collection = (List<XYPointWValue>) sw.getPoints();
        compare(60, 40, "P2", collection.get(0));
        compare(70, 30, "P3", collection.get(1));
        assertEquals(2, sw.getCount());

        delete(tree, 60, 11);
        delete(tree, 60, 40);

        PointRegionQuadTreeNodeLeaf<Object> root = navigateLeaf(tree, "");
        collection = (List<XYPointWValue>) root.getPoints();
        assertEquals(3, root.getCount());
        assertEquals(3, collection.size());
        compare(60, 10, "P4", collection.get(0));
        compare(70, 30, "P3", collection.get(1));
        compare(90, 45, "P5", collection.get(2));
    }

    public void testRemoveNonExistent() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 20, 20);
        delete(tree, 10, 61);
        set(tree, 10, 60, "P1");
        delete(tree, 10, 61);
        set(tree, 10, 80, "P2");
        set(tree, 20, 70, "P3");
        set(tree, 10, 80, "P4");
        assertCollectAll(tree, "P1,P3,P4");

        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "P1,P3,P4");

        delete(tree, 10, 61);
        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "P1,P3,P4");

        delete(tree, 9, 60);
        delete(tree, 10, 80);
        assertEquals(2, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "P1,P3");

        delete(tree, 9, 60);
        delete(tree, 10, 80);
        delete(tree, 10, 60);
        assertEquals(1, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "P3");

        delete(tree, 20, 70);
        assertEquals(0, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "");
    }

    public void testSubdivideSingleMerge() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 3, 2);
        set(tree, 65, 75, "P1");
        set(tree, 81, 60, "P2");
        set(tree, 80, 60, "P3");
        set(tree, 80, 61, "P4");
        assertCollect(tree, 60, 60, 21, 21, "P1,P3,P4");
        assertCollectAll(tree, "P1,P2,P3,P4");

        assertFalse(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertEquals(4, navigateLeaf(tree, "se").getCount());
        List<XYPointWValue> collection = (List<XYPointWValue>) navigateLeaf(tree, "se").getPoints();
        assertEquals(4, collection.size());
        compare(65, 75, "P1", collection.get(0));
        compare(81, 60, "P2", collection.get(1));
        compare(80, 60, "P3", collection.get(2));
        compare(80, 61, "P4", collection.get(3));

        set(tree, 66, 78, "P5");
        delete(tree, 65, 75);
        delete(tree, 80, 60);

        assertEquals(3, navigateLeaf(tree, "se").getCount());
        assertCollectAll(tree, "P2,P4,P5");
        assertEquals(3, collection.size());
        compare(81, 60, "P2", collection.get(0));
        compare(80, 61, "P4", collection.get(1));
        compare(66, 78, "P5", collection.get(2));

        delete(tree, 66, 78);

        assertCollectAll(tree, "P2,P4");
        assertEquals(2, navigateLeaf(tree, "").getCount());
        collection = (List<XYPointWValue>) navigateLeaf(tree, "").getPoints();
        assertEquals(2, collection.size());
        compare(81, 60, "P2", collection.get(0));
        compare(80, 61, "P4", collection.get(1));
    }

    public void testSubdivideMerge() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 3, 2);
        assertEquals(1, tree.getRoot().getLevel());
        set(tree, 10, 10, "P1");
        set(tree, 9.9, 10, "P2");
        set(tree, 10, 9.9, "P3");
        set(tree, 10, 10, "P4");
        set(tree, 10, 9.9, "P5");
        set(tree, 9.9, 10, "P6");
        assertTrue(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertCollect(tree, 9, 10, 1, 1, "P6");
        assertCollect(tree, 10, 9, 1, 1, "P5");
        assertCollect(tree, 10, 10, 1, 1, "P4");
        assertCollect(tree, 9, 9, 2, 2, "P4,P5,P6");
        assertCollectAll(tree, "P4,P5,P6");

        set(tree, 10, 10, "P7");
        assertTrue(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);

        set(tree, 9.9, 9.9, "P8");

        assertFalse(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertEquals(1, tree.getRoot().getLevel());
        assertEquals(4, navigateLeaf(tree, "nw").getCount());
        List<XYPointWValue> collection = (List<XYPointWValue>) navigateLeaf(tree, "nw").getPoints();
        assertEquals(4, collection.size());
        compare(10, 10, "P7", collection.get(0));
        compare(9.9, 10, "P6", collection.get(1));
        compare(10, 9.9, "P5", collection.get(2));
        compare(9.9, 9.9, "P8", collection.get(3));
        assertCollect(tree, 9, 10, 1, 1, "P6");
        assertCollect(tree, 10, 9, 1, 1, "P5");
        assertCollect(tree, 10, 10, 1, 1, "P7");
        assertCollect(tree, 9, 9, 2, 2, "P5,P6,P7,P8");
        assertCollectAll(tree, "P5,P6,P7,P8");

        set(tree, 9.9, 10, "P9");
        set(tree, 10, 9.9, "P10");
        set(tree, 10, 10, "P11");
        set(tree, 10, 10, "P12");
        set(tree, 10, 10, "P13");

        assertEquals(4, navigateLeaf(tree, "nw").getCount());
        assertEquals(2, navigateLeaf(tree, "nw").getLevel());
        assertEquals(4, collection.size());
        compare(10, 10, "P13", collection.get(0));
        compare(9.9, 10, "P9", collection.get(1));
        compare(10, 9.9, "P10", collection.get(2));
        compare(9.9, 9.9, "P8", collection.get(3));
        assertCollect(tree, 9, 10, 1, 1, "P9");
        assertCollect(tree, 10, 9, 1, 1, "P10");
        assertCollect(tree, 10, 10, 1, 1, "P13");
        assertCollect(tree, 9, 9, 2, 2, "P8,P9,P10,P13");

        delete(tree, 9.9, 10);
        delete(tree, 10, 9.9);
        delete(tree, 10, 9.9);
        delete(tree, 10, 9.9);
        assertCollect(tree, 9, 10, 1, 1, "");
        assertCollect(tree, 10, 9, 1, 1, "");
        assertCollect(tree, 10, 10, 1, 1, "P13");
        assertCollect(tree, 9, 9, 2, 2, "P8,P13");
        assertCollectAll(tree, "P8,P13");

        assertEquals(2, navigateLeaf(tree, "").getCount());
        collection = (List<XYPointWValue>) navigateLeaf(tree, "").getPoints();
        assertEquals(2, collection.size());
        compare(10, 10, "P13", collection.get(0));
        compare(9.9, 9.9, "P8", collection.get(1));

        delete(tree, 9.9, 9.9);
        delete(tree, 10, 10);
        assertTrue(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertEquals(0, navigateLeaf(tree, "").getCount());
        assertCollectAll(tree, "");
    }
}
