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
 * Inner exception to {@link com.espertech.esper.client.deploy.DeploymentActionException} available on statement level.
 */
public class DeploymentNotFoundException extends DeploymentException {
    private static final long serialVersionUID = -1243745018013856125L;

    /**
     * Ctor.
     *
     * @param message error message
     */
    public DeploymentNotFoundException(String message) {
        super(message);
    }
}
