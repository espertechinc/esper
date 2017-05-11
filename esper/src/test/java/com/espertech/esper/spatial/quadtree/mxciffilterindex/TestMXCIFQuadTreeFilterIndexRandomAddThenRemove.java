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

import com.espertech.esper.spatial.quadtree.core.SupportExecRandomAddThenRemove;
import com.espertech.esper.spatial.quadtree.core.SupportGeneratorPointUniqueByXYDouble;
import com.espertech.esper.spatial.quadtree.core.SupportGeneratorRectangleUniqueByXYWH;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolUnique;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.mxcif.SupportMXCIFQuadTreeUtil.MXCIF_FACTORY;
import static com.espertech.esper.spatial.quadtree.mxciffilterindex.SupportMXCIFQuadTreeFilterIndexUtil.*;

public class TestMXCIFQuadTreeFilterIndexRandomAddThenRemove extends TestCase {

    public void testRun() {
        SupportQuadTreeToolUnique<MXCIFQuadTree<Object>> toolsOne = new SupportQuadTreeToolUnique<>(MXCIF_FACTORY, SupportGeneratorPointUniqueByXYDouble.INSTANCE, MXCIF_FI_ADDER, MXCIF_FI_REMOVER, MXCIF_FI_QUERIER, false);
        SupportExecRandomAddThenRemove.runAssertion(toolsOne);

        SupportQuadTreeToolUnique<MXCIFQuadTree<Object>> toolsTwo = new SupportQuadTreeToolUnique<>(MXCIF_FACTORY, SupportGeneratorRectangleUniqueByXYWH.INSTANCE, MXCIF_FI_ADDER, MXCIF_FI_REMOVER, MXCIF_FI_QUERIER, false);
        SupportExecRandomAddThenRemove.runAssertion(toolsTwo);
    }
}
