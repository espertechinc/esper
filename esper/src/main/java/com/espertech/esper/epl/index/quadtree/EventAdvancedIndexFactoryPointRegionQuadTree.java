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

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableOrganization;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;

import java.util.Map;

import static com.espertech.esper.epl.index.quadtree.AdvancedIndexFactoryProviderPointRegionQuadTree.configureIndex;

public class EventAdvancedIndexFactoryPointRegionQuadTree implements EventAdvancedIndexFactory {

    public final static EventAdvancedIndexFactoryPointRegionQuadTree INSTANCE = new EventAdvancedIndexFactoryPointRegionQuadTree();

    private EventAdvancedIndexFactoryPointRegionQuadTree() {
    }

    public boolean providesIndexForOperation(String operationName, Map<Integer, ExprNode> value) {
        return operationName.equals(EngineImportApplicationDotMethodPointInsideRectange.LOOKUP_OPERATION_NAME);
    }

    public AdvancedIndexConfigContextPartition configureContextPartition(EventType eventType, AdvancedIndexDesc indexDesc, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext, EventTableOrganization organization, EventAdvancedIndexConfigStatement advancedIndexConfigStatement) {
        String indexName = organization.getIndexName();
        return configureIndex(indexName, parameters, exprEvaluatorContext);
    }

    public EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configCP, EventTableOrganization organization) {
        AdvancedIndexConfigContextPartitionQuadTree qt = (AdvancedIndexConfigContextPartitionQuadTree) configCP;
        QuadTree<Object> quadTree = QuadTreeFactory.make(qt.getX(), qt.getY(), qt.getWidth(), qt.getHeight(), qt.getLeafCapacity(), qt.getMaxTreeHeight());
        return new EventTablePointRegionQuadTreeImpl(organization, (AdvancedIndexConfigStatementQuadtree) configStatement, quadTree);
    }

    public SubordTableLookupStrategyFactoryQuadTree getSubordinateLookupStrategy(String operationName, Map<Integer, ExprNode> positionalExpressions, boolean isNWOnTrigger, int numOuterstreams) {
        ExprEvaluator x = positionalExpressions.get(0).getExprEvaluator();
        ExprEvaluator y = positionalExpressions.get(1).getExprEvaluator();
        ExprEvaluator width = positionalExpressions.get(2).getExprEvaluator();
        ExprEvaluator height = positionalExpressions.get(3).getExprEvaluator();
        String[] expressions = new String[positionalExpressions.size()];
        for (Map.Entry<Integer, ExprNode> entry : positionalExpressions.entrySet()) {
            expressions[entry.getKey()] = ExprNodeUtility.toExpressionStringMinPrecedenceSafe(entry.getValue());
        }
        LookupStrategyDesc lookupStrategyDesc = new LookupStrategyDesc(LookupStrategyType.ADVANCED, expressions);
        return new SubordTableLookupStrategyFactoryQuadTree(x, y, width, height, isNWOnTrigger, numOuterstreams, lookupStrategyDesc);
    }
}
