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
package com.espertech.esper.core.deploy;

import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.util.UuidGenerator;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for storing deployment state.
 */
public class DeploymentStateServiceImpl implements DeploymentStateService {
    private final Map<String, DeploymentInformation> deployments;

    public DeploymentStateServiceImpl() {
        deployments = new ConcurrentHashMap<String, DeploymentInformation>();
    }

    public String nextDeploymentId() {
        return UuidGenerator.generate();
    }

    public synchronized DeploymentInformation[] getAllDeployments() {
        Collection<DeploymentInformation> dep = deployments.values();
        return dep.toArray(new DeploymentInformation[dep.size()]);
    }

    public synchronized void addUpdateDeployment(DeploymentInformation descriptor) {
        deployments.put(descriptor.getDeploymentId(), descriptor);
    }

    public synchronized void remove(String deploymentId) {
        deployments.remove(deploymentId);
    }

    public synchronized String[] getDeployments() {
        Set<String> keys = deployments.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    public synchronized DeploymentInformation getDeployment(String deploymentId) {
        if (deploymentId == null) {
            return null;
        }
        return deployments.get(deploymentId);
    }

    public synchronized void destroy() {
        deployments.clear();
    }
}
