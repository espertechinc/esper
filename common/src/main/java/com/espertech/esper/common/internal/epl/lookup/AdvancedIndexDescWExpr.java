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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierVisitor;

import java.io.StringWriter;

public class AdvancedIndexDescWExpr {
    private final String indexTypeName;
    private final ExprNode[] indexedExpressions;

    public AdvancedIndexDescWExpr(String indexTypeName, ExprNode[] indexedExpressions) {
        this.indexTypeName = indexTypeName;
        this.indexedExpressions = indexedExpressions;
    }

    public String getIndexTypeName() {
        return indexTypeName;
    }

    public ExprNode[] getIndexedExpressions() {
        return indexedExpressions;
    }

    public String toQueryPlan() {
        if (indexedExpressions.length == 0) {
            return indexTypeName;
        }
        StringWriter writer = new StringWriter();
        writer.append(indexTypeName);
        writer.append("(");
        ExprNodeUtilityPrint.toExpressionStringMinPrecedenceAsList(indexedExpressions, writer);
        writer.append(")");
        return writer.toString();
    }

    public AdvancedIndexIndexMultiKeyPart getAdvancedIndexDescRuntime() {
        String[] indexExpressionTexts = new String[indexedExpressions.length];
        String[] indexedProperties = new String[indexExpressionTexts.length];
        for (int i = 0; i < indexedExpressions.length; i++) {
            indexExpressionTexts[i] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(indexedExpressions[i]);
            ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(true);
            indexedExpressions[i].accept(visitor);
            if (visitor.getExprProperties().size() != 1) {
                throw new IllegalStateException("Failed to find indexed property");
            }
            indexedProperties[i] = visitor.getExprProperties().iterator().next().getSecond();
        }
        return new AdvancedIndexIndexMultiKeyPart(indexTypeName, indexExpressionTexts, indexedProperties);
    }
}
