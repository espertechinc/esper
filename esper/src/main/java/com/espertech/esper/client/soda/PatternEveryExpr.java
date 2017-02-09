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
 * Pattern 'every' expression that controls the lifecycle of pattern sub-expressions.
 */
public class PatternEveryExpr extends PatternExprBase {
    private static final long serialVersionUID = 6325304538100271837L;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     */
    public PatternEveryExpr() {
    }

    /**
     * Ctor.
     *
     * @param inner is the pattern expression to control lifecycle on
     */
    public PatternEveryExpr(PatternExpr inner) {
        addChild(inner);
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.EVERY_NOT;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.write("every ");
        PatternExprPrecedenceEnum precedence = getPrecedence();
        if (this.getChildren().get(0) instanceof PatternEveryExpr) {
            precedence = PatternExprPrecedenceEnum.MAXIMIM;
        }
        this.getChildren().get(0).toEPL(writer, precedence, formatter);
    }
}
