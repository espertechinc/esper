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
package com.espertech.esper.runtime.client;

/**
 * This exception is thrown to indicate that the runtime instance has been destroyed.
 * <p>
 * This exception applies to destroyed runtime when a client attempts to use the runtime after it was destroyed.
 */
public class EPRuntimeDestroyedException extends RuntimeException {
    private static final long serialVersionUID = 14163093254581288L;

    /**
     * Ctor.
     *
     * @param runtimeURI - runtime URI
     */
    public EPRuntimeDestroyedException(final String runtimeURI) {
        super("Runtime has already been destroyed for runtime URI '" + runtimeURI + "'");
    }
}