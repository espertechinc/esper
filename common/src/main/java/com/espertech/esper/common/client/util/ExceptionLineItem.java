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
package com.espertech.esper.common.client.util;

/**
 * Exception for line item.
 */
public class ExceptionLineItem extends Exception {
    private static final long serialVersionUID = 8381912568144436585L;
    protected final String expression;
    protected final int lineNumber;

    /**
     * Ctor.
     * @param message the message
     * @param cause the cause
     * @param expression the expression
     * @param lineNumber line number starting at 1
     */
    public ExceptionLineItem(String message, Throwable cause, String expression, int lineNumber) {
        super(message, cause);
        this.expression = replaceNewline(expression);
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the expression
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns the line number starting at 1
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    private String replaceNewline(String text) {
        text = text.replaceAll("\\n", " ");
        text = text.replaceAll("\\t", " ");
        text = text.replaceAll("\\r", " ");
        return text;
    }
}
