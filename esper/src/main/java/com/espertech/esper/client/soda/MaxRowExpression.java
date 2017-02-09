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
 * Maximum-value per-row expression (not aggregating) determines the maximum value among a set of values.
 */
public class MaxRowExpression extends ExpressionBase {
    private static final long serialVersionUID = -286616738319179765L;

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     */
    public MaxRowExpression() {
    }

    /**
     * Ctor.
     *
     * @param propertyOne    the name of the property providing a value to determine the maximum of
     * @param propertyTwo    the name of the property providing a value to determine the maximum of
     * @param moreProperties optional additional properties to consider
     */
    public MaxRowExpression(String propertyOne, String propertyTwo, String[] moreProperties) {
        addChild(new PropertyValueExpression(propertyOne));
        addChild(new PropertyValueExpression(propertyTwo));
        for (int i = 0; i < moreProperties.length; i++) {
            addChild(new PropertyValueExpression(moreProperties[i]));
        }
    }

    /**
     * Ctor.
     *
     * @param exprOne         provides a value to determine the maximum of
     * @param exprTwo         provides a value to determine the maximum of
     * @param moreExpressions optional additional values to consider
     */
    public MaxRowExpression(Expression exprOne, Expression exprTwo, Expression... moreExpressions) {
        addChild(exprOne);
        addChild(exprTwo);
        for (int i = 0; i < moreExpressions.length; i++) {
            addChild(moreExpressions[i]);
        }
    }

    /**
     * Add a constant to include in the computation.
     *
     * @param object constant to add
     * @return expression
     */
    public MaxRowExpression add(Object object) {
        this.getChildren().add(new ConstantExpression(object));
        return this;
    }

    /**
     * Add an expression to include in the computation.
     *
     * @param expression to add
     * @return expression
     */
    public MaxRowExpression add(Expression expression) {
        this.getChildren().add(expression);
        return this;
    }

    /**
     * Add a property to include in the computation.
     *
     * @param propertyName is the name of the property
     * @return expression
     */
    public MaxRowExpression add(String propertyName) {
        this.getChildren().add(new PropertyValueExpression(propertyName));
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("max(");

        String delimiter = "";
        for (Expression expr : this.getChildren()) {
            writer.write(delimiter);
            expr.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        writer.write(')');
    }
}
