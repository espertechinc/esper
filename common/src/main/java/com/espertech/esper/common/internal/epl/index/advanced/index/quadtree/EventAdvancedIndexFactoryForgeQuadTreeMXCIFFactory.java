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
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTreeFactory;

public class EventAdvancedIndexFactoryForgeQuadTreeMXCIFFactory extends EventAdvancedIndexFactoryForgeQuadTreeFactory {

    public final static EventAdvancedIndexFactoryForgeQuadTreeMXCIFFactory INSTANCE = new EventAdvancedIndexFactoryForgeQuadTreeMXCIFFactory();

    private EventAdvancedIndexFactoryForgeQuadTreeMXCIFFactory() {
    }

    public EventAdvancedIndexFactoryForge getForge() {
        return EventAdvancedIndexFactoryForgeQuadTreeMXCIFForge.INSTANCE;
    }

    public EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configCP, EventTableOrganization organization) {
        AdvancedIndexConfigContextPartitionQuadTree qt = (AdvancedIndexConfigContextPartitionQuadTree) configCP;
        MXCIFQuadTree<Object> quadTree = MXCIFQuadTreeFactory.make(qt.getX(), qt.getY(), qt.getWidth(), qt.getHeight(), qt.getLeafCapacity(), qt.getMaxTreeHeight());
        return new EventTableQuadTreeMXCIFImpl(organization, (AdvancedIndexConfigStatementMXCIFQuadtree) configStatement, quadTree);
    }

    public EventAdvancedIndexConfigStatementForge toConfigStatement(ExprNode[] indexedExpr) {
        return new AdvancedIndexConfigStatementMXCIFQuadtreeForge(
                indexedExpr[0].getForge(),
                indexedExpr[1].getForge(),
                indexedExpr[2].getForge(),
                indexedExpr[3].getForge());
    }
}
