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
 * Named parameter expression of the form "name:expression" or "name:(expression, expression...)"
 */
public class NamedParameterExpression extends ExpressionBase {
    private static final long serialVersionUID = 3556889255505413412L;
    private String name;

    /**
     * Ctor.
     */
    public NamedParameterExpression() {
    }

    /**
     * Ctor.
     *
     * @param name substitution parameter name
     */
    public NamedParameterExpression(String name) {
        this.name = name;
    }

    /**
     * Returns the parameter name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the parameter name.
     *
     * @param name name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(name);
        writer.write(':');
        if (this.getChildren().size() > 1 || this.getChildren().isEmpty()) {
            writer.write('(');
        }

        String delimiter = "";
        for (Expression expr : this.getChildren()) {
            writer.write(delimiter);
            expr.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        if (this.getChildren().size() > 1 || this.getChildren().isEmpty()) {
            writer.write(')');
        }
    }
}
