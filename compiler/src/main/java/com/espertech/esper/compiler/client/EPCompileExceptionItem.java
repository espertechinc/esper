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
package com.espertech.esper.compiler.client;

import com.espertech.esper.common.client.EPException;

/**
 * Exception information.
 */
public class EPCompileExceptionItem extends EPException {
    private final String expression;
    private final int lineNumber;

    /**
     * Ctor.
     *
     * @param message    - error message
     * @param expression - expression text
     * @param lineNumber - line number
     */
    public EPCompileExceptionItem(final String message, String expression, int lineNumber) {
        super(message);
        this.expression = expression;
        this.lineNumber = lineNumber;
    }

    /**
     * Ctor.
     *
     * @param message    error message
     * @param cause      inner exception
     * @param expression expression text
     * @param lineNumber - line number
     */
    public EPCompileExceptionItem(String message, Throwable cause, String expression, int lineNumber) {
        super(message, cause);
        this.expression = expression;
        this.lineNumber = lineNumber;
    }

    /**
     * Returns expression text for statement.
     *
     * @return expression text
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns the line number.
     *
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        StringBuilder msg;
        if (super.getMessage() != null) {
            msg = new StringBuilder(super.getMessage());
        } else {
            msg = new StringBuilder("Unexpected exception");
        }
        if (expression != null) {
            msg.append(" [");
            msg.append(expression);
            msg.append(']');
        }
        return msg.toString();
    }
}
