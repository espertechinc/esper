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
package com.espertech.esper.common.client.hook.recompile;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.ModuleProperty;

import java.util.List;
import java.util.Map;

public class EPRecompileProviderContext {
    private final EPCompiled compiled;
    private final Configuration configuration;
    private final String deploymentId;
    private final String moduleName;
    private final Map<ModuleProperty, Object> moduleProperties;
    private final List<EPCompiled> path;
    private final int statementIdFirstStatement;
    private final Map<Integer, Object> userObjectsRuntime;
    private final Map<Integer, String> statementNamesWhenProvidedByAPI;
    private final Map<Integer, Map<Integer, Object>> substitutionParameters;

    public EPRecompileProviderContext(EPCompiled compiled, Configuration configuration, String deploymentId, String moduleName, Map<ModuleProperty, Object> moduleProperties, List<EPCompiled> path, int statementIdFirstStatement, Map<Integer, Object> userObjectsRuntime, Map<Integer, String> statementNamesWhenProvidedByAPI, Map<Integer, Map<Integer, Object>> substitutionParameters) {
        this.compiled = compiled;
        this.configuration = configuration;
        this.deploymentId = deploymentId;
        this.moduleName = moduleName;
        this.moduleProperties = moduleProperties;
        this.path = path;
        this.statementIdFirstStatement = statementIdFirstStatement;
        this.userObjectsRuntime = userObjectsRuntime;
        this.statementNamesWhenProvidedByAPI = statementNamesWhenProvidedByAPI;
        this.substitutionParameters = substitutionParameters;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Map<ModuleProperty, Object> getModuleProperties() {
        return moduleProperties;
    }

    public int getStatementIdFirstStatement() {
        return statementIdFirstStatement;
    }

    public EPCompiled getCompiled() {
        return compiled;
    }

    public Map<Integer, Object> getUserObjectsRuntime() {
        return userObjectsRuntime;
    }

    public Map<Integer, String> getStatementNamesWhenProvidedByAPI() {
        return statementNamesWhenProvidedByAPI;
    }

    public Map<Integer, Map<Integer, Object>> getSubstitutionParameters() {
        return substitutionParameters;
    }

    public List<EPCompiled> getPath() {
        return path;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
