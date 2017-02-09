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
package com.espertech.esper.client.deploy;

/**
 * Inner exception to {@link DeploymentActionException} available on statement level.
 */
public class DeploymentItemException extends DeploymentException {

    private static final long serialVersionUID = 5496069128630634391L;

    private String expression;
    private RuntimeException inner;
    private int lineNumber;

    /**
     * Ctor.
     *
     * @param message    exception text
     * @param expression EPL
     * @param inner      compile or start exception
     * @param lineNumber line number
     */
    public DeploymentItemException(String message, String expression, RuntimeException inner, int lineNumber) {
        super(message, inner);
        this.expression = expression;
        this.inner = inner;
        this.lineNumber = lineNumber;
    }

    /**
     * Returns EPL expression.
     *
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns EPL compile or start exception.
     *
     * @return exception
     */
    public RuntimeException getInner() {
        return inner;
    }

    /**
     * Returns line number.
     *
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }
}