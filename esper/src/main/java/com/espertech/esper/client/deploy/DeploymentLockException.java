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
 * Exception to indicate a problem taking a lock
 */
public class DeploymentLockException extends DeploymentException {

    private static final long serialVersionUID = -889687180064957731L;

    /**
     * Ctor.
     * @param message message
     */
    public DeploymentLockException(String message) {
        super(message);
    }

    /**
     * Ctor
     * @param message message
     * @param cause cause
     */
    public DeploymentLockException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Ctor.
     * @param cause cause
     */
    public DeploymentLockException(Throwable cause) {
        super(cause);
    }
}
