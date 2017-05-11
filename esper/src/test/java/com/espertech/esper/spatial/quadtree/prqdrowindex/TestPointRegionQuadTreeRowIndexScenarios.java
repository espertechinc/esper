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
package com.espertech.esper.spatial.quadtree.prqdrowindex;

import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeLeaf;
import junit.framework.TestCase;

import java.util.List;

import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.navigateBranch;
import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.navigateLeaf;
import static com.espertech.esper.spatial.quadtree.prqdrowindex.SupportPointRegionQuadTreeRowIndexUtil.*;

public class TestPointRegionQuadTreeRowIndexScenarios extends TestCase {

    public void testSubdivideAdd() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 2, 3);
        addNonUnique(tree, 0, 0, "P1");
        addNonUnique(tree, 0, 0, "P2");
        addNonUnique(tree, 0, 0, "P3");
        assertEquals(3, navigateLeaf(tree, "nw,nw").getCount());
    }

    public void testDimension() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(1000, 100000, 9000, 900000);
        assertFalse(addNonUnique(tree, 10, 90, "P1"));
        assertFalse(addNonUnique(tree, 10999999, 90, "P2"));
        assertTrue(addNonUnique(tree, 5000, 800000, "P3"));

        assertFound(tree, 0, 0, 10000000, 10000000, "P3");
        assertFound(tree, 4000, 790000, 1200, 11000, "P3");
        assertFound(tree, 4000, 790000, 900, 9000, "");
    }

    public void testSuperslim() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 1, 100);
        addNonUnique(tree, 10, 90, "P1");
        addNonUnique(tree, 10, 95, "P2");
        PointRegionQuadTreeNodeLeaf<Object> ne = navigateLeaf(tree, "sw,sw,sw,ne");
        compare(10, 90, "P1", (XYPointMultiType) ne.getPoints());
        PointRegionQuadTreeNodeLeaf<Object> se = navigateLeaf(tree, "sw,sw,sw,se");
        compare(10, 95, "P2", (XYPointMultiType) se.getPoints());
    }

    public void testSubdivideMultiChild() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 4, 3);
        addNonUnique(tree, 60, 10, "P1");
        addNonUnique(tree, 60, 40, "P2");
        addNonUnique(tree, 70, 30, "P3");
        addNonUnique(tree, 60, 10, "P4");
        addNonUnique(tree, 90, 45, "P5");

        navigateLeaf(tree, "nw");
        navigateLeaf(tree, "se");
        navigateLeaf(tree, "sw");
        PointRegionQuadTreeNodeBranch<Object> ne = navigateBranch(tree, "ne");
        assertEquals(2, ne.getLevel());

        PointRegionQuadTreeNodeLeaf<Object> nw = navigateLeaf(ne, "nw");
        compare(60, 10, "[P1, P4]", (XYPointMultiType) nw.getPoints());
        assertEquals(2, nw.getCount());

        PointRegionQuadTreeNodeLeaf<Object> se = navigateLeaf(ne, "se");
        compare(90, 45, "P5", (XYPointMultiType) se.getPoints());
        assertEquals(1, se.getCount());

        PointRegionQuadTreeNodeLeaf<Object> sw = navigateLeaf(ne, "sw");
        List<XYPointMultiType> collection = (List<XYPointMultiType>) sw.getPoints();
        compare(60, 40, "P2", collection.get(0));
        compare(70, 30, "P3", collection.get(1));
        assertEquals(2, sw.getCount());

        remove(tree, 60, 10, "P1");
        remove(tree, 60, 40, "P2");

        PointRegionQuadTreeNodeLeaf<Object> root = navigateLeaf(tree, "");
        collection = (List<XYPointMultiType>) root.getPoints();
        assertEquals(3, root.getCount());
        assertEquals(3, collection.size());
        compare(60, 10, "[P4]", collection.get(0));
        compare(70, 30, "P3", collection.get(1));
        compare(90, 45, "P5", collection.get(2));
    }

    public void testRemoveNonExistent() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 20, 20);
        remove(tree, 10, 61, "P1");
        addNonUnique(tree, 10, 60, "P1");
        remove(tree, 10, 61, "P1");
        addNonUnique(tree, 10, 80, "P2");
        addNonUnique(tree, 20, 70, "P3");
        addNonUnique(tree, 10, 80, "P4");
        assertEquals(4, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "P1,P2,P3,P4");

        remove(tree, 10, 61, "P1");
        remove(tree, 9, 60, "P1");
        remove(tree, 10, 60, "P2");
        remove(tree, 10, 80, "P1");
        assertEquals(4, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "P1,P2,P3,P4");

        remove(tree, 10, 80, "P4");
        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "P1,P2,P3");

        remove(tree, 10, 80, "P2");
        assertEquals(2, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "P1,P3");

        remove(tree, 10, 60, "P1");
        assertEquals(1, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "P3");

        remove(tree, 20, 70, "P3");
        assertEquals(0, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "");
    }

    public void testSubdivideSingleMerge() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 3, 2);
        addNonUnique(tree, 65, 75, "P1");
        addNonUnique(tree, 80, 75, "P2");
        addNonUnique(tree, 80, 60, "P3");
        addNonUnique(tree, 80, 60, "P4");
        assertFound(tree, 60, 60, 21, 21, "P1,P2,P3,P4");

        assertFalse(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertEquals(4, navigateLeaf(tree, "se").getCount());
        List<XYPointMultiType> collection = (List<XYPointMultiType>) navigateLeaf(tree, "se").getPoints();
        assertEquals(3, collection.size());
        compare(65, 75, "P1", collection.get(0));
        compare(80, 75, "P2", collection.get(1));
        compare(80, 60, "[P3, P4]", collection.get(2));

        addNonUnique(tree, 66, 78, "P5");
        remove(tree, 65, 75, "P1");
        remove(tree, 80, 60, "P3");

        assertEquals(3, navigateLeaf(tree, "se").getCount());
        assertFound(tree, 60, 60, 21, 21, "P2,P4,P5");
        assertEquals(3, collection.size());
        compare(80, 75, "P2", collection.get(0));
        compare(80, 60, "[P4]", collection.get(1));
        compare(66, 78, "P5", collection.get(2));

        remove(tree, 66, 78, "P5");

        assertFound(tree, 60, 60, 21, 21, "P2,P4");
        assertEquals(2, navigateLeaf(tree, "").getCount());
        collection = (List<XYPointMultiType>) navigateLeaf(tree, "").getPoints();
        assertEquals(2, collection.size());
        compare(80, 75, "P2", collection.get(0));
        compare(80, 60, "[P4]", collection.get(1));
    }

    public void testSubdivideMultitypeMerge() {
        PointRegionQuadTree<Object> tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100, 6, 2);
        assertEquals(1, tree.getRoot().getLevel());
        addNonUnique(tree, 10, 10, "P1");
        addNonUnique(tree, 9.9, 10, "P2");
        addNonUnique(tree, 10, 9.9, "P3");
        addNonUnique(tree, 10, 10, "P4");
        addNonUnique(tree, 10, 9.9, "P5");
        addNonUnique(tree, 9.9, 10, "P6");
        assertTrue(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertFound(tree, 9, 10, 1, 1, "P2,P6");
        assertFound(tree, 10, 9, 1, 1, "P3,P5");
        assertFound(tree, 10, 10, 1, 1, "P1,P4");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P3,P4,P5,P6");

        addNonUnique(tree, 10, 10, "P7");

        assertFalse(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertEquals(1, tree.getRoot().getLevel());
        assertEquals(7, navigateLeaf(tree, "nw").getCount());
        List<XYPointMultiType> collection = (List<XYPointMultiType>) navigateLeaf(tree, "nw").getPoints();
        assertEquals(3, collection.size());
        compare(10, 10, "[P1, P4, P7]", collection.get(0));
        compare(9.9, 10, "[P2, P6]", collection.get(1));
        compare(10, 9.9, "[P3, P5]", collection.get(2));
        assertFound(tree, 9, 10, 1, 1, "P2,P6");
        assertFound(tree, 10, 9, 1, 1, "P3,P5");
        assertFound(tree, 10, 10, 1, 1, "P1,P4,P7");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P3,P4,P5,P6,P7");

        addNonUnique(tree, 9.9, 10, "P8");
        addNonUnique(tree, 10, 9.9, "P9");
        addNonUnique(tree, 10, 10, "P10");
        addNonUnique(tree, 10, 10, "P11");
        addNonUnique(tree, 10, 10, "P12");

        assertEquals(12, navigateLeaf(tree, "nw").getCount());
        assertEquals(2, navigateLeaf(tree, "nw").getLevel());
        assertEquals(3, collection.size());
        compare(10, 10, "[P1, P4, P7, P10, P11, P12]", collection.get(0));
        compare(9.9, 10, "[P2, P6, P8]", collection.get(1));
        compare(10, 9.9, "[P3, P5, P9]", collection.get(2));
        assertFound(tree, 9, 10, 1, 1, "P2,P6,P8");
        assertFound(tree, 10, 9, 1, 1, "P3,P5,P9");
        assertFound(tree, 10, 10, 1, 1, "P1,P4,P7,P10,P11,P12");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P3,P4,P5,P6,P7,P8,P9,P10,P11,P12");

        remove(tree, 9.9, 10, "P8");
        remove(tree, 10, 9.9, "P3");
        remove(tree, 10, 9.9, "P5");
        remove(tree, 10, 9.9, "P9");
        assertFound(tree, 9, 10, 1, 1, "P2,P6");
        assertFound(tree, 10, 9, 1, 1, "");
        assertFound(tree, 10, 10, 1, 1, "P1,P4,P7,P10,P11,P12");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P4,P6,P7,P10,P11,P12");

        assertEquals(8, navigateLeaf(tree, "nw").getCount());
        assertEquals(2, collection.size());
        compare(10, 10, "[P1, P4, P7, P10, P11, P12]", collection.get(0));
        compare(9.9, 10, "[P2, P6]", collection.get(1));
        assertFalse(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);

        remove(tree, 9.9, 10, "P2");
        remove(tree, 10, 10, "P1");
        remove(tree, 10, 10, "P10");
        assertTrue(tree.getRoot() instanceof PointRegionQuadTreeNodeLeaf);
        assertEquals(5, navigateLeaf(tree, "").getCount());
        collection = (List<XYPointMultiType>) navigateLeaf(tree, "").getPoints();
        assertEquals(2, collection.size());
        compare(10, 10, "[P4, P7, P11, P12]", collection.get(0));
        compare(9.9, 10, "[P6]", collection.get(1));
        assertFound(tree, 9, 10, 1, 1, "P6");
        assertFound(tree, 10, 9, 1, 1, "");
        assertFound(tree, 10, 10, 1, 1, "P4,P7,P11,P12");
        assertFound(tree, 9, 9, 2, 2, "P4,P6,P7,P11,P12");
    }
}
