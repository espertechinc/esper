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

import com.espertech.esper.type.BitWiseOpEnum;

import java.io.StringWriter;

/**
 * Bitwise (binary) operator for binary AND, binary OR and binary XOR.
 */
public class BitwiseOpExpression extends ExpressionBase {
    private BitWiseOpEnum binaryOp;
    private static final long serialVersionUID = 5564205980185587363L;

    /**
     * Ctor.
     */
    public BitwiseOpExpression() {
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     *
     * @param binaryOp the binary operator
     */
    public BitwiseOpExpression(BitWiseOpEnum binaryOp) {
        this.binaryOp = binaryOp;
    }

    /**
     * Add a property to the expression.
     *
     * @param property to add
     * @return expression
     */
    public BitwiseOpExpression add(String property) {
        this.getChildren().add(new PropertyValueExpression(property));
        return this;
    }

    /**
     * Add a constant to the expression.
     *
     * @param object constant to add
     * @return expression
     */
    public BitwiseOpExpression add(Object object) {
        this.getChildren().add(new ConstantExpression(object));
        return this;
    }

    /**
     * Add an expression to the expression.
     *
     * @param expression to add
     * @return expression
     */
    public BitwiseOpExpression add(Expression expression) {
        this.getChildren().add(expression);
        return this;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.BITWISE;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        boolean isFirst = true;
        for (Expression child : this.getChildren()) {
            if (!isFirst) {
                writer.write(binaryOp.getExpressionText());
            }
            child.toEPL(writer, getPrecedence());
            isFirst = false;
        }
    }

    /**
     * Returns the binary operator.
     *
     * @return operator
     */
    public BitWiseOpEnum getBinaryOp() {
        return binaryOp;
    }

    /**
     * Sets the binary operator.
     *
     * @param binaryOp operator to set
     */
    public void setBinaryOp(BitWiseOpEnum binaryOp) {
        this.binaryOp = binaryOp;
    }
}
