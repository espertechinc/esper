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

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionCompileTime;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexDescWExpr;

import static com.espertech.esper.common.internal.epl.index.advanced.index.service.AdvancedIndexValidationHelper.validateColumnCount;
import static com.espertech.esper.common.internal.epl.index.advanced.index.service.AdvancedIndexValidationHelper.validateColumnReturnTypeNumber;

public class AdvancedIndexFactoryProviderMXCIFQuadTree extends AdvancedIndexFactoryProviderQuadTree {

    public EventAdvancedIndexProvisionCompileTime validateEventIndex(String indexName, String indexTypeName, ExprNode[] columns, ExprNode[] parameters) throws ExprValidationException {
        validateColumnCount(4, indexTypeName, columns.length);
        validateColumnReturnTypeNumber(indexTypeName, 0, columns[0], AdvancedIndexQuadTreeConstants.COL_X);
        validateColumnReturnTypeNumber(indexTypeName, 1, columns[1], AdvancedIndexQuadTreeConstants.COL_Y);
        validateColumnReturnTypeNumber(indexTypeName, 2, columns[2], AdvancedIndexQuadTreeConstants.COL_WIDTH);
        validateColumnReturnTypeNumber(indexTypeName, 3, columns[3], AdvancedIndexQuadTreeConstants.COL_HEIGHT);

        validateParameters(indexTypeName, parameters);

        AdvancedIndexDescWExpr indexDesc = new AdvancedIndexDescWExpr(indexTypeName, columns);
        ExprForge xEval = indexDesc.getIndexedExpressions()[0].getForge();
        ExprForge yEval = indexDesc.getIndexedExpressions()[1].getForge();
        ExprForge widthEval = indexDesc.getIndexedExpressions()[2].getForge();
        ExprForge heightEval = indexDesc.getIndexedExpressions()[3].getForge();
        AdvancedIndexConfigStatementMXCIFQuadtreeForge indexStatementConfigs = new AdvancedIndexConfigStatementMXCIFQuadtreeForge(xEval, yEval, widthEval, heightEval);

        return new EventAdvancedIndexProvisionCompileTime(indexDesc, parameters, EventAdvancedIndexFactoryForgeQuadTreeMXCIFForge.INSTANCE, indexStatementConfigs);
    }
}
