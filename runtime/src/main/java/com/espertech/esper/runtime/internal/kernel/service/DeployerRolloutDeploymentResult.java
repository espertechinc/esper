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
package com.espertech.esper.runtime.internal.kernel.service;

public class DeployerRolloutDeploymentResult {
    private final int numStatements;
    private final DeploymentInternal[] deployments;

    public DeployerRolloutDeploymentResult(int numStatements, DeploymentInternal[] deployments) {
        this.numStatements = numStatements;
        this.deployments = deployments;
    }

    public int getNumStatements() {
        return numStatements;
    }

    public DeploymentInternal[] getDeployments() {
        return deployments;
    }
}
