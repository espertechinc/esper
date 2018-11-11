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
    public EPCompileExceptionItem(String message, String expression, int lineNumber) {
        super(message, expression, lineNumber);
    }

    public EPCompileExceptionItem(String message, Throwable cause, String expression, int lineNumber) {
        super(message, cause, expression, lineNumber);
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
