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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.client.EPDeploymentDependencyConsumed;
import com.espertech.esper.runtime.client.EPDeploymentDependencyProvided;
import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.Collections;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertEqualsAnyOrder;
import static org.junit.Assert.assertTrue;

public class SupportDeploymentDependencies {
    public static void assertEmpty(RegressionEnvironment env, String deployedStatementName) {
        String deploymentId = env.deploymentId(deployedStatementName);
        EPDeploymentDependencyConsumed consumed = env.runtime().getDeploymentService().getDeploymentDependenciesConsumed(deploymentId);
        assertTrue(consumed.getDependencies().isEmpty());
        EPDeploymentDependencyProvided provided = env.runtime().getDeploymentService().getDeploymentDependenciesProvided(deploymentId);
        assertTrue(provided.getDependencies().isEmpty());
    }

    public static void assertSingle(RegressionEnvironment env, String deployedStmtNameConsume, String deployedStmtNameProvide, EPObjectType objectType, String objectName) {
        String deploymentIdConsume = env.deploymentId(deployedStmtNameConsume);
        String deploymentIdProvide = env.deploymentId(deployedStmtNameProvide);
        EPDeploymentDependencyConsumed consumed = env.runtime().getDeploymentService().getDeploymentDependenciesConsumed(deploymentIdConsume);
        assertEqualsAnyOrder(new EPDeploymentDependencyConsumed.Item[]{new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, objectType, objectName)}, consumed.getDependencies().toArray());
        EPDeploymentDependencyProvided provided = env.runtime().getDeploymentService().getDeploymentDependenciesProvided(deploymentIdProvide);
        assertEqualsAnyOrder(new EPDeploymentDependencyProvided.Item[]{new EPDeploymentDependencyProvided.Item(objectType, objectName, Collections.singleton(deploymentIdConsume))}, provided.getDependencies().toArray());
    }
}
