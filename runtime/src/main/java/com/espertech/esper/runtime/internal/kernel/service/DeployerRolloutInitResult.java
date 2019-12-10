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

import java.util.Set;

public class DeployerRolloutInitResult {
    private final Set<String> deploymentIdDependencies;
    private final DeployerModuleEPLObjects moduleEPLObjects;
    private final DeployerModulePaths modulePaths;
    private final String moduleName;

    public DeployerRolloutInitResult(Set<String> deploymentIdDependencies, DeployerModuleEPLObjects moduleEPLObjects, DeployerModulePaths modulePaths, String moduleName) {
        this.deploymentIdDependencies = deploymentIdDependencies;
        this.moduleEPLObjects = moduleEPLObjects;
        this.modulePaths = modulePaths;
        this.moduleName = moduleName;
    }

    public Set<String> getDeploymentIdDependencies() {
        return deploymentIdDependencies;
    }

    public DeployerModuleEPLObjects getModuleEPLObjects() {
        return moduleEPLObjects;
    }

    public DeployerModulePaths getModulePaths() {
        return modulePaths;
    }

    public String getModuleName() {
        return moduleName;
    }
}
