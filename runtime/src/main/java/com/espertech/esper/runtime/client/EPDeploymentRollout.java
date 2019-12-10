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
 * Contains the result of a rollout as described in {@link EPDeploymentService#rollout(Collection, RolloutOptions)},
 * captures the rollout result wherein the deployment result of each compilation unit is provided by {@link EPDeploymentRolloutItem}.
 */
public class EPDeploymentRollout {
    private final EPDeploymentRolloutItem[] items;

    /**
     * Ctor.
     * @param items deployment items
     */
    public EPDeploymentRollout(EPDeploymentRolloutItem[] items) {
        this.items = items;
    }

    /**
     * Returns the deployment items
     * @return deployment items
     */
    public EPDeploymentRolloutItem[] getItems() {
        return items;
    }
}
