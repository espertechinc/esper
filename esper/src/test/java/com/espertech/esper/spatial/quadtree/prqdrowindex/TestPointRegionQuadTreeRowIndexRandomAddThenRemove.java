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

import com.espertech.esper.spatial.quadtree.core.SupportGeneratorPointNonUniqueDouble;
import com.espertech.esper.spatial.quadtree.core.SupportGeneratorPointUniqueByXYDouble;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeToolUnique;
import com.espertech.esper.spatial.quadtree.core.SupportExecRandomAddThenRemove;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.POINTREGION_FACTORY;
import static com.espertech.esper.spatial.quadtree.prqdrowindex.SupportPointRegionQuadTreeRowIndexUtil.*;

public class TestPointRegionQuadTreeRowIndexRandomAddThenRemove extends TestCase {

    public void testRun() {
        SupportQuadTreeToolUnique<PointRegionQuadTree<Object>> toolsOne = new SupportQuadTreeToolUnique<>(POINTREGION_FACTORY, SupportGeneratorPointNonUniqueDouble.INSTANCE, POINTREGION_RI_ADDERUNIQUE, POINTREGION_RI_REMOVER, POINTREGION_RI_QUERIER, true);
        SupportExecRandomAddThenRemove.runAssertion(toolsOne);

        SupportQuadTreeToolUnique<PointRegionQuadTree<Object>> toolsTwo = new SupportQuadTreeToolUnique<>(POINTREGION_FACTORY, SupportGeneratorPointUniqueByXYDouble.INSTANCE, POINTREGION_RI_ADDERUNIQUE, POINTREGION_RI_REMOVER, POINTREGION_RI_QUERIER, true);
        SupportExecRandomAddThenRemove.runAssertion(toolsTwo);
    }
}
