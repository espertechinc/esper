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
package com.espertech.esper.common.client.hook.exception;

/**
 * Context provided to {@link ExceptionHandler} implementations providing
 * exception-contextual information as well as the exception itself,
 * for use with inbound pools and for exceptions unassociated to statements when using inbound pools.
 */
public class ExceptionHandlerContextUnassociated {
    private final String runtimeURI;
    private final Throwable throwable;
    private final Object currentEvent;

    /**
     * Ctor.
     *
     * @param runtimeURI   runtime URI
     * @param throwable    exception
     * @param currentEvent the event when applicable
     */
    public ExceptionHandlerContextUnassociated(String runtimeURI, Throwable throwable, Object currentEvent) {
        this.runtimeURI = runtimeURI;
        this.throwable = throwable;
        this.currentEvent = currentEvent;
    }

    /**
     * Returns the runtime URI.
     *
     * @return runtime URI
     */
    public String getRuntimeURI() {
        return runtimeURI;
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
