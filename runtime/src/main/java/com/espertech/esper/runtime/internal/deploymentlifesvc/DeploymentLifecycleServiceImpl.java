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

import com.espertech.esper.common.internal.util.CRC32Util;
import com.espertech.esper.runtime.client.DeploymentStateListener;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeploymentLifecycleServiceImpl implements DeploymentLifecycleService {
    private final Map<String, DeploymentInternal> deploymentsByName = new HashMap<>();
    private final Map<Long, DeploymentInternal> deploymentsByCRC = new HashMap<>();
    private final CopyOnWriteArrayList<DeploymentStateListener> listeners = new CopyOnWriteArrayList<>();

    public void addDeployment(String deploymentId, DeploymentInternal deployment) {
        DeploymentInternal existing = deploymentsByName.get(deploymentId);
        if (existing != null) {
            throw new IllegalStateException("Deployment already exists by name '" + deploymentId + "'");
        }
        long crc = CRC32Util.computeCRC32(deploymentId);
        existing = deploymentsByCRC.get(crc);
        if (existing != null) {
            throw new IllegalStateException("Deployment already exists by same-value crc");
        }

        deploymentsByName.put(deploymentId, deployment);
        deploymentsByCRC.put(crc, deployment);
    }

    public String[] getDeploymentIds() {
        Set<String> keys = deploymentsByName.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    public DeploymentInternal undeploy(String deploymentId) {
        DeploymentInternal deployment = deploymentsByName.remove(deploymentId);
        if (deployment != null) {
            long crc = CRC32Util.computeCRC32(deploymentId);
            deploymentsByCRC.remove(crc);
        }
        return deployment;
    }

    public DeploymentInternal getDeploymentByCRC(long deploymentId) {
        return deploymentsByCRC.get(deploymentId);
    }

    public DeploymentInternal getDeploymentById(String deploymentId) {
        return deploymentsByName.get(deploymentId);
    }

    public EPStatement getStatementByName(String deploymentId, String statementName) {
        DeploymentInternal deployment = deploymentsByName.get(deploymentId);
        if (deployment == null) {
            return null;
        }
        for (EPStatement stmt : deployment.getStatements()) {
            if (stmt.getName().equals(statementName)) {
                return stmt;
            }
        }
        return null;
    }

    public Map<String, DeploymentInternal> getDeploymentMap() {
        return deploymentsByName;
    }

    public CopyOnWriteArrayList<DeploymentStateListener> getListeners() {
        return listeners;
    }
}
