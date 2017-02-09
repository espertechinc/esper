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
package com.espertech.esper.client.deploy;

/**
 * Base deployment exception.
 */
public class DeploymentException extends Exception {

    private static final long serialVersionUID = 6859620436230176552L;

    /**
     * Ctor.
     *
     * @param message error message
     */
    public DeploymentException(String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message error message
     * @param cause   cause
     */
    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Ctor.
     *
     * @param cause cause
     */
    public DeploymentException(Throwable cause) {
        super(cause);
    }
}