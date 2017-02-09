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
 * Not-expression for negating a pattern sub-expression for use in pattern expressions.
 */
public class PatternNotExpr extends PatternExprBase {
    private static final long serialVersionUID = -7739688374458616308L;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     */
    public PatternNotExpr() {
    }

    /**
     * Ctor.
     *
     * @param inner is the pattern expression to negate
     */
    public PatternNotExpr(PatternExpr inner) {
        this.getChildren().add(inner);
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.EVERY_NOT;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.write("not ");
        this.getChildren().get(0).toEPL(writer, getPrecedence(), formatter);
    }
}
