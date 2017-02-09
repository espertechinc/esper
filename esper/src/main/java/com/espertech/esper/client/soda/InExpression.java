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
 * In-expresson checks that a value is in (or not in) a set of values, equivalent to the syntax "color in ('red', 'blue')".
 */
public class InExpression extends ExpressionBase {
    private boolean notIn;
    private static final long serialVersionUID = -5105764419995579144L;

    /**
     * Ctor.
     */
    public InExpression() {
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     *
     * @param isNotIn true for the not-in expression, false for the in-expression
     */
    public InExpression(boolean isNotIn) {
        this.notIn = isNotIn;
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     *
     * @param value      an expression that provides the value to search for in the set
     * @param isNotIn    true for the not-in expression, false for the in-expression
     * @param parameters is a set of constants to match against
     */
    public InExpression(Expression value, boolean isNotIn, Object... parameters) {
        this.notIn = isNotIn;
        this.getChildren().add(value);
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] instanceof Expression) {
                this.getChildren().add((Expression) parameters[i]);
            } else {
                this.getChildren().add(new ConstantExpression(parameters[i]));
            }
        }
    }

    /**
     * Ctor.
     *
     * @param value      expression to check
     * @param isNotIn    indicator whether not-in (true) or in (false)
     * @param parameters expression list
     */
    public InExpression(Expression value, boolean isNotIn, Expression[] parameters) {
        this.notIn = isNotIn;
        this.getChildren().add(value);
        for (Expression parameter : parameters) {
            this.getChildren().add(parameter);
        }
    }

    /**
     * Returns true for the not-in expression, or false for an in-expression.
     *
     * @return true for not-in
     */
    public boolean isNotIn() {
        return notIn;
    }

    /**
     * Returns true for the not-in expression, or false for an in-expression.
     *
     * @return true for not-in
     */
    public boolean getNotIn() {
        return notIn;
    }

    /**
     * Set to true to indicate this is a not-in expression.
     *
     * @param notIn true for not-in, false for in-expression
     */
    public void setNotIn(boolean notIn) {
        this.notIn = notIn;
    }

    /**
     * Add a constant to include in the computation.
     *
     * @param object constant to add
     * @return expression
     */
    public InExpression add(Object object) {
        this.getChildren().add(new ConstantExpression(object));
        return this;
    }

    /**
     * Add an expression to include in the computation.
     *
     * @param expression to add
     * @return expression
     */
    public InExpression add(Expression expression) {
        this.getChildren().add(expression);
        return this;
    }

    /**
     * Add a property to include in the computation.
     *
     * @param propertyName is the name of the property
     * @return expression
     */
    public InExpression add(String propertyName) {
        this.getChildren().add(new PropertyValueExpression(propertyName));
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildren().get(0).toEPL(writer, getPrecedence());
        if (notIn) {
            writer.write(" not in (");
        } else {
            writer.write(" in (");
        }

        String delimiter = "";
        for (int i = 1; i < this.getChildren().size(); i++) {
            writer.write(delimiter);
            this.getChildren().get(i).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        writer.write(')');
    }
}
