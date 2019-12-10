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

import java.util.Collection;

/**
 * The result item of a rollout as described in {@link EPDeploymentService#rollout(Collection, RolloutOptions)},
 * captures the rollout result of a single compilation unit that was deployed as part of a rollout.
 */
public class EPDeploymentRolloutItem {
    private final EPDeployment deployment;

    /**
     * Ctor.
     * @param deployment deployment
     */
    public EPDeploymentRolloutItem(EPDeployment deployment) {
        this.deployment = deployment;
    }

    /**
     * Returns the deployment.
     * @return deployment
     */
    public EPDeployment getDeployment() {
        return deployment;
    }
}
