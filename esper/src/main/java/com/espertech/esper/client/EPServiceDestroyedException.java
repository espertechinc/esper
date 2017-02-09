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
 * This exception is thrown to indicate that the EPServiceProvider (engine) instance has been destroyed.
 * <p>
 * This exception applies to destroyed engine instances when a client attempts to receive the runtime
 * or administrative interfaces from a destroyed engine instance.
 */
public class EPServiceDestroyedException extends RuntimeException {
    private static final long serialVersionUID = 14163093254581288L;

    /**
     * Ctor.
     *
     * @param engineURI - engine URI
     */
    public EPServiceDestroyedException(final String engineURI) {
        super("EPServiceProvider has already been destroyed for engine URI '" + engineURI + "'");
    }
}