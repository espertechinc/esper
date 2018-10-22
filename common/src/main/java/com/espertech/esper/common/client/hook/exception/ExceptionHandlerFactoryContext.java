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
 * Context provided to {@link ExceptionHandlerFactory} implementations providing
 * runtime contextual information.
 */
public class ExceptionHandlerFactoryContext {
    private final String runtimeURI;

    /**
     * Ctor.
     *
     * @param runtimeURI runtime URI
     */
    public ExceptionHandlerFactoryContext(String runtimeURI) {
        this.runtimeURI = runtimeURI;
    }

    /**
     * Returns the runtime URI.
     *
     * @return runtime URI
     */
    public String getRuntimeURI() {
        return runtimeURI;
    }
}
