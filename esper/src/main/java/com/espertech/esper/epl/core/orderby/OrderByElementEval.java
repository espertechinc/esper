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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;

public class OrderByElementEval {
    private final ExprNode exprNode;
    private final ExprEvaluator expr;
    private final boolean isDescending;

    public OrderByElementEval(ExprNode exprNode, ExprEvaluator expr, boolean descending) {
        this.exprNode = exprNode;
        this.expr = expr;
        isDescending = descending;
    }

    public ExprNode getExprNode() {
        return exprNode;
    }

    public ExprEvaluator getExpr() {
        return expr;
    }

    public boolean isDescending() {
        return isDescending;
    }
}
