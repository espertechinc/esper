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
 * Thrown to indicate a configuration problem.
 */
final public class ConfigurationException extends EPException {
    private static final long serialVersionUID = -4664999702525699192L;

    /**
     * Ctor.
     *
     * @param message - error message
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Ctor for an inner exception and message.
     *
     * @param message - error message
     * @param cause   - inner exception
     */
    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Ctor - just an inner exception.
     *
     * @param cause - inner exception
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }
}
