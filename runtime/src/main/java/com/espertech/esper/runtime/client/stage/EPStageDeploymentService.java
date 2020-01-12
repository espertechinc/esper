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
package com.espertech.esper.runtime.client.stage;

import com.espertech.esper.runtime.client.EPDeployment;

/**
 * Stage deployment service provides information about deployments staged to a given {@link EPStage}.
 * <p>
 *     This API is under development for version 8.4 and newer, and is considered UNSTABLE.
 * </p>
 */
public interface EPStageDeploymentService {

    /**
     * Returns the staged deployment or null if the deployment is not staged
     * @param deploymentId deployment id
     * @return deployment id
     */
    EPDeployment getDeployment(String deploymentId);

    /**
     * Returns the deployment ids of all staged deployments.
     *
     * @return deployment ids
     */
    String[] getDeployments();
}
