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
 * Exception during a deploy operation by {@link EPDeploymentService#deploy}
 */
public class EPDeployException extends Exception {

    /**
     * Ctor.
     *
     * @param message message
     */
    public EPDeployException(String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param cause cause
     */
    public EPDeployException(Throwable cause) {
        super(cause);
    }

    /**
     * Ctor.
     *
     * @param message message
     * @param cause   cause
     */
    public EPDeployException(String message, Throwable cause) {
        super(message, cause);
    }
}
