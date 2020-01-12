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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.collection.PathDeploymentEntry;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesPath;

public class StageDeploymentHelper {

    public static void movePath(DeploymentInternal deployment, EPServicesPath from, EPServicesPath to) {
        String moduleName = deployment.getModuleProvider().getModuleName();
        handleProvided(deployment.getDeploymentId(), deployment.getPathNamedWindows(), from.getNamedWindowPathRegistry(), moduleName, to.getNamedWindowPathRegistry());
        handleProvided(deployment.getDeploymentId(), deployment.getPathTables(), from.getTablePathRegistry(), moduleName, to.getTablePathRegistry());
        handleProvided(deployment.getDeploymentId(), deployment.getPathContexts(), from.getContextPathRegistry(), moduleName, to.getContextPathRegistry());
        handleProvided(deployment.getDeploymentId(), deployment.getPathVariables(), from.getVariablePathRegistry(), moduleName, to.getVariablePathRegistry());
        handleProvided(deployment.getDeploymentId(), deployment.getPathEventTypes(), from.getEventTypePathRegistry(), moduleName, to.getEventTypePathRegistry());
        handleProvided(deployment.getDeploymentId(), deployment.getPathExprDecls(), from.getExprDeclaredPathRegistry(), moduleName, to.getExprDeclaredPathRegistry());
        handleProvided(deployment.getDeploymentId(), deployment.getPathScripts(), from.getScriptPathRegistry(), moduleName, to.getScriptPathRegistry());
    }

    private static <K, E> void handleProvided(String deploymentId, K[] objectNames, PathRegistry<K, E> source, String moduleName, PathRegistry<K, E> target) {
        for (K objectName : objectNames) {
            PathDeploymentEntry<E> e = source.getEntryWithModule(objectName, moduleName);
            if (e != null) {
                try {
                    target.addEntry(objectName, moduleName, e);
                } catch (PathException ex) {
                    throw new EPException(ex);
                }
            }
        }
        source.deleteDeployment(deploymentId);
    }
}
