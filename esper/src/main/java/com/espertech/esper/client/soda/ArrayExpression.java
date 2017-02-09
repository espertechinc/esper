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
 * Array expression forms array results, similar to the syntax of "{element 1, element 2, ... element n}".
 */
public class ArrayExpression extends ExpressionBase {
    private static final long serialVersionUID = 5332982041846659978L;

    /**
     * Ctor.
     */
    public ArrayExpression() {
    }

    /**
     * Add a property to the expression.
     *
     * @param property to add
     * @return expression
     */
    public ArrayExpression add(String property) {
        this.getChildren().add(new PropertyValueExpression(property));
        return this;
    }

    /**
     * Add a constant to the expression.
     *
     * @param object constant to add
     * @return expression
     */
    public ArrayExpression add(Object object) {
        this.getChildren().add(new ConstantExpression(object));
        return this;
    }

    /**
     * Add an expression representing an array element to the expression.
     *
     * @param expression to add
     * @return expression
     */
    public ArrayExpression add(Expression expression) {
        this.getChildren().add(expression);
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("{");
        boolean isFirst = true;
        for (Expression child : this.getChildren()) {
            if (!isFirst) {
                writer.write(",");
            }
            child.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            isFirst = false;
        }
        writer.write("}");
    }
}
