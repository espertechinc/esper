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
 * Deployment base event.
 */
public abstract class DeploymentStateEvent {
    private final String runtimeURI;
    private final String deploymentId;
    private final String moduleName;
    private final EPStatement[] statements;

    /**
     * Ctor
     *
     * @param runtimeURI   runtime URI
     * @param deploymentId deployment id
     * @param moduleName   module name
     * @param statements   statements
     */
    public DeploymentStateEvent(String runtimeURI, String deploymentId, String moduleName, EPStatement[] statements) {
        this.runtimeURI = runtimeURI;
        this.deploymentId = deploymentId;
        this.moduleName = moduleName;
        this.statements = statements;
    }

    /**
     * Returns the runtime uri
     *
     * @return runtime uri
     */
    public String getRuntimeURI() {
        return runtimeURI;
    }

    /**
     * Returns the deployment id
     *
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Returns the module name, when provided, or null if none provided
     *
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns the statements for the deployment or undeployment
     *
     * @return statements
     */
    public EPStatement[] getStatements() {
        return statements;
    }
}
