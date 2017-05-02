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

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.index.service.AdvancedIndexFactoryProvider;
import com.espertech.esper.epl.index.service.EventAdvancedIndexProvisionDesc;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.epl.lookup.AdvancedIndexDesc;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;

import static com.espertech.esper.epl.index.quadtree.AdvancedIndexPointRegionQuadTree.*;
import static com.espertech.esper.epl.index.service.AdvancedIndexEvaluationHelper.*;
import static com.espertech.esper.epl.index.service.AdvancedIndexValidationHelper.*;

public class AdvancedIndexFactoryProviderPointRegionQuadTree implements AdvancedIndexFactoryProvider {

    public EventAdvancedIndexProvisionDesc validateEventIndex(String indexName, String indexTypeName, ExprNode[] columns, ExprNode[] parameters) throws ExprValidationException {
        validateColumnCount(2, indexTypeName, columns.length);
        validateColumnReturnTypeNumber(indexTypeName, 0, columns[0], AdvancedIndexPointRegionQuadTree.COL_X);
        validateColumnReturnTypeNumber(indexTypeName, 1, columns[1], AdvancedIndexPointRegionQuadTree.COL_Y);

        validateParameters(indexTypeName, parameters);

        AdvancedIndexDesc indexDesc = new AdvancedIndexDesc(indexTypeName, columns);
        ExprEvaluator xEval = indexDesc.getIndexedExpressions()[0].getExprEvaluator();
        ExprEvaluator yEval = indexDesc.getIndexedExpressions()[1].getExprEvaluator();
        AdvancedIndexConfigStatementQuadtree indexStatementConfigs = new AdvancedIndexConfigStatementQuadtree(xEval, yEval);

        return new EventAdvancedIndexProvisionDesc(indexDesc, ExprNodeUtility.getEvaluators(parameters), EventAdvancedIndexFactoryPointRegionQuadTree.INSTANCE, indexStatementConfigs);
    }

    public AdvancedIndexConfigContextPartition validateConfigureFilterIndex(String indexName, String indexTypeName, ExprNode[] parameters, ExprValidationContext validationContext) throws ExprValidationException {
        validateParameters(indexTypeName, parameters);
        try {
            return configureIndex(indexName, ExprNodeUtility.getEvaluators(parameters), validationContext.getExprEvaluatorContext());
        } catch (EPException ex) {
            throw new ExprValidationException(ex.getMessage(), ex);
        }
    }

    protected static AdvancedIndexConfigContextPartition configureIndex(String indexName, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
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

    private static void validateParameters(String indexTypeName, ExprNode[] parameters) throws ExprValidationException {
        validateParameterCount(4, 6, indexTypeName, parameters == null ? 0 : parameters.length);
        validateParameterReturnTypeNumber(indexTypeName, 0, parameters[0], AdvancedIndexPointRegionQuadTree.PARAM_XMIN);
        validateParameterReturnTypeNumber(indexTypeName, 1, parameters[1], AdvancedIndexPointRegionQuadTree.PARAM_YMIN);
        validateParameterReturnTypeNumber(indexTypeName, 2, parameters[2], AdvancedIndexPointRegionQuadTree.PARAM_WIDTH);
        validateParameterReturnTypeNumber(indexTypeName, 3, parameters[3], AdvancedIndexPointRegionQuadTree.PARAM_HEIGHT);
        if (parameters.length > 4) {
            validateParameterReturnType(Integer.class, indexTypeName, 4, parameters[4], AdvancedIndexPointRegionQuadTree.PARAM_LEAFCAPACITY);
        }
        if (parameters.length > 5) {
            validateParameterReturnType(Integer.class, indexTypeName, 5, parameters[5], AdvancedIndexPointRegionQuadTree.PARAM_MAXTREEHEIGHT);
        }
    }
}
