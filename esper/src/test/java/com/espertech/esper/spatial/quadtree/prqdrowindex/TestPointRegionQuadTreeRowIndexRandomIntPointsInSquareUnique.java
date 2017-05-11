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

import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolUnique;
import com.espertech.esper.spatial.quadtree.core.SupportExecUniqueRandomIntPointsInSquare;
import com.espertech.esper.spatial.quadtree.core.SupportGeneratorPointUniqueByXYInteger;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.POINTREGION_FACTORY;
import static com.espertech.esper.spatial.quadtree.prqdrowindex.SupportPointRegionQuadTreeRowIndexUtil.*;

public class TestPointRegionQuadTreeRowIndexRandomIntPointsInSquareUnique extends TestCase {
    public void testRandomIntPoints() {
        SupportQuadTreeToolUnique<PointRegionQuadTree<Object>> tools = new SupportQuadTreeToolUnique<>(POINTREGION_FACTORY, SupportGeneratorPointUniqueByXYInteger.INSTANCE, POINTREGION_RI_ADDERUNIQUE, POINTREGION_RI_REMOVER, POINTREGION_RI_QUERIER, false);
        SupportExecUniqueRandomIntPointsInSquare.runAssertion(tools);
    }
}
