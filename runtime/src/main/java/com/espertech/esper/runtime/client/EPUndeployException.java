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
 * Exception during an undeploy operation by {@link EPDeploymentService#undeploy}
 */
public class EPUndeployException extends Exception {
    /**
     * Ctor
     *
     * @param message message
     */
    public EPUndeployException(String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message message
     * @param cause   cause
     */
    public EPUndeployException(String message, Throwable cause) {
        super(message, cause);
    }
}
