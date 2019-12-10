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

import java.util.Map;

public class DeploymentRecoveryInformation {
    private final Map<Integer, Object> statementUserObjectsRuntime;
    private final Map<Integer, String> statementNamesWhenProvidedByAPI;

    public DeploymentRecoveryInformation(Map<Integer, Object> statementUserObjectsRuntime, Map<Integer, String> statementNamesWhenProvidedByAPI) {
        this.statementUserObjectsRuntime = statementUserObjectsRuntime;
        this.statementNamesWhenProvidedByAPI = statementNamesWhenProvidedByAPI;
    }

    public Map<Integer, Object> getStatementUserObjectsRuntime() {
        return statementUserObjectsRuntime;
    }

    public Map<Integer, String> getStatementNamesWhenProvidedByAPI() {
        return statementNamesWhenProvidedByAPI;
    }
}
