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

public class ExceptionLineItem extends Exception {
    protected final String expression;
    protected final int lineNumber;

    public ExceptionLineItem(String message, String expression, int lineNumber) {
        super(message);
        this.expression = expression;
        this.lineNumber = lineNumber;
    }

    public ExceptionLineItem(String message, Throwable cause, String expression, int lineNumber) {
        super(message, cause);
        this.expression = expression;
        this.lineNumber = lineNumber;
    }

    public String getExpression() {
        return expression;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
