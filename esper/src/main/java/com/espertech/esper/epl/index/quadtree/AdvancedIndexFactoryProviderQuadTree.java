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
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;

import static com.espertech.esper.epl.index.quadtree.AdvancedIndexQuadTreeConstants.*;
import static com.espertech.esper.epl.index.service.AdvancedIndexEvaluationHelper.*;
import static com.espertech.esper.epl.index.service.AdvancedIndexValidationHelper.*;

public abstract class AdvancedIndexFactoryProviderQuadTree implements AdvancedIndexFactoryProvider {

    public AdvancedIndexConfigContextPartition validateConfigureFilterIndex(String indexName, String indexTypeName, ExprNode[] parameters, ExprValidationContext validationContext) throws ExprValidationException {
        validateParameters(indexTypeName, parameters);
        try {
            return configureQuadTree(indexName, ExprNodeUtilityCore.getEvaluatorsNoCompile(parameters), validationContext.getExprEvaluatorContext());
        } catch (EPException ex) {
            throw new ExprValidationException(ex.getMessage(), ex);
        }
    }

    protected static void validateParameters(String indexTypeName, ExprNode[] parameters) throws ExprValidationException {
        validateParameterCount(4, 6, indexTypeName, parameters == null ? 0 : parameters.length);
        validateParameterReturnTypeNumber(indexTypeName, 0, parameters[0], AdvancedIndexQuadTreeConstants.PARAM_XMIN);
        validateParameterReturnTypeNumber(indexTypeName, 1, parameters[1], AdvancedIndexQuadTreeConstants.PARAM_YMIN);
        validateParameterReturnTypeNumber(indexTypeName, 2, parameters[2], AdvancedIndexQuadTreeConstants.PARAM_WIDTH);
        validateParameterReturnTypeNumber(indexTypeName, 3, parameters[3], AdvancedIndexQuadTreeConstants.PARAM_HEIGHT);
        if (parameters.length > 4) {
            validateParameterReturnType(Integer.class, indexTypeName, 4, parameters[4], AdvancedIndexQuadTreeConstants.PARAM_LEAFCAPACITY);
        }
        if (parameters.length > 5) {
            validateParameterReturnType(Integer.class, indexTypeName, 5, parameters[5], AdvancedIndexQuadTreeConstants.PARAM_MAXTREEHEIGHT);
        }
    }

    protected static AdvancedIndexConfigContextPartition configureQuadTree(String indexName, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
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
        int leafCapacity = parameters.length > 4 ? evalIntParameter(parameters[4], indexName, PARAM_LEAFCAPACITY, exprEvaluatorContext) : PointRegionQuadTreeFactory.DEFAULT_LEAF_CAPACITY;
        if (leafCapacity < 1) {
            throw invalidParameterValue(indexName, PARAM_LEAFCAPACITY, leafCapacity, "value>=1");
        }
        int maxTreeHeight = parameters.length > 5 ? evalIntParameter(parameters[5], indexName, PARAM_MAXTREEHEIGHT, exprEvaluatorContext) : PointRegionQuadTreeFactory.DEFAULT_MAX_TREE_HEIGHT;
        if (maxTreeHeight < 2) {
            throw invalidParameterValue(indexName, PARAM_MAXTREEHEIGHT, maxTreeHeight, "value>=2");
        }
        return new AdvancedIndexConfigContextPartitionQuadTree(x, y, width, height, leafCapacity, maxTreeHeight);
    }
}
