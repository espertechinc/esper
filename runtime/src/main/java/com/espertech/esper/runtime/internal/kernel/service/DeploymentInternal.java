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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.module.ModuleProperty;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.context.module.ModuleProvider;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.Map;

public class DeploymentInternal {
    private final String deploymentId;
    private final EPStatement[] statements;
    private final String[] deploymentIdDependencies;
    private final String[] pathNamedWindows;
    private final String[] pathTables;
    private final String[] pathVariables;
    private final String[] pathContexts;
    private final String[] pathEventTypes;
    private final String[] pathExprDecls;
    private final NameAndParamNum[] pathScripts;
    private final ModuleIndexMeta[] pathIndexes;
    private final ModuleProvider moduleProvider;
    private final Map<ModuleProperty, Object> modulePropertiesCached;
    private final Map<Long, EventType> deploymentTypes;
    private final long lastUpdateDate;

    public DeploymentInternal(String deploymentId, EPStatement[] statements, String[] deploymentIdDependencies, String[] pathNamedWindows, String[] pathTables, String[] pathVariables, String[] pathContexts, String[] pathEventTypes, String[] pathExprDecls, NameAndParamNum[] pathScripts, ModuleIndexMeta[] pathIndexes, ModuleProvider moduleProvider, Map<ModuleProperty, Object> modulePropertiesCached, Map<Long, EventType> deploymentTypes, long lastUpdateDate) {
        this.deploymentId = deploymentId;
        this.statements = statements;
        this.deploymentIdDependencies = deploymentIdDependencies;
        this.pathNamedWindows = pathNamedWindows;
        this.pathTables = pathTables;
        this.pathVariables = pathVariables;
        this.pathContexts = pathContexts;
        this.pathEventTypes = pathEventTypes;
        this.pathExprDecls = pathExprDecls;
        this.pathScripts = pathScripts;
        this.pathIndexes = pathIndexes;
        this.moduleProvider = moduleProvider;
        this.modulePropertiesCached = modulePropertiesCached;
        this.deploymentTypes = deploymentTypes;
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public EPStatement[] getStatements() {
        return statements;
    }

    public String[] getDeploymentIdDependencies() {
        return deploymentIdDependencies;
    }

    public String[] getPathNamedWindows() {
        return pathNamedWindows;
    }

    public String[] getPathTables() {
        return pathTables;
    }

    public String[] getPathVariables() {
        return pathVariables;
    }

    public String[] getPathContexts() {
        return pathContexts;
    }

    public String[] getPathEventTypes() {
        return pathEventTypes;
    }

    public String[] getPathExprDecls() {
        return pathExprDecls;
    }

    public NameAndParamNum[] getPathScripts() {
        return pathScripts;
    }

    public ModuleIndexMeta[] getPathIndexes() {
        return pathIndexes;
    }

    public ModuleProvider getModuleProvider() {
        return moduleProvider;
    }

    public Map<Long, EventType> getDeploymentTypes() {
        return deploymentTypes;
    }

    public Map<ModuleProperty, Object> getModulePropertiesCached() {
        return modulePropertiesCached;
    }

    public long getLastUpdateDate() {
        return lastUpdateDate;
    }
}
