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
 * This exception is thrown to indicate a problem in statement creation, such as syntax error or type checking problem
 * etc.
 */
public class EPStatementException extends EPException {
    private String expression;
    private static final long serialVersionUID = 3279540500985257610L;

    /**
     * Ctor.
     *
     * @param message    - error message
     * @param expression - expression text
     */
    public EPStatementException(final String message, final String expression) {
        super(message);
        this.expression = expression;
    }

    /**
     * Ctor.
     *
     * @param message    error message
     * @param cause      inner exception
     * @param expression expression text
     */
    public EPStatementException(String message, Throwable cause, String expression) {
        super(message, cause);
        this.expression = expression;
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
     * Sets expression text for statement.
     *
     * @param expression text
     */
    public void setExpression(String expression) {
        this.expression = expression;
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
