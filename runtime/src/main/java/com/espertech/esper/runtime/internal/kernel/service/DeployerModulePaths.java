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
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;

import java.util.List;
import java.util.Map;

public class DeployerModulePaths {
    private final Map<Long, EventType> deploymentTypes;
    private final List<String> pathEventTypes;
    private final List<String> pathNamedWindows;
    private final List<String> pathTables;
    private final List<String> pathContexts;
    private final List<String> pathVariables;
    private final List<String> pathExprDecl;
    private final List<NameAndParamNum> pathScripts;
    private final List<String> pathClassProvideds;

    public DeployerModulePaths(Map<Long, EventType> deploymentTypes, List<String> pathEventTypes, List<String> pathNamedWindows, List<String> pathTables, List<String> pathContexts, List<String> pathVariables, List<String> pathExprDecl, List<NameAndParamNum> pathScripts, List<String> pathClassProvideds) {
        this.deploymentTypes = deploymentTypes;
        this.pathEventTypes = pathEventTypes;
        this.pathNamedWindows = pathNamedWindows;
        this.pathTables = pathTables;
        this.pathContexts = pathContexts;
        this.pathVariables = pathVariables;
        this.pathExprDecl = pathExprDecl;
        this.pathScripts = pathScripts;
        this.pathClassProvideds = pathClassProvideds;
    }

    public Map<Long, EventType> getDeploymentTypes() {
        return deploymentTypes;
    }

    public List<String> getPathEventTypes() {
        return pathEventTypes;
    }

    public List<String> getPathNamedWindows() {
        return pathNamedWindows;
    }

    public List<String> getPathTables() {
        return pathTables;
    }

    public List<String> getPathContexts() {
        return pathContexts;
    }

    public List<String> getPathVariables() {
        return pathVariables;
    }

    public List<String> getPathExprDecl() {
        return pathExprDecl;
    }

    public List<NameAndParamNum> getPathScripts() {
        return pathScripts;
    }

    public List<String> getPathClassProvideds() {
        return pathClassProvideds;
    }
}