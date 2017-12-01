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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;

public class QueryGraphValueEntryCustomKey implements QueryGraphValueEntry {
    private final String operationName;
    private final ExprNode[] exprNodes;

    public QueryGraphValueEntryCustomKey(String operationName, ExprNode[] exprNodes) {
        this.operationName = operationName;
        this.exprNodes = exprNodes;
    }

    public String getOperationName() {
        return operationName;
    }

    public ExprNode[] getExprNodes() {
        return exprNodes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryGraphValueEntryCustomKey that = (QueryGraphValueEntryCustomKey) o;

        if (!operationName.equals(that.operationName)) return false;
        return ExprNodeUtilityCore.deepEquals(exprNodes, that.exprNodes, true);
    }

    public int hashCode() {
        return operationName.hashCode();
    }
}

