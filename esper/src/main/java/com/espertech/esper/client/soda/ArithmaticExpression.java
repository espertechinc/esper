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
 * Arithmatic expression for addition, subtraction, multiplication, division and modulo.
 */
public class ArithmaticExpression extends ExpressionBase {
    private static final long serialVersionUID = 2237615280620839719L;

    private String operator;

    /**
     * Ctor.
     */
    public ArithmaticExpression() {
    }

    /**
     * Ctor.
     *
     * @param operator can be any of '-', '+', '*', '/' or '%' (modulo).
     */
    public ArithmaticExpression(String operator) {
        this.operator = operator;
    }

    /**
     * Ctor.
     *
     * @param left     the left hand side
     * @param operator can be any of '-', '+', '*', '/' or '%' (modulo).
     * @param right    the right hand side
     */
    public ArithmaticExpression(Expression left, String operator, Expression right) {
        this.operator = operator;
        addChild(left);
        addChild(right);
    }

    /**
     * Returns the arithmatic operator.
     *
     * @return operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Set operator
     *
     * @param operator new value
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * Add a constant to include in the computation.
     *
     * @param object constant to add
     * @return expression
     */
    public ArithmaticExpression add(Object object) {
        this.getChildren().add(new ConstantExpression(object));
        return this;
    }

    /**
     * Add an expression to include in the computation.
     *
     * @param expression to add
     * @return expression
     */
    public ArithmaticExpression add(Expression expression) {
        this.getChildren().add(expression);
        return this;
    }

    /**
     * Add a property to include in the computation.
     *
     * @param propertyName is the name of the property
     * @return expression
     */
    public ArithmaticExpression add(String propertyName) {
        this.getChildren().add(new PropertyValueExpression(propertyName));
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        if (operator.equals("*") || operator.equals("/") || operator.equals("%")) {
            return ExpressionPrecedenceEnum.MULTIPLY;
        } else {
            return ExpressionPrecedenceEnum.ADDITIVE;
        }
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        for (Expression child : this.getChildren()) {
            writer.write(delimiter);
            child.toEPL(writer, getPrecedence());
            delimiter = operator;
        }
    }
}
