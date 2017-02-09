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
 * In-expression for a set of values returned by a lookup.
 */
public class SubqueryInExpression extends ExpressionBase {
    private boolean notIn;
    private EPStatementObjectModel model;
    private static final long serialVersionUID = 8013096666658967181L;

    /**
     * Ctor.
     */
    public SubqueryInExpression() {
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     *
     * @param model   is the lookup statement object model
     * @param isNotIn is true for not-in
     */
    public SubqueryInExpression(EPStatementObjectModel model, boolean isNotIn) {
        this.model = model;
        this.notIn = isNotIn;
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     *
     * @param expression is the expression providing the value to match
     * @param model      is the lookup statement object model
     * @param isNotIn    is true for not-in
     */
    public SubqueryInExpression(Expression expression, EPStatementObjectModel model, boolean isNotIn) {
        this.getChildren().add(expression);
        this.model = model;
        this.notIn = isNotIn;
    }

    /**
     * Returns true for not-in, or false for in-lookup.
     *
     * @return true for not-in
     */
    public boolean isNotIn() {
        return notIn;
    }

    /**
     * Set to true for not-in, or false for in-lookup.
     *
     * @param notIn true for not-in
     */
    public void setNotIn(boolean notIn) {
        this.notIn = notIn;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildren().get(0).toEPL(writer, getPrecedence());
        if (notIn) {
            writer.write(" not in (");
        } else {
            writer.write(" in (");
        }
        writer.write(model.toEPL());
        writer.write(')');
    }

    /**
     * Returns the lookup statement object model.
     *
     * @return lookup model
     */
    public EPStatementObjectModel getModel() {
        return model;
    }

    /**
     * Sets the lookup statement object model.
     *
     * @param model is the lookup model to set
     */
    public void setModel(EPStatementObjectModel model) {
        this.model = model;
    }
}
