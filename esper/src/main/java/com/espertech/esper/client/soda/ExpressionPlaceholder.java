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
 * For use in expression as a placeholder to represent its child nodes.
 */
public class ExpressionPlaceholder extends ExpressionBase {
    private static final long serialVersionUID = 8066735155786013524L;

    /**
     * Ctor.
     */
    public ExpressionPlaceholder() {
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if ((this.getChildren() == null) || (this.getChildren().size() == 0)) {
            return;
        }
        this.getChildren().get(0).toEPL(writer, getPrecedence());
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.MINIMUM;
    }
}