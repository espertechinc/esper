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
 * Cast expression casts the return value of an expression to a specified type.
 */
public class CastExpression extends ExpressionBase {
    private String typeName;
    private static final long serialVersionUID = -8931072217889088459L;

    /**
     * Ctor.
     */
    public CastExpression() {
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     *
     * @param typeName is the type to cast to: a fully-qualified class name or Java primitive type name or "string"
     */
    public CastExpression(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Ctor.
     *
     * @param expressionToCheck provides values to cast
     * @param typeName          is the type to cast to: a fully-qualified class names or Java primitive type names or "string"
     */
    public CastExpression(Expression expressionToCheck, String typeName) {
        this.getChildren().add(expressionToCheck);
        this.typeName = typeName;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("cast(");
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        writer.write(",");
        writer.write(typeName);
        for (int i = 1; i < this.getChildren().size(); i++) {
            writer.write(",");
            this.getChildren().get(i).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.write(")");
    }

    /**
     * Returns the name of the type to cast to.
     *
     * @return type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets the name of the type to cast to.
     *
     * @param typeName is the name of type to cast to
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
