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
import java.util.Locale;

/**
 * Previous function for obtaining property values of previous events.
 */
public class PreviousExpression extends ExpressionBase {
    private static final long serialVersionUID = -4347875900366616364L;

    private PreviousExpressionType type = PreviousExpressionType.PREV;

    /**
     * Ctor.
     */
    public PreviousExpression() {
    }

    /**
     * Ctor.
     *
     * @param expression   provides the index to use
     * @param propertyName is the name of the property to return the value for
     */
    public PreviousExpression(Expression expression, String propertyName) {
        this.addChild(expression);
        this.addChild(new PropertyValueExpression(propertyName));
    }

    /**
     * Ctor.
     *
     * @param index        provides the index
     * @param propertyName is the name of the property to return the value for
     */
    public PreviousExpression(int index, String propertyName) {
        this.addChild(new ConstantExpression(index));
        this.addChild(new PropertyValueExpression(propertyName));
    }

    /**
     * Ctor.
     *
     * @param type       type of previous expression (tail, first, window, count)
     * @param expression to evaluate
     */
    public PreviousExpression(PreviousExpressionType type, Expression expression) {
        this.type = type;
        this.addChild(expression);
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    /**
     * Returns the type of the previous expression (tail, first, window, count)
     *
     * @return type
     */
    public PreviousExpressionType getType() {
        return type;
    }

    /**
     * Sets the type of the previous expression (tail, first, window, count)
     *
     * @param type to set
     */
    public void setType(PreviousExpressionType type) {
        this.type = type;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(type.toString().toLowerCase(Locale.ENGLISH));
        writer.write("(");
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        if (this.getChildren().size() > 1) {
            writer.write(",");
            this.getChildren().get(1).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.write(')');
    }
}
