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

import com.espertech.esper.common.client.EPCompiled;

import java.util.Collection;

/**
 * For use with rollout as described in {@link EPDeploymentService#rollout(Collection, RolloutOptions)},
 * for passing a compilation unit and the deployment options for the compilation unit.
 */
public class EPDeploymentRolloutCompiled {
    private final EPCompiled compiled;
    private final DeploymentOptions options;

    /**
     * Ctor, assumes default deployment options
     * @param compiled compiled module to deploy
     */
    public EPDeploymentRolloutCompiled(EPCompiled compiled) {
        this.compiled = compiled;
        this.options = new DeploymentOptions();
    }

    /**
     * Ctor.
     * @param compiled compiled module to deploy
     * @param options deployment options
     */
    public EPDeploymentRolloutCompiled(EPCompiled compiled, DeploymentOptions options) {
        this.compiled = compiled;
        this.options = options == null ? new DeploymentOptions() : options;
    }

    /**
     * Returns the compiled module.
     * @return compiled module
     */
    public EPCompiled getCompiled() {
        return compiled;
    }

    /**
     * Returns the deployment options
     * @return deployment options
     */
    public DeploymentOptions getOptions() {
        return options;
    }
}
