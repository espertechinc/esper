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

import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolUnique;
import com.espertech.esper.spatial.quadtree.core.SupportExecUniqueRandomMovingRectangles;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.MXCIF_FACTORY;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.MXCIF_FI_ADDER;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.MXCIF_FI_QUERIER;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.MXCIF_FI_REMOVER;

public class TestMXCIFQuadTreeFilterIndexRandomMovingPoints extends TestCase {

    public void testIt() {
        SupportQuadTreeToolUnique<MXCIFQuadTree<Object>> tools = new SupportQuadTreeToolUnique<>(MXCIF_FACTORY, null, MXCIF_FI_ADDER, MXCIF_FI_REMOVER, MXCIF_FI_QUERIER, false);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 1, 1);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 10, 10);
        SupportExecUniqueRandomMovingRectangles.runAssertion(tools, 0.1, 0.1);
    }
}
