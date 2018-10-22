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
 * Deployment event indicating a deployment completed
 */
public class DeploymentStateEventDeployed extends DeploymentStateEvent {

    /**
     * Ctor.
     *
     * @param runtimeURI   runtime uri
     * @param deploymentId deployment id
     * @param moduleName   module name
     * @param statements   statements
     */
    public DeploymentStateEventDeployed(String runtimeURI, String deploymentId, String moduleName, EPStatement[] statements) {
        super(runtimeURI, deploymentId, moduleName, statements);
    }
}
