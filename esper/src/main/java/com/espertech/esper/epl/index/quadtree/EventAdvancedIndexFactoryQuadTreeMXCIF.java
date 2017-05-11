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
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeFactory;

import java.util.Map;

public class EventAdvancedIndexFactoryQuadTreeMXCIF extends EventAdvancedIndexFactoryQuadTree {

    public final static EventAdvancedIndexFactoryQuadTreeMXCIF INSTANCE = new EventAdvancedIndexFactoryQuadTreeMXCIF();

    private EventAdvancedIndexFactoryQuadTreeMXCIF() {}

    public boolean providesIndexForOperation(String operationName, Map<Integer, ExprNode> value) {
        return operationName.equals(EngineImportApplicationDotMethodRectangeIntersectsRectangle.LOOKUP_OPERATION_NAME);
    }

    public EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configCP, EventTableOrganization organization) {
        AdvancedIndexConfigContextPartitionQuadTree qt = (AdvancedIndexConfigContextPartitionQuadTree) configCP;
        MXCIFQuadTree<Object> quadTree = MXCIFQuadTreeFactory.make(qt.getX(), qt.getY(), qt.getWidth(), qt.getHeight(), qt.getLeafCapacity(), qt.getMaxTreeHeight());
        return new EventTableQuadTreeMXCIFImpl(organization, (AdvancedIndexConfigStatementMXCIFQuadtree) configStatement, quadTree);
    }
}
