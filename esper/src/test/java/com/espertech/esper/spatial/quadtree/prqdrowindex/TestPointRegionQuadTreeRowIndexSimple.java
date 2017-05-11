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

import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.prqdrowindex.SupportPointRegionQuadTreeRowIndexUtil.addNonUnique;
import static com.espertech.esper.spatial.quadtree.prqdrowindex.SupportPointRegionQuadTreeRowIndexUtil.assertFound;

public class TestPointRegionQuadTreeRowIndexSimple extends TestCase {
    private PointRegionQuadTree<Object> tree;

    public void tearDown() {
        tree = null;
    }

    public void testAddRemoveSimple() {
        tree = PointRegionQuadTreeFactory.make(0, 0, 50, 60, 4, 20);
        assertFound(tree, 0, 0, 10, 10, "");

        addNonUnique(tree, 5, 8, "P1");
        assertFound(tree, 0, 0, 10, 10, "P1");
        assertFound(tree, 0, 0, 5, 5, "");

        SupportPointRegionQuadTreeRowIndexUtil.remove(tree, 5, 8, "P1");
        assertFound(tree, 0, 0, 10, 10, "");
    }

    public void testPoints() {
        tree = PointRegionQuadTreeFactory.make(0, 0, 10, 10);

        addNonUnique(tree, 8.0, 4.0, "P0");
        assertFound(tree, 0, 0, 10, 10, "P0");

        addNonUnique(tree, 8.0, 1.0, "P1");
        assertFound(tree, 0, 0, 10, 10, "P0,P1");

        addNonUnique(tree, 8.0, 2.0, "P2");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2");

        addNonUnique(tree, 4.0, 4.0, "P3");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3");

        addNonUnique(tree, 1.0, 9.0, "P4");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3,P4");

        addNonUnique(tree, 8.0, 3.0, "P5");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3,P4,P5");

        addNonUnique(tree, 0.0, 6.0, "P6");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3,P4,P5,P6");

        addNonUnique(tree, 5.0, 1.0, "P7");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3,P4,P5,P6,P7");

        addNonUnique(tree, 5.0, 8.0, "P8");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3,P4,P5,P6,P7,P8");

        addNonUnique(tree, 7.0, 6.0, "P9");
        assertFound(tree, 0, 0, 10, 10, "P0,P1,P2,P3,P4,P5,P6,P7,P8,P9");
    }

    public void testAddRemoveSamePoint() {
        tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100);

        addNonUnique(tree, 5, 8, "P1");
        addNonUnique(tree, 5, 8, "P2");
        assertFound(tree, 0, 0, 10, 10, "P1,P2");

        SupportPointRegionQuadTreeRowIndexUtil.remove(tree, 5, 8, "P1");
        assertFound(tree, 0, 0, 10, 10, "P2");

        SupportPointRegionQuadTreeRowIndexUtil.remove(tree, 5, 8, "P2");
        assertFound(tree, 0, 0, 10, 10, "");
    }

    public void testFewValues() {
        tree = PointRegionQuadTreeFactory.make(0, 0, 100, 100);

        addNonUnique(tree, 73.32704983331149, 23.46990952575032, "P0");
        addNonUnique(tree, 53.09747562396894, 17.100976152185034, "P1");
        addNonUnique(tree, 56.75757294858788, 25.508506696809608, "P2");
        addNonUnique(tree, 83.66639067675291, 76.53772974832937, "P3");
        addNonUnique(tree, 51.01654641861326, 43.49009281983866, "P4");

        double beginX = 50.45945198254618;
        double endX = 88.31594559038719;

        double beginY = 4.577595744501329;
        double endY = 22.93393078279351;

        assertFound(tree, beginX, beginY, endX - beginX, endY - beginY, "P1");
    }
}
