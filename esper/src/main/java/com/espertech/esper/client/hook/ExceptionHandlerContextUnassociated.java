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

/**
 * Context provided to {@link ExceptionHandler} implementations providing
 * exception-contextual information as well as the exception itself,
 * for use with inbound pools and for exceptions unassociated to statements when using inbound pools.
 */
public class ExceptionHandlerContextUnassociated {
    private final String engineURI;
    private final Throwable throwable;
    private final Object currentEvent;

    /**
     * Ctor.
     *
     * @param engineURI    engine URI
     * @param throwable    exception
     * @param currentEvent the event when applicable
     */
    public ExceptionHandlerContextUnassociated(String engineURI, Throwable throwable, Object currentEvent) {
        this.engineURI = engineURI;
        this.throwable = throwable;
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
     * Returns the current event, when available.
     *
     * @return current event or null if not available
     */
    public Object getCurrentEvent() {
        return currentEvent;
    }
}
