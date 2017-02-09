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
 * Inner exception to {@link DeploymentActionException} available on statement level.
 */
public class DeploymentStateException extends DeploymentException {

    private static final long serialVersionUID = 8451246235746829231L;

    /**
     * Ctor.
     *
     * @param message error message
     */
    public DeploymentStateException(String message) {
        super(message);
    }
}
