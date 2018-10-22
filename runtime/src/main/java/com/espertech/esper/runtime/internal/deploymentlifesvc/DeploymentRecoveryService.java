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

import java.util.Iterator;
import java.util.Map;

public interface DeploymentRecoveryService {
    void add(String deploymentId, int statementIdFirstStatement, EPCompiled compiled, Map<Integer, Object> userObjectsRuntime, Map<Integer, String> statementNamesWhenProvidedByAPI, Map<Integer, Map<Integer, Object>> substitutionParameters);

    Iterator<Map.Entry<String, DeploymentRecoveryEntry>> deployments();

    void remove(String deploymentId);
}
