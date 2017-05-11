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

import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolUnique;
import com.espertech.esper.spatial.quadtree.core.SupportExecUniqueRandomMovingRectangles;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.MXCIF_FACTORY;
import static com.espertech.esper.spatial.quadtree.mxcifrowindex.SupportMXCIFQuadTreeRowIndexUtil.MXCIF_RI_ADDERUNIQUE;
import static com.espertech.esper.spatial.quadtree.mxcifrowindex.SupportMXCIFQuadTreeRowIndexUtil.MXCIF_RI_QUERIER;
import static com.espertech.esper.spatial.quadtree.mxcifrowindex.SupportMXCIFQuadTreeRowIndexUtil.MXCIF_RI_REMOVER;

public class TestMXCIFQuadTreeRowIndexRandomMovingPointsUnique extends TestCase {

    public void testUnique() {
        SupportQuadTreeToolUnique<MXCIFQuadTree<Object>> tools = new SupportQuadTreeToolUnique<>(MXCIF_FACTORY, null, MXCIF_RI_ADDERUNIQUE, MXCIF_RI_REMOVER, MXCIF_RI_QUERIER, false);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 5, 5);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 1, 1);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 0.5, 0.5);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 0.99, 0.99);
    }
}
