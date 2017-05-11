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

import com.espertech.esper.spatial.quadtree.core.SupportGeneratorPointNonUniqueInteger;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolNonUnique;
import com.espertech.esper.spatial.quadtree.core.SupportExecNonUniqueRandomMovingRectangles;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.POINTREGION_FACTORY;
import static com.espertech.esper.spatial.quadtree.prqdrowindex.SupportPointRegionQuadTreeRowIndexUtil.*;

public class TestPointRegionQuadTreeRowIndexRandomMovingPointsNonUnique extends TestCase {
    public void testNonUnique() {
        SupportQuadTreeToolNonUnique<PointRegionQuadTree<Object>> tools = new SupportQuadTreeToolNonUnique<>(POINTREGION_FACTORY, SupportGeneratorPointNonUniqueInteger.INSTANCE, POINTREGION_RI_ADDERNONUNIQUE, POINTREGION_RI_REMOVER, POINTREGION_RI_QUERIER, true);
        SupportExecNonUniqueRandomMovingRectangles.runAssertion(tools);
    }
}
