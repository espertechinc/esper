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

/**
 * Indicates a syntax exception
 */
public class EPCompileExceptionSyntaxItem extends EPCompileExceptionItem {
    private static final long serialVersionUID = -1042773433127517692L;

    /**
     * Ctor.
     *
     * @param message    - error message
     * @param cause - cause
     * @param expression - expression text
     * @param lineNumber - line number
     */
    public EPCompileExceptionSyntaxItem(String message, Throwable cause, String expression, int lineNumber) {
        super(message, cause, expression, lineNumber);
    }
}




