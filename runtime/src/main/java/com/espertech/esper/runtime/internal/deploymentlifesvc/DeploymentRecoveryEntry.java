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
package com.espertech.esper.runtime.internal.deploymentlifesvc;

import com.espertech.esper.common.client.EPCompiled;

import java.util.Map;

public class DeploymentRecoveryEntry {
    private final int statementIdFirstStatement;
    private final EPCompiled compiled;
    private final Map<Integer, Object> userObjectsRuntime;
    private final Map<Integer, String> statementNamesWhenProvidedByAPI;
    private final Map<Integer, Map<Integer, Object>> substitutionParameters;

    public DeploymentRecoveryEntry(int statementIdFirstStatement, EPCompiled compiled, Map<Integer, Object> userObjectsRuntime, Map<Integer, String> statementNamesWhenProvidedByAPI, Map<Integer, Map<Integer, Object>> substitutionParameters) {
        this.statementIdFirstStatement = statementIdFirstStatement;
        this.compiled = compiled;
        this.userObjectsRuntime = userObjectsRuntime;
        this.statementNamesWhenProvidedByAPI = statementNamesWhenProvidedByAPI;
        this.substitutionParameters = substitutionParameters;
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
}
