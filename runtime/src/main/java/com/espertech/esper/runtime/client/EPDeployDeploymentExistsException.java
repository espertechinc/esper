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
 * Deploy exception to indicate that a deployment by the same deployment id already exists
 */
public class EPDeployDeploymentExistsException extends EPDeployException {

    /**
     * Ctor.
     *
     * @param message message
     */
    public EPDeployDeploymentExistsException(String message) {
        super(message);
    }
}
