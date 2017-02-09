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
import java.util.List;

/**
 * A combination of expressions is for example "(a, b)", wherein the list of expressions provided
 * together logically make up a grouping level.
 */
public class GroupByClauseExpressionCombination implements GroupByClauseExpression {
    private static final long serialVersionUID = -971482121074669985L;
    private List<Expression> expressions;

    /**
     * Ctor.
     *
     * @param expressions combination of expressions
     */
    public GroupByClauseExpressionCombination(List<Expression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Ctor.
     */
    public GroupByClauseExpressionCombination() {
    }

    /**
     * Returns the combined expressions.
     *
     * @return expressions
     */
    public List<Expression> getExpressions() {
        return expressions;
    }

    /**
     * Sets the combined expressions.
     *
     * @param expressions expressions
     */
    public void setExpressions(List<Expression> expressions) {
        this.expressions = expressions;
    }

    public void toEPL(StringWriter writer) {
        writer.write("(");
        String delimiter = "";
        for (Expression e : expressions) {
            writer.write(delimiter);
            e.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ", ";
        }
        writer.write(")");
    }
}
