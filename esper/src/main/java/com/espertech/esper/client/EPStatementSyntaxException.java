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
package com.espertech.esper.client;

/**
 * This exception is thrown to indicate a problem in statement creation.
 */
public class EPStatementSyntaxException extends EPStatementException {
    private static final long serialVersionUID = -1042773433127517692L;

    /**
     * Ctor.
     *
     * @param message    - error message
     * @param expression - expression text
     */
    public EPStatementSyntaxException(String message, String expression) {
        super(message, expression);
    }
}




