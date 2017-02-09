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
 * Part of a select-clause to describe individual select-clause expressions.
 */
public class SelectClauseExpression implements SelectClauseElement {
    private static final long serialVersionUID = 0L;

    private Expression expression;
    private String asName;
    private boolean annotatedByEventFlag;

    /**
     * Ctor.
     */
    public SelectClauseExpression() {
    }

    /**
     * Ctor.
     *
     * @param expression is the selection expression
     */
    public SelectClauseExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Ctor.
     *
     * @param expression     is the selection expression
     * @param optionalAsName is the "as"-tag for the expression
     */
    public SelectClauseExpression(Expression expression, String optionalAsName) {
        this.expression = expression;
        this.asName = optionalAsName;
    }

    /**
     * Returns the selection expression.
     *
     * @return expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the selection expression.
     *
     * @param expression is the selection expression
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns the optional "as"-name of the expression, or null if not defined
     *
     * @return tag or null for selection expression
     */
    public String getAsName() {
        return asName;
    }

    /**
     * Sets the optional "as"-name of the expression, or null if not defined
     *
     * @param asName column name or null for selection expression
     */
    public void setAsName(String asName) {
        this.asName = asName;
    }

    /**
     * Returns indicator whether annotated as "@eventbean"
     *
     * @return "@eventbean" indicator
     */
    public boolean isAnnotatedByEventFlag() {
        return annotatedByEventFlag;
    }

    /**
     * Sets indicator whether annotated as "@eventbean"
     *
     * @param annotatedByEventFlag "@eventbean" indicator
     */
    public void setAnnotatedByEventFlag(boolean annotatedByEventFlag) {
        this.annotatedByEventFlag = annotatedByEventFlag;
    }

    /**
     * Renders the element in textual representation.
     *
     * @param writer to output to
     */
    public void toEPLElement(StringWriter writer) {
        expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        if (annotatedByEventFlag) {
            writer.write(" @eventbean");
        }
        if (asName != null) {
            writer.write(" as ");
            writer.write(asName);
        }
    }
}
