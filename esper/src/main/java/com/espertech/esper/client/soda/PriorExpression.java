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
 * Expression representing the prior function.
 */
public class PriorExpression extends ExpressionBase {
    private static final long serialVersionUID = 3914409812498086994L;

    /**
     * Ctor - for use to create an expression tree, without child expression.
     */
    public PriorExpression() {
    }

    /**
     * Ctor.
     *
     * @param index        is the index of the prior event
     * @param propertyName is the property to return
     */
    public PriorExpression(int index, String propertyName) {
        this.addChild(new ConstantExpression(index));
        this.addChild(new PropertyValueExpression(propertyName));
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("prior(");
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        writer.write(",");
        this.getChildren().get(1).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        writer.write(')');
    }
}
