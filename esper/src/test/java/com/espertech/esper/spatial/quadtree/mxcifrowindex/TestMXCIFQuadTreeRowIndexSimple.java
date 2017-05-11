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
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.mxcifrowindex.SupportMXCIFQuadTreeRowIndexUtil.*;

public class TestMXCIFQuadTreeRowIndexSimple extends TestCase {
    private MXCIFQuadTree<Object> tree;

    public void tearDown() {
        tree = null;
    }

    public void testAddRemoveSimple() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 50, 60);
        assertFound(tree, 0, 0, 10, 10, "");

        addNonUnique(tree, 5, 8, 1, 1, "R1");
        assertFound(tree, 0, 0, 10, 10, "R1");
        assertFound(tree, 0, 0, 5, 5, "");

        MXCIFQuadTreeRowIndexRemove.remove(5, 8, 1, 1, "R1", tree);
        assertFound(tree, 0, 0, 10, 10, "");
    }

    public void testPoints() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 10, 10);

        addNonUnique(tree, 8.0, 4.0, 1, 1, "R0");
        assertFound(tree, 0, 0, 10, 10, "R0");

        addNonUnique(tree, 8.0, 1.0, 1, 1, "R1");
        assertFound(tree, 0, 0, 10, 10, "R0,R1");

        addNonUnique(tree, 8.0, 2.0, 1, 1, "R2");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2");

        addNonUnique(tree, 4.0, 4.0, 1, 1, "R3");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3");

        addNonUnique(tree, 1.0, 9.0, 1, 1, "R4");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3,R4");

        addNonUnique(tree, 8.0, 3.0, 1, 1, "R5");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3,R4,R5");

        addNonUnique(tree, 0.0, 6.0, 1, 1, "R6");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3,R4,R5,R6");

        addNonUnique(tree, 5.0, 1.0, 1, 1, "R7");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3,R4,R5,R6,R7");

        addNonUnique(tree, 5.0, 8.0, 1, 1, "R8");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3,R4,R5,R6,R7,R8");

        addNonUnique(tree, 7.0, 6.0, 1, 1, "R9");
        assertFound(tree, 0, 0, 10, 10, "R0,R1,R2,R3,R4,R5,R6,R7,R8,R9");
    }

    public void testAddRemoveSamePoint() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100);

        addNonUnique(tree, 5, 8, 1, 1, "R1");
        addNonUnique(tree, 5, 8, 1, 1, "R2");
        assertFound(tree, 0, 0, 10, 10, "R1,R2");

        MXCIFQuadTreeRowIndexRemove.remove(5, 8, 1, 1, "R1", tree);
        assertFound(tree, 0, 0, 10, 10, "R2");

        MXCIFQuadTreeRowIndexRemove.remove(5, 8, 1, 1, "R2", tree);
        assertFound(tree, 0, 0, 10, 10, "");
    }

    public void testFewValues() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100);

        addNonUnique(tree, 73.32704983331149, 23.46990952575032, 1, 1, "R0");
        addNonUnique(tree, 53.09747562396894, 17.100976152185034, 1, 1, "R1");
        addNonUnique(tree, 56.75757294858788, 25.508506696809608, 1, 1, "R2");
        addNonUnique(tree, 83.66639067675291, 76.53772974832937, 1, 1, "R3");
        addNonUnique(tree, 51.01654641861326, 43.49009281983866, 1, 1, "R4");

        double beginX = 50.45945198254618;
        double endX = 88.31594559038719;

        double beginY = 4.577595744501329;
        double endY = 22.93393078279351;

        assertFound(tree, beginX, beginY, endX - beginX, endY - beginY, "R1");
    }

    public void testAddRemoveScenario() {
        tree = MXCIFQuadTreeFactory.make(0, 0, 100, 100);

        addUnique(tree, 85.0, 65.0, 0.999, 0.999, "P3");
        addUnique(tree, 86.0, 50.0, 0.999, 0.999, "P6");
        addUnique(tree, 17.0, 84.0, 0.999, 0.999, "P0");
        addUnique(tree, 7.0, 34.0, 0.999, 0.999, "P4");
        addUnique(tree, 7.0, 69.0, 0.999, 0.999, "P8");
        addUnique(tree, 36.0, 47.0, 0.999, 0.999, "P9");
        addUnique(tree, 62.0, 50.0, 0.999, 0.999, "P1");
        addUnique(tree, 46.0, 17.0, 0.999, 0.999, "P2");
        addUnique(tree, 43.0, 16.0, 0.999, 0.999, "P5");
        addUnique(tree, 79.0, 92.0, 0.999, 0.999, "P7");
        remove(tree, 46.0, 17.0, 0.999, 0.999, "P2");
        addUnique(tree, 47.0, 17.0, 0.999, 0.999, "P2");
        remove(tree, 43.0, 16.0, 0.999, 0.999, "P5");
        addUnique(tree, 44.0, 16.0, 0.999, 0.999, "P5");
        remove(tree, 62.0, 50.0, 0.999, 0.999, "P1");
        addUnique(tree, 62.0, 49.0, 0.999, 0.999, "P1");
        remove(tree, 17.0, 84.0, 0.999, 0.999, "P0");
        addUnique(tree, 18.0, 84.0, 0.999, 0.999, "P0");
        remove(tree, 86.0, 50.0, 0.999, 0.999, "P6");
        addUnique(tree, 86.0, 51.0, 0.999, 0.999, "P6");
        assertFound(tree, 81.0, 46.0, 10.0, 10.0, "P6");
    }
}
