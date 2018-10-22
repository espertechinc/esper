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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class QueryGraphValueEntryCustomKey implements QueryGraphValueEntry {
    private String operationName;
    private ExprEvaluator[] exprNodes;
    private String[] expressions;

    public String getOperationName() {
        return operationName;
    }

    public ExprEvaluator[] getExprNodes() {
        return exprNodes;
    }

    public String[] getExpressions() {
        return expressions;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public void setExprNodes(ExprEvaluator[] exprNodes) {
        this.exprNodes = exprNodes;
    }

    public void setExpressions(String[] expressions) {
        this.expressions = expressions;
    }
}

