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

import com.espertech.esper.common.client.module.ModuleProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Represents a deployment.
 */
public class EPDeployment implements Serializable {

    private final String deploymentId;
    private final String moduleName;
    private final Map<ModuleProperty, Object> moduleProperties;
    private final EPStatement[] statements;
    private final String[] deploymentIdDependencies;
    private final Date lastUpdateDate;

    /**
     * Ctor.
     *
     * @param deploymentId             deployment id
     * @param moduleName               module name or null if none provided
     * @param moduleProperties         module properties
     * @param statements               statements
     * @param deploymentIdDependencies array of deployment ids that this deployment depends
     * @param lastUpdateDate           last update date
     */
    public EPDeployment(String deploymentId, String moduleName, Map<ModuleProperty, Object> moduleProperties, EPStatement[] statements, String[] deploymentIdDependencies, Date lastUpdateDate) {
        this.deploymentId = deploymentId;
        this.moduleName = moduleName;
        this.moduleProperties = moduleProperties;
        this.statements = statements;
        this.deploymentIdDependencies = deploymentIdDependencies;
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * Returns the statements
     *
     * @return statements
     */
    public EPStatement[] getStatements() {
        return statements;
    }

    /**
     * Returns the module name or null if none provided
     *
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns module properties
     *
     * @return module properties
     */
    public Map<ModuleProperty, Object> getModuleProperties() {
        return moduleProperties;
    }

    /**
     * Returns the last update date
     *
     * @return last update date
     */
    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * Returns the deployment ids of the deployments that this deployment depends on
     *
     * @return deployment id array of dependencies
     */
    public String[] getDeploymentIdDependencies() {
        return deploymentIdDependencies;
    }

    /**
     * Returns the deployment id
     *
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }
}
