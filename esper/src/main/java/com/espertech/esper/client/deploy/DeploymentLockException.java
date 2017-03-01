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

    public DeploymentLockException(String message) {
        super(message);
    }

    public DeploymentLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeploymentLockException(Throwable cause) {
        super(cause);
    }
}
