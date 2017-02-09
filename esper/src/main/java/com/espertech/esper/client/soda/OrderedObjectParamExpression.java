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
 * For use in view parameter lists, this is a wrapper expression
 * that adds an ascending or descending sort indicator to its single child expression.
 */
public class OrderedObjectParamExpression extends ExpressionBase {
    private boolean descending;
    private static final long serialVersionUID = -8607653757516174092L;

    /**
     * Ctor.
     */
    public OrderedObjectParamExpression() {
    }

    /**
     * Ctor.
     *
     * @param descending to indicate a descending sort, or false for ascending
     */
    public OrderedObjectParamExpression(boolean descending) {
        this.descending = descending;
    }

    /**
     * Returns true for descending, false for ascending.
     *
     * @return indicator for descending sort
     */
    public boolean isDescending() {
        return descending;
    }

    /**
     * Returns true for descending, false for ascending.
     *
     * @return indicator for descending sort
     */
    public boolean getDescending() {
        return descending;
    }

    /**
     * Return true for descending.
     *
     * @param descending indicator
     */
    public void setDescending(boolean descending) {
        this.descending = descending;
    }

    /**
     * Return precedence.
     *
     * @return precedence
     */
    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        if (descending) {
            writer.write(" desc");
        } else {
            writer.write(" asc");
        }
    }
}
