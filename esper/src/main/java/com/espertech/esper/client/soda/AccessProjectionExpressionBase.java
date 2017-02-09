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
import java.util.List;

/**
 * Represents the base expression for "first", "last" and "window" aggregation functions.
 */
public abstract class AccessProjectionExpressionBase extends ExpressionBase {
    private static final long serialVersionUID = 6858302558659972064L;

    /**
     * Ctor.
     */
    public AccessProjectionExpressionBase() {
    }

    /**
     * Returns the function name of the aggregation function.
     *
     * @return function name
     */
    public abstract String getAggregationFunctionName();

    /**
     * Ctor.
     *
     * @param expression to aggregate
     */
    public AccessProjectionExpressionBase(Expression expression) {
        this.getChildren().add(expression);
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(getAggregationFunctionName());
        writer.write('(');
        String delimiter = "";
        List<Expression> children = this.getChildren();
        if (children.size() > 0) {
            writer.write(delimiter);
            children.get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            for (int i = 1; i < children.size(); i++) {
                writer.write(",");
                children.get(i).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            }
        }
        writer.write(")");
    }
}