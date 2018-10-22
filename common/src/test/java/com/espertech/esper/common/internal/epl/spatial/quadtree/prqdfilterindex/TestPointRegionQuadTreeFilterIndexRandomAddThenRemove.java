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
package com.espertech.esper.common.internal.epl.spatial.quadtree.prqdfilterindex;

import com.espertech.esper.common.internal.epl.spatial.quadtree.core.SupportGeneratorPointUniqueByXYDouble;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.SupportQuadTreeToolUnique;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTree;
import junit.framework.TestCase;

import static com.espertech.esper.common.internal.epl.spatial.quadtree.core.SupportExecRandomAddThenRemove.runAssertion;
import static com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.SupportPointRegionQuadTreeUtil.POINTREGION_FACTORY;
import static com.espertech.esper.common.internal.epl.spatial.quadtree.prqdfilterindex.SupportPointRegionQuadTreeFilterIndexUtil.*;

public class TestPointRegionQuadTreeFilterIndexRandomAddThenRemove extends TestCase {

    public void testRun() {
        SupportQuadTreeToolUnique<PointRegionQuadTree<Object>> tools = new SupportQuadTreeToolUnique<PointRegionQuadTree<Object>>(POINTREGION_FACTORY, SupportGeneratorPointUniqueByXYDouble.INSTANCE, POINTREGION_FI_ADDERUNIQUE, POINTREGION_FI_REMOVER, POINTREGION_FI_QUERIER, true);
        runAssertion(tools);
    }
}
