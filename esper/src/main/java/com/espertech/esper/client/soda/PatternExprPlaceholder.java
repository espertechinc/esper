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
 * For use in pattern expression as a placeholder to represent its child nodes.
 */
public class PatternExprPlaceholder extends PatternExprBase {
    private static final long serialVersionUID = -2249254387564559395L;

    /**
     * Ctor.
     */
    public PatternExprPlaceholder() {
    }

    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        if ((this.getChildren() == null) || (this.getChildren().size() == 0)) {
            return;
        }
        PatternExpr patternExpr = getChildren().get(0);
        if (patternExpr != null) {
            patternExpr.toEPL(writer, getPrecedence(), formatter);
        }
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.MINIMUM;
    }
}