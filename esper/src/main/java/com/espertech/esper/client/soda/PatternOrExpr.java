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
 * Logical OR for use in pattern expressions.
 */
public class PatternOrExpr extends PatternExprBase {
    private static final long serialVersionUID = 2085920071129698255L;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     */
    public PatternOrExpr() {
    }

    /**
     * Ctor.
     *
     * @param first        a first pattern expression in the OR relationship
     * @param second       a second pattern expression in the OR relationship
     * @param patternExprs further optional pattern expressions in the OR relationship
     */
    public PatternOrExpr(PatternExpr first, PatternExpr second, PatternExpr... patternExprs) {
        this.addChild(first);
        this.addChild(second);
        for (int i = 0; i < patternExprs.length; i++) {
            this.addChild(patternExprs[i]);
        }
    }

    /**
     * Adds a pattern expression to the OR relationship between patterns.
     *
     * @param expr to add
     * @return pattern expression
     */
    public PatternOrExpr add(PatternExpr expr) {
        this.getChildren().add(expr);
        return this;
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.OR;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        String delimiter = "";
        for (PatternExpr child : this.getChildren()) {
            writer.write(delimiter);
            child.toEPL(writer, getPrecedence(), formatter);
            delimiter = " or ";
        }
    }
}
