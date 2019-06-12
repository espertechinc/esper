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
package com.espertech.esper.common.internal.compile.stage2;

public class StatementSpecCompileException extends Exception {
    private static final long serialVersionUID = -917985672709976300L;
    private final String expression;

    public StatementSpecCompileException(String message, String expression) {
        super(message);
        this.expression = expression;
    }

    public StatementSpecCompileException(String message, Throwable cause, String expression) {
        super(message, cause);
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }
}
