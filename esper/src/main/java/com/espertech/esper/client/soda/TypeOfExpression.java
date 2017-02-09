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
 * Type-of expression return the type name, as a string value, of the events in the stream if passing a stream name or
 * the fragment event type if passing a property name that results in a fragment event otherwise
 * the class simple name of the expression result or null if the expression returns a null value.
 */
public class TypeOfExpression extends ExpressionBase {
    private String[] typeNames;
    private static final long serialVersionUID = 8091600570950143727L;

    /**
     * Ctor.
     */
    public TypeOfExpression() {
    }

    /**
     * Ctor.
     *
     * @param expression for which to return the result type or null if the result is null
     */
    public TypeOfExpression(Expression expression) {
        this.getChildren().add(expression);
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("typeof(");
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        writer.write(")");
    }
}
