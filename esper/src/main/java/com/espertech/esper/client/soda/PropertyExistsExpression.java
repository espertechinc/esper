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
 * Property-exists checks if a dynamic property exists.
 */
public class PropertyExistsExpression extends ExpressionBase {
    private static final long serialVersionUID = 415089848067641931L;

    /**
     * Ctor - for use to create an expression tree, without child expression.
     */
    public PropertyExistsExpression() {
    }

    /**
     * Ctor.
     *
     * @param propertyName is the name of the property to check existence
     */
    public PropertyExistsExpression(String propertyName) {
        this.getChildren().add(Expressions.getPropExpr(propertyName));
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("exists(");
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        writer.write(")");
    }
}
