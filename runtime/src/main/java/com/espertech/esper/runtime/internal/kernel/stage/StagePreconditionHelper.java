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

import com.espertech.esper.runtime.client.EPDeploymentDependencyConsumed;
import com.espertech.esper.runtime.client.EPDeploymentDependencyProvided;
import com.espertech.esper.runtime.client.stage.EPStageException;
import com.espertech.esper.runtime.client.stage.EPStagePreconditionException;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesPath;

import java.util.Set;

import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperDependencies.getDependenciesConsumed;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperDependencies.getDependenciesProvided;

public class StagePreconditionHelper {
    public static void validateDependencyPreconditions(Set<String> deploymentSet, EPServicesPath paths, DeploymentLifecycleService deploymentLifecycleService) throws EPStageException {
        for (String deploymentId : deploymentSet) {
            EPDeploymentDependencyConsumed consumed = getDependenciesConsumed(deploymentId, paths, deploymentLifecycleService);
            if (consumed == null) {
                throw new EPStageException("Deployment '" + deploymentId + "' was not found");
            }

            for (EPDeploymentDependencyConsumed.Item item : consumed.getDependencies()) {
                if (!deploymentSet.contains(item.getDeploymentId())) {
                    String message = "Failed to stage deployment '" + deploymentId + "': Deployment consumes " +
                        item.getObjectType().getPrettyName() + " '" + item.getObjectName() + "'" +
                        " from deployment '" + item.getDeploymentId() + "' and must therefore also be staged";
                    throw new EPStagePreconditionException(message);
                }
            }

            EPDeploymentDependencyProvided provided = getDependenciesProvided(deploymentId, paths, deploymentLifecycleService);
            for (EPDeploymentDependencyProvided.Item item : provided.getDependencies()) {
                for (String other : item.getDeploymentIds()) {
                    if (!deploymentSet.contains(other)) {
                        String message = "Failed to stage deployment '" + deploymentId + "': Deployment provides " +
                            item.getObjectType().getPrettyName() + " '" + item.getObjectName() + "'" +
                            " to deployment '" + other + "' and must therefore also be staged";
                        throw new EPStagePreconditionException(message);
                    }
                }
            }
        }
    }
}
