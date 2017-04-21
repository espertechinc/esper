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

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.index.service.AdvancedIndexFactoryProvider;
import com.espertech.esper.epl.index.service.AdvancedIndexProvisionDesc;
import com.espertech.esper.epl.lookup.AdvancedIndexDesc;

import static com.espertech.esper.epl.index.service.AdvancedIndexValidationHelper.*;

public class AdvancedIndexFactoryProviderPointRegionQuadTree implements AdvancedIndexFactoryProvider {

    public AdvancedIndexProvisionDesc validate(String indexName, String indexTypeName, boolean unique, ExprNode[] columns, ExprNode[] parameters) throws ExprValidationException {
        validateColumnCount(2, indexTypeName, columns.length);
        validateColumnReturnTypeNumber(indexTypeName, 0, columns[0], AdvancedIndexPointRegionQuadTree.COL_X);
        validateColumnReturnTypeNumber(indexTypeName, 1, columns[1], AdvancedIndexPointRegionQuadTree.COL_Y);

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

        AdvancedIndexDesc indexDesc = new AdvancedIndexDesc(indexTypeName, columns);

        ExprEvaluator xEval = indexDesc.getIndexedExpressions()[0].getExprEvaluator();
        ExprEvaluator yEval = indexDesc.getIndexedExpressions()[1].getExprEvaluator();
        AdvancedIndexConfigStatementQuadtree indexStatementConfigs = new AdvancedIndexConfigStatementQuadtree(xEval, yEval);

        return new AdvancedIndexProvisionDesc(indexDesc, ExprNodeUtility.getEvaluators(parameters), AdvancedIndexFactoryPointRegionQuadTree.INSTANCE, indexStatementConfigs);
    }
}
