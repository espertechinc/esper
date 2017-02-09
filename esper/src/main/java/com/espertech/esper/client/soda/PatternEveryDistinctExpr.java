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
import java.util.ArrayList;
import java.util.List;

/**
 * Every-Distinct construct for use in pattern expressions.
 */
public class PatternEveryDistinctExpr extends PatternExprBase {
    private static final long serialVersionUID = 0;
    private List<Expression> expressions = new ArrayList<Expression>();

    /**
     * Ctor - for use to create a pattern expression tree, without unique-criterial expression.
     */
    public PatternEveryDistinctExpr() {
    }

    /**
     * Ctor - for use to create a pattern expression tree, without unique-criterial expression.
     *
     * @param expressions distinct expressions
     */
    public PatternEveryDistinctExpr(List<Expression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Returns distinct expressions
     *
     * @return expr
     */
    public List<Expression> getExpressions() {
        return expressions;
    }

    /**
     * Sets distinct expressions.
     *
     * @param expressions expr
     */
    public void setExpressions(List<Expression> expressions) {
        this.expressions = expressions;
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.EVERY_NOT;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.write("every-distinct(");
        String delimiter = "";
        for (Expression expr : expressions) {
            writer.write(delimiter);
            expr.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        writer.write(") ");

        this.getChildren().get(0).toEPL(writer, getPrecedence(), formatter);
    }
}