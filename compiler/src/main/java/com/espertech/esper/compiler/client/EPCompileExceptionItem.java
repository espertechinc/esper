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

import com.espertech.esper.common.client.util.ExceptionLineItem;

/**
 * Exception information.
 */
public class EPCompileExceptionItem extends ExceptionLineItem {
    /**
     * Ctor.
     * @param message the message
     * @param cause the cause
     * @param expression the expression
     * @param lineNumber line number starting at 1
     */
    public EPCompileExceptionItem(String message, Throwable cause, String expression, int lineNumber) {
        super(message, cause, expression, lineNumber);
    }
}
