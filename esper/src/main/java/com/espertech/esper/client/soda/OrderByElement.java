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

import java.io.Serializable;
import java.io.StringWriter;

/**
 * A single entry in an order-by clause consisting of an expression and order ascending or descending flag.
 */
public class OrderByElement implements Serializable {
    private static final long serialVersionUID = 0L;

    private Expression expression;
    private boolean descending;

    /**
     * Ctor.
     */
    public OrderByElement() {
    }

    /**
     * Ctor.
     *
     * @param expression is the expression to order by
     * @param descending true for descending sort, false for ascending sort
     */
    public OrderByElement(Expression expression, boolean descending) {
        this.expression = expression;
        this.descending = descending;
    }

    /**
     * Returns the order-by value expression.
     *
     * @return expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the order-by value expression.
     *
     * @param expression provides order-by values
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns true for descending sorts for this column, false for ascending sort.
     *
     * @return true for descending sort
     */
    public boolean isDescending() {
        return descending;
    }

    /**
     * Set to true for descending sorts for this column, false for ascending sort.
     *
     * @param descending true for descending sort
     */
    public void setDescending(boolean descending) {
        this.descending = descending;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        if (descending) {
            writer.write(" desc");
        }
    }
}
