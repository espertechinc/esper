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
 * Listener for events in respect to deployment and undeployment.
 */
public interface DeploymentStateListener {
    /**
     * Called when a deployment completed
     *
     * @param event deployment information
     */
    void onDeployment(DeploymentStateEventDeployed event);

    /**
     * Called when an undeployment completed
     *
     * @param event undeployment information
     */
    void onUndeployment(DeploymentStateEventUndeployed event);
}
