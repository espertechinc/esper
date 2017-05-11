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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableOrganization;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.epl.lookup.EventAdvancedIndexConfigStatement;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;

import java.util.Map;

public class EventAdvancedIndexFactoryQuadTreePointRegion extends EventAdvancedIndexFactoryQuadTree {

    public final static EventAdvancedIndexFactoryQuadTreePointRegion INSTANCE = new EventAdvancedIndexFactoryQuadTreePointRegion();

    private EventAdvancedIndexFactoryQuadTreePointRegion() {}

    public boolean providesIndexForOperation(String operationName, Map<Integer, ExprNode> value) {
        return operationName.equals(EngineImportApplicationDotMethodPointInsideRectange.LOOKUP_OPERATION_NAME);
    }

    public EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configCP, EventTableOrganization organization) {
        AdvancedIndexConfigContextPartitionQuadTree qt = (AdvancedIndexConfigContextPartitionQuadTree) configCP;
        PointRegionQuadTree<Object> quadTree = PointRegionQuadTreeFactory.make(qt.getX(), qt.getY(), qt.getWidth(), qt.getHeight(), qt.getLeafCapacity(), qt.getMaxTreeHeight());
        return new EventTableQuadTreePointRegionImpl(organization, (AdvancedIndexConfigStatementPointRegionQuadtree) configStatement, quadTree);
    }
}
