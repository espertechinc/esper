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

import static com.espertech.esper.epl.index.quadtree.AdvancedIndexPointRegionQuadTree.*;
import static com.espertech.esper.epl.index.service.AdvancedIndexEvaluationHelper.*;

public class AdvancedIndexFactoryPointRegionQuadTree implements AdvancedIndexFactory {

    public final static AdvancedIndexFactoryPointRegionQuadTree INSTANCE = new AdvancedIndexFactoryPointRegionQuadTree();

    private AdvancedIndexFactoryPointRegionQuadTree() {
    }

    public boolean providesIndexForOperation(String operationName, Map<Integer, ExprNode> value) {
        return operationName.equals(EngineImportApplicationDotMethodPointInsideRectange.LOOKUP_OPERATION_NAME);
    }

    public AdvancedIndexConfigContextPartition configureContextPartition(EventType eventType, AdvancedIndexDesc indexDesc, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext, EventTableOrganization organization, AdvancedIndexConfigStatement advancedIndexConfigStatement) {
        String indexName = organization.getIndexName();
        double x = evalDoubleParameter(parameters[0], indexName, PARAM_XMIN, exprEvaluatorContext);
        double y = evalDoubleParameter(parameters[1], indexName, PARAM_YMIN, exprEvaluatorContext);
        double width = evalDoubleParameter(parameters[2], indexName, PARAM_WIDTH, exprEvaluatorContext);
        if (width <= 0) {
            throw invalidParameterValue(indexName, PARAM_WIDTH, width, "value>0");
        }
        double height = evalDoubleParameter(parameters[3], indexName, PARAM_HEIGHT, exprEvaluatorContext);
        if (height <= 0) {
            throw invalidParameterValue(indexName, PARAM_HEIGHT, height, "value>0");
        }
        int leafCapacity = parameters.length > 4 ? evalIntParameter(parameters[4], indexName, PARAM_LEAFCAPACITY, exprEvaluatorContext) : QuadTreeFactory.DEFAULT_LEAF_CAPACITY;
        if (leafCapacity < 1) {
            throw invalidParameterValue(indexName, PARAM_LEAFCAPACITY, leafCapacity, "value>=1");
        }
        int maxTreeHeight = parameters.length > 5 ? evalIntParameter(parameters[5], indexName, PARAM_MAXTREEHEIGHT, exprEvaluatorContext) : QuadTreeFactory.DEFAULT_MAX_TREE_HEIGHT;
        if (maxTreeHeight < 2) {
            throw invalidParameterValue(indexName, PARAM_MAXTREEHEIGHT, maxTreeHeight, "value>=2");
        }
        return new AdvancedIndexConfigContextPartitionQuadTree(x, y, width, height, leafCapacity, maxTreeHeight);
    }

    public EventTable make(AdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configCP, EventTableOrganization organization) {
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
