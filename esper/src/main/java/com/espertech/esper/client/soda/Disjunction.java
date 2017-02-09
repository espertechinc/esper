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
 * Disjunction represents a logical OR allowing multiple sub-expressions to be connected by OR.
 */
public class Disjunction extends Junction {
    private static final long serialVersionUID = -3457740552640868614L;

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     */
    public Disjunction() {
    }

    /**
     * Ctor.
     *
     * @param first       an expression to add to the OR-test
     * @param second      an expression to add to the OR-test
     * @param expressions is the expression to put in the OR-relationship.
     */
    public Disjunction(Expression first, Expression second, Expression... expressions) {
        addChild(first);
        addChild(second);
        for (int i = 0; i < expressions.length; i++) {
            addChild(expressions[i]);
        }
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.OR;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        for (Expression child : this.getChildren()) {
            writer.write(delimiter);
            child.toEPL(writer, getPrecedence());
            delimiter = " or ";
        }
    }
}
