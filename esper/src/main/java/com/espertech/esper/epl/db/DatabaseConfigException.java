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
package com.espertech.esper.epl.db;

/**
 * Exception to indicate that a stream name could not be resolved.
 */
public class DatabaseConfigException extends Exception {
    private static final long serialVersionUID = 6493251258537897912L;

    /**
     * Ctor.
     *
     * @param msg - message
     */
    public DatabaseConfigException(String msg) {
        super(msg);
    }

    /**
     * Ctor.
     *
     * @param message - error message
     * @param cause   - cause is the inner exception
     */
    public DatabaseConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
