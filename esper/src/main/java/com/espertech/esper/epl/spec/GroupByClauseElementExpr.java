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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

public class GroupByClauseElementExpr implements GroupByClauseElement {
    private static final long serialVersionUID = -5528463243284179063L;
    private final ExprNode expr;

    public GroupByClauseElementExpr(ExprNode expr) {
        this.expr = expr;
    }

    public ExprNode getExpr() {
        return expr;
    }
}
