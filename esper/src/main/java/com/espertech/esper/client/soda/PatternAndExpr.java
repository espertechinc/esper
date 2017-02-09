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
 * Logical AND for use in pattern expressions.
 */
public class PatternAndExpr extends PatternExprBase {
    private static final long serialVersionUID = -3845778460131464052L;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     */
    public PatternAndExpr() {
    }

    /**
     * Ctor.
     *
     * @param first        a first pattern expression in the AND relationship
     * @param second       a second pattern expression in the AND relationship
     * @param patternExprs further optional pattern expressions in the AND relationship
     */
    public PatternAndExpr(PatternExpr first, PatternExpr second, PatternExpr... patternExprs) {
        this.addChild(first);
        this.addChild(second);
        for (int i = 0; i < patternExprs.length; i++) {
            this.addChild(patternExprs[i]);
        }
    }

    /**
     * Adds a pattern expression to the AND relationship between patterns.
     *
     * @param expr to add
     * @return pattern expression
     */
    public PatternAndExpr add(PatternExpr expr) {
        this.getChildren().add(expr);
        return this;
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.AND;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        String delimiter = "";
        for (PatternExpr child : this.getChildren()) {
            writer.write(delimiter);
            child.toEPL(writer, getPrecedence(), formatter);
            delimiter = " and ";
        }
    }
}
