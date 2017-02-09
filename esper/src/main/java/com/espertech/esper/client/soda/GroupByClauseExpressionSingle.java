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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

/**
 * Represents a single expression (non-combined, rollup/cube or grouping set) as part of a group-by expression.
 */
public class GroupByClauseExpressionSingle implements GroupByClauseExpression {
    private static final long serialVersionUID = -6781748002118514367L;
    private Expression expression;

    /**
     * Ctor.
     *
     * @param expression the expression
     */
    public GroupByClauseExpressionSingle(Expression expression) {
        this.expression = expression;
    }

    /**
     * Ctor.
     */
    public GroupByClauseExpressionSingle() {
    }

    /**
     * Returns the expression.
     *
     * @return expressions
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the expression.
     *
     * @param expression expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public void toEPL(StringWriter writer) {
        expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
    }
}
