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
package com.espertech.esper.spatial.quadtree.mxcifrowindex;

import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeFactory;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;
import junit.framework.TestCase;

import java.util.List;

import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.navigateBranch;
import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.navigateLeaf;
import static com.espertech.esper.spatial.quadtree.mxcifrowindex.SupportMXCIFQuadTreeRowIndexUtil.*;

public class TestMXCIFQuadTreeRowIndexScenarios extends TestCase {

    public void testSubdivideAdd() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 2, 3);
        addNonUnique(tree, 0, 0, 10, 10, "R1");
        addNonUnique(tree, 0, 0, 10, 10, "R2");
        addNonUnique(tree, 0, 0, 10, 10, "R3");
        assertEquals(3, navigateLeaf(tree, "nw,nw").getCount());
    }

    public void testDimension() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(1000, 100000, 9000, 900000);
        assertFalse(addNonUnique(tree, 10, 90, 1, 1, "R1"));
        assertFalse(addNonUnique(tree, 10999999, 90, 1, 1, "R2"));
        assertTrue(addNonUnique(tree, 5000, 800000, 1, 1, "R3"));

        assertFound(tree, 0, 0, 10000000, 10000000, "R3");
        assertFound(tree, 4000, 790000, 1200, 11000, "R3");
        assertFound(tree, 4000, 790000, 900, 9000, "");
    }

    public void testSuperslim() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 1, 100);
        addNonUnique(tree, 10, 90, 1, 1, "R1");
        addNonUnique(tree, 10, 95, 1, 1, "R2");
        MXCIFQuadTreeNodeLeaf<Object> ne = navigateLeaf(tree, "sw,sw,sw,ne");
        compare(10, 90, 1, 1, "R1", (XYWHRectangleMultiType) ne.getData());
        MXCIFQuadTreeNodeLeaf<Object> se = navigateLeaf(tree, "sw,sw,sw,se");
        compare(10, 95, 1, 1, "R2", (XYWHRectangleMultiType) se.getData());
    }

    public void testSubdivideMultiChild() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 4, 3);
        addNonUnique(tree, 60, 10, 1, 1, "R1");
        addNonUnique(tree, 60, 40, 1, 1, "R2");
        addNonUnique(tree, 70, 30, 1, 1, "R3");
        addNonUnique(tree, 60, 10, 1, 1, "R4");
        addNonUnique(tree, 90, 45, 1, 1, "R5");

        navigateLeaf(tree, "nw");
        navigateLeaf(tree, "se");
        navigateLeaf(tree, "sw");
        MXCIFQuadTreeNodeBranch<Object> ne = navigateBranch(tree, "ne");
        assertEquals(2, ne.getLevel());

        MXCIFQuadTreeNodeLeaf<Object> nw = navigateLeaf(ne, "nw");
        compare(60, 10, 1, 1, "[R1, R4]", (XYWHRectangleMultiType) nw.getData());
        assertEquals(2, nw.getCount());

        MXCIFQuadTreeNodeLeaf<Object> se = navigateLeaf(ne, "se");
        compare(90, 45, 1, 1, "R5", (XYWHRectangleMultiType) se.getData());
        assertEquals(1, se.getCount());

        MXCIFQuadTreeNodeLeaf<Object> sw = navigateLeaf(ne, "sw");
        List<XYWHRectangleMultiType> collection = (List<XYWHRectangleMultiType>) sw.getData();
        compare(60, 40, 1, 1, "R2", collection.get(0));
        compare(70, 30, 1, 1, "R3", collection.get(1));
        assertEquals(2, sw.getCount());

        remove(tree, 60, 10, 1, 1, "R1");
        remove(tree, 60, 40, 1, 1, "R2");

        MXCIFQuadTreeNodeLeaf<Object> root = navigateLeaf(tree, "");
        collection = (List<XYWHRectangleMultiType>) root.getData();
        assertEquals(3, root.getCount());
        assertEquals(3, collection.size());
        compare(60, 10, 1, 1, "[R4]", collection.get(0));
        compare(70, 30, 1, 1, "R3", collection.get(1));
        compare(90, 45, 1, 1, "R5", collection.get(2));
    }

    public void testRemoveNonExistent() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 20, 20);
        remove(tree, 10, 61, 1, 1, "R1");
        addNonUnique(tree, 10, 60, 1, 1, "R1");
        remove(tree, 10, 61, 1, 1, "R1");
        remove(tree, 10, 60, 2, 1, "R1");
        remove(tree, 10, 60, 1, 2, "R1");
        remove(tree, 11, 60, 1, 1, "R1");
        addNonUnique(tree, 10, 80, 1, 1, "R2");
        addNonUnique(tree, 20, 70, 1, 1, "R3");
        addNonUnique(tree, 10, 80, 1, 1, "R4");
        assertEquals(4, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "R1,R2,R3,R4");

        remove(tree, 10, 61, 1, 1, "R1");
        remove(tree, 9, 60, 1, 1, "R1");
        remove(tree, 10, 60, 1, 1, "R2");
        remove(tree, 10, 80, 1, 1, "R1");
        assertEquals(4, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "R1,R2,R3,R4");

        remove(tree, 10, 80, 1, 1, "R4");
        assertEquals(3, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "R1,R2,R3");

        remove(tree, 10, 80, 1, 1, "R2");
        assertEquals(2, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "R1,R3");

        remove(tree, 10, 60, 1, 1, "R1");
        assertEquals(1, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "R3");

        remove(tree, 20, 70, 1, 1, "R3");
        assertEquals(0, navigateLeaf(tree, "").getCount());
        assertFound(tree, 10, 60, 10000, 10000, "");
    }

    public void testSubdivideSingleMerge() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 3, 2);
        addNonUnique(tree, 65, 75, 1, 1, "P1");
        addNonUnique(tree, 80, 75, 1, 1, "P2");
        addNonUnique(tree, 80, 60, 1, 1, "P3");
        addNonUnique(tree, 80, 60, 1, 1, "P4");
        assertFound(tree, 60, 60, 21, 21, "P1,P2,P3,P4");

        assertFalse(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertEquals(4, navigateLeaf(tree, "se").getCount());
        List<XYWHRectangleMultiType> collection = (List<XYWHRectangleMultiType>) navigateLeaf(tree, "se").getData();
        assertEquals(3, collection.size());
        compare(65, 75, 1, 1, "P1", collection.get(0));
        compare(80, 75, 1, 1, "P2", collection.get(1));
        compare(80, 60, 1, 1, "[P3, P4]", collection.get(2));

        addNonUnique(tree, 66, 78, 1, 1, "P5");
        remove(tree, 65, 75, 1, 1, "P1");
        remove(tree, 80, 60, 1, 1, "P3");

        assertEquals(3, navigateLeaf(tree, "se").getCount());
        assertFound(tree, 60, 60, 21, 21, "P2,P4,P5");
        assertEquals(3, collection.size());
        compare(80, 75, 1, 1, "P2", collection.get(0));
        compare(80, 60, 1, 1, "[P4]", collection.get(1));
        compare(66, 78, 1, 1, "P5", collection.get(2));

        remove(tree, 66, 78, 1, 1, "P5");

        assertFound(tree, 60, 60, 21, 21, "P2,P4");
        assertEquals(2, navigateLeaf(tree, "").getCount());
        collection = (List<XYWHRectangleMultiType>) navigateLeaf(tree, "").getData();
        assertEquals(2, collection.size());
        compare(80, 75, 1, 1, "P2", collection.get(0));
        compare(80, 60, 1, 1, "[P4]", collection.get(1));
    }

    public void testSubdivideMultitypeMerge() {
        MXCIFQuadTree<Object> tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100, 6, 2);
        assertEquals(1, tree.getRoot().getLevel());
        addNonUnique(tree, 10, 10, 0, 0,"P1");
        addNonUnique(tree, 9.9, 10, 0, 0,"P2");
        addNonUnique(tree, 10, 9.9, 0, 0,"P3");
        addNonUnique(tree, 10, 10, 0, 0,"P4");
        addNonUnique(tree, 10, 9.9, 0, 0,"P5");
        addNonUnique(tree, 9.9, 10, 0, 0,"P6");
        assertTrue(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertFound(tree, 9, 10, 0.99, 0.99, "P2,P6");
        assertFound(tree, 10, 9, 0.99, 0.99, "P3,P5");
        assertFound(tree, 10, 10, 0, 0,"P1,P4");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P3,P4,P5,P6");

        addNonUnique(tree, 10, 10, 0, 0,"P7");

        assertFalse(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertEquals(1, tree.getRoot().getLevel());
        assertEquals(7, navigateLeaf(tree, "nw").getCount());
        List<XYWHRectangleMultiType> collection = (List<XYWHRectangleMultiType>) navigateLeaf(tree, "nw").getData();
        assertEquals(3, collection.size());
        compare(10, 10, 0, 0,"[P1, P4, P7]", collection.get(0));
        compare(9.9, 10, 0, 0,"[P2, P6]", collection.get(1));
        compare(10, 9.9, 0, 0,"[P3, P5]", collection.get(2));
        assertFound(tree, 9, 10, 0.99, 0.99,"P2,P6");
        assertFound(tree, 10, 9, 0.99, 0.99,"P3,P5");
        assertFound(tree, 10, 10, 0, 0,"P1,P4,P7");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P3,P4,P5,P6,P7");

        addNonUnique(tree, 9.9, 10, 0, 0,"P8");
        addNonUnique(tree, 10, 9.9, 0, 0,"P9");
        addNonUnique(tree, 10, 10, 0, 0,"P10");
        addNonUnique(tree, 10, 10, 0, 0,"P11");
        addNonUnique(tree, 10, 10, 0, 0,"P12");

        assertEquals(12, navigateLeaf(tree, "nw").getCount());
        assertEquals(2, navigateLeaf(tree, "nw").getLevel());
        assertEquals(3, collection.size());
        compare(10, 10, 0, 0,"[P1, P4, P7, P10, P11, P12]", collection.get(0));
        compare(9.9, 10, 0, 0,"[P2, P6, P8]", collection.get(1));
        compare(10, 9.9, 0, 0,"[P3, P5, P9]", collection.get(2));
        assertFound(tree, 9, 10, 0.99, 0.99,"P2,P6,P8");
        assertFound(tree, 10, 9, 0.99, 0.99,"P3,P5,P9");
        assertFound(tree, 10, 10, 1, 1,"P1,P4,P7,P10,P11,P12");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P3,P4,P5,P6,P7,P8,P9,P10,P11,P12");

        remove(tree, 9.9, 10, 0, 0,"P8");
        remove(tree, 10, 9.9, 0, 0,"P3");
        remove(tree, 10, 9.9, 0, 0,"P5");
        remove(tree, 10, 9.9, 0, 0,"P9");
        assertFound(tree, 9, 10, 0.99, 0.99,"P2,P6");
        assertFound(tree, 10, 9, 0.99, 0.99,"");
        assertFound(tree, 10, 10, 1, 1,"P1,P4,P7,P10,P11,P12");
        assertFound(tree, 9, 9, 2, 2, "P1,P2,P4,P6,P7,P10,P11,P12");

        assertEquals(8, navigateLeaf(tree, "nw").getCount());
        assertEquals(2, collection.size());
        compare(10, 10, 0, 0,"[P1, P4, P7, P10, P11, P12]", collection.get(0));
        compare(9.9, 10, 0, 0,"[P2, P6]", collection.get(1));
        assertFalse(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);

        remove(tree, 9.9, 10, 0, 0,"P2");
        remove(tree, 10, 10, 0, 0,"P1");
        remove(tree, 10, 10, 0, 0,"P10");
        assertTrue(tree.getRoot() instanceof MXCIFQuadTreeNodeLeaf);
        assertEquals(5, navigateLeaf(tree, "").getCount());
        collection = (List<XYWHRectangleMultiType>) navigateLeaf(tree, "").getData();
        assertEquals(2, collection.size());
        compare(10, 10, 0, 0,"[P4, P7, P11, P12]", collection.get(0));
        compare(9.9, 10, 0, 0,"[P6]", collection.get(1));
        assertFound(tree, 9, 10, 0.99, 0.99,"P6");
        assertFound(tree, 10, 9, 0.99, 0.99,"");
        assertFound(tree, 10, 10, 1, 1,"P4,P7,P11,P12");
        assertFound(tree, 9, 9, 2, 2, "P4,P6,P7,P11,P12");
    }
}
