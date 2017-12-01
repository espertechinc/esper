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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.StringWriter;

public class AdvancedIndexDesc {
    private final String indexTypeName;
    private final ExprNode[] indexedExpressions;

    public AdvancedIndexDesc(String indexTypeName, ExprNode[] indexedExpressions) {
        this.indexTypeName = indexTypeName;
        this.indexedExpressions = indexedExpressions;
    }

    public String getIndexTypeName() {
        return indexTypeName;
    }

    public ExprNode[] getIndexedExpressions() {
        return indexedExpressions;
    }

    public boolean equalsAdvancedIndex(AdvancedIndexDesc that) {
        return indexTypeName.equals(that.indexTypeName) && ExprNodeUtilityCore.deepEquals(indexedExpressions, that.indexedExpressions, true);
    }

    public String toQueryPlan() {
        if (indexedExpressions.length == 0) {
            return indexTypeName;
        }
        StringWriter writer = new StringWriter();
        writer.append(indexTypeName);
        writer.append("(");
        ExprNodeUtilityCore.toExpressionStringMinPrecedenceAsList(indexedExpressions, writer);
        writer.append(")");
        return writer.toString();
    }
}
