/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.client.hook;

/**
 * Context provided to {@link ExceptionHandler} implementations providing
 * exception-contextual information as well as the exception itself.
 * <p>
 * Statement information pertains to the statement currently being processed when the unchecked exception occured.
 */
public class ExceptionHandlerContext {
    private final String engineURI;
    private final Throwable throwable;
    private final String statementName;
    private final String epl;
    private final ExceptionHandlerExceptionType type;

    /**
     * Ctor.
     * @param engineURI engine URI
     * @param throwable exception
     * @param statementName statement name
     * @param epl statement EPL expression text
     */
    public ExceptionHandlerContext(String engineURI, Throwable throwable, String statementName, String epl, ExceptionHandlerExceptionType type) {
        this.engineURI = engineURI;
        this.throwable = throwable;
        this.statementName = statementName;
        this.epl = epl;
        this.type = type;
    }

    /**
     * Returns the engine URI.
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns the exception.
     * @return exception
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Returns the statement name, if provided, or the statement id assigned to the statement if no name was provided.
     * @return statement name or id
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the expression text of the statement.
     * @return statement.
     */
    public String getEpl() {
        return epl;
    }

    public ExceptionHandlerExceptionType getType() {
        return type;
    }
}
