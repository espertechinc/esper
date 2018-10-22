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
 * Deploy exception to indicate that a precondition is not satisfied
 */
public class EPDeployPreconditionException extends EPDeployException {

    /**
     * Ctor.
     *
     * @param message message
     */
    public EPDeployPreconditionException(String message) {
        super("A precondition is not satisfied: " + message);
    }

    /**
     * Ctor.
     *
     * @param message message
     * @param cause   cause
     */
    public EPDeployPreconditionException(String message, Throwable cause) {
        super("A precondition is not satisfied: " + message, cause);
    }
}
