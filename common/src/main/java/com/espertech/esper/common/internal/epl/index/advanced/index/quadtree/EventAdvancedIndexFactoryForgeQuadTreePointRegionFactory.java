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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatement;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatementForge;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexFactoryForge;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;

public class EventAdvancedIndexFactoryForgeQuadTreePointRegionFactory extends EventAdvancedIndexFactoryForgeQuadTreeFactory {

    public final static EventAdvancedIndexFactoryForgeQuadTreePointRegionFactory INSTANCE = new EventAdvancedIndexFactoryForgeQuadTreePointRegionFactory();

    private EventAdvancedIndexFactoryForgeQuadTreePointRegionFactory() {
    }

    public EventAdvancedIndexFactoryForge getForge() {
        return EventAdvancedIndexFactoryForgeQuadTreePointRegionForge.INSTANCE;
    }

    public EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configCP, EventTableOrganization organization) {
        AdvancedIndexConfigContextPartitionQuadTree qt = (AdvancedIndexConfigContextPartitionQuadTree) configCP;
        PointRegionQuadTree<Object> quadTree = PointRegionQuadTreeFactory.make(qt.getX(), qt.getY(), qt.getWidth(), qt.getHeight(), qt.getLeafCapacity(), qt.getMaxTreeHeight());
        return new EventTableQuadTreePointRegionImpl(organization, (AdvancedIndexConfigStatementPointRegionQuadtree) configStatement, quadTree);
    }

    public EventAdvancedIndexConfigStatementForge toConfigStatement(ExprNode[] indexedExpr) {
        return new AdvancedIndexConfigStatementPointRegionQuadtreeForge(indexedExpr[0].getForge(),
                indexedExpr[1].getForge());
    }
}
