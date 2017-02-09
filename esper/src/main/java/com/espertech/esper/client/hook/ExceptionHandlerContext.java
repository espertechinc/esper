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
package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventBean;

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
    private final EventBean currentEvent;

    /**
     * Ctor.
     *
     * @param engineURI     engine URI
     * @param throwable     exception
     * @param statementName statement name
     * @param epl           statement EPL expression text
     * @param type          exception type
     * @param currentEvent  the event when applicable
     */
    public ExceptionHandlerContext(String engineURI, Throwable throwable, String statementName, String epl, ExceptionHandlerExceptionType type, EventBean currentEvent) {
        this.engineURI = engineURI;
        this.throwable = throwable;
        this.statementName = statementName;
        this.epl = epl;
        this.type = type;
        this.currentEvent = currentEvent;
    }

    /**
     * Returns the engine URI.
     *
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns the exception.
     *
     * @return exception
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Returns the statement name, if provided, or the statement id assigned to the statement if no name was provided.
     *
     * @return statement name or id
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the expression text of the statement.
     *
     * @return statement.
     */
    public String getEpl() {
        return epl;
    }

    /**
     * Returns exception type information
     *
     * @return type
     */
    public ExceptionHandlerExceptionType getType() {
        return type;
    }

    /**
     * Returns the current event, when available.
     *
     * @return current event or null if not available
     */
    public EventBean getCurrentEvent() {
        return currentEvent;
    }
}
