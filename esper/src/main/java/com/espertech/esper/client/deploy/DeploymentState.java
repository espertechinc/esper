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
 * Deployment state.
 */
public enum DeploymentState {
    /**
     * In undeployed state a deployment is added but not currently deployed.
     */
    UNDEPLOYED,

    /**
     * In deployed state a deployment is added and it is deployed, i.e. has zero to many active EPL statements
     * associated.
     */
    DEPLOYED
}
