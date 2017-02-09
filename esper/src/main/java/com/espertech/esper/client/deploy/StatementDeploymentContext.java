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
 * Context object passed to {@link StatementNameResolver} or {@link StatementUserObjectResolver} to help
 * in determining the right statement name or user object for a statement deployed via the deployment admin API.
 */
public class StatementDeploymentContext {
    private final String epl;
    private final Module module;
    private final ModuleItem moduleItem;
    private final String deploymentId;

    /**
     * Ctor.
     *
     * @param epl          EPL expression
     * @param module       encapsulating module
     * @param moduleItem   item in module
     * @param deploymentId deployment id
     */
    public StatementDeploymentContext(String epl, Module module, ModuleItem moduleItem, String deploymentId) {
        this.epl = epl;
        this.module = module;
        this.moduleItem = moduleItem;
        this.deploymentId = deploymentId;
    }

    /**
     * Returns the EPL expression.
     *
     * @return EPL
     */
    public String getEpl() {
        return epl;
    }

    /**
     * Returns the module.
     *
     * @return module
     */
    public Module getModule() {
        return module;
    }

    /**
     * Returns the deployment id.
     *
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Returns the module item.
     *
     * @return module item
     */
    public ModuleItem getModuleItem() {
        return moduleItem;
    }
}
