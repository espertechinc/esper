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

import com.espertech.esper.spatial.quadtree.core.SupportGeneratorRectangleNonUniqueIntersecting;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolNonUnique;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.core.SupportExecNonUniqueRandomMovingRectangles.runAssertion;
import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.MXCIF_FACTORY;
import static com.espertech.esper.spatial.quadtree.mxcifrowindex.SupportMXCIFQuadTreeRowIndexUtil.*;

public class TestMXCIFQuadTreeRowIndexRandomMovingPointsNonUnique extends TestCase {

    public void testNonUnique() {
        SupportQuadTreeToolNonUnique<MXCIFQuadTree<Object>> tools = new SupportQuadTreeToolNonUnique<>(MXCIF_FACTORY, SupportGeneratorRectangleNonUniqueIntersecting.INSTANCE, MXCIF_RI_ADDERNONUNIQUE, MXCIF_RI_REMOVER, MXCIF_RI_QUERIER, false);
        runAssertion(tools);
    }
}
