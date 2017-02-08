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

import java.util.List;

public class GroupByClauseElementCombinedExpr implements GroupByClauseElement {
    private static final long serialVersionUID = -8761673351655924445L;
    private final List<ExprNode> expressions;

    public GroupByClauseElementCombinedExpr(List<ExprNode> expressions) {
        this.expressions = expressions;
    }

    public List<ExprNode> getExpressions() {
        return expressions;
    }
}
