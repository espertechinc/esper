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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class NamedWindowManagementServiceImpl implements NamedWindowManagementService {
    private final Map<String, NamedWindowDeployment> deployments = new HashMap<>();

    public void addNamedWindow(String windowName, NamedWindowMetaData metadata, EPStatementInitServices services) {
        NamedWindowDeployment deployment = deployments.get(services.getDeploymentId());
        if (deployment == null) {
            deployment = new NamedWindowDeployment();
            deployments.put(services.getDeploymentId(), deployment);
        }
        deployment.add(windowName, metadata, services);
    }

    public NamedWindow getNamedWindow(String deploymentId, String namedWindowName) {
        NamedWindowDeployment deployment = deployments.get(deploymentId);
        return deployment == null ? null : deployment.getProcessor(namedWindowName);
    }

    public int getDeploymentCount() {
        return deployments.size();
    }

    public void destroyNamedWindow(String deploymentId, String namedWindowName) {
        NamedWindowDeployment deployment = deployments.get(deploymentId);
        if (deployment == null) {
            return;
        }
        deployment.remove(namedWindowName);
        if (deployment.isEmpty()) {
            deployments.remove(deploymentId);
        }
    }

    public void traverseNamedWindows(BiConsumer<String, NamedWindow> consumer) {
        for (Map.Entry<String, NamedWindowDeployment> entry : deployments.entrySet()) {
            for (Map.Entry<String, NamedWindow> nw : entry.getValue().getNamedWindows().entrySet()) {
                consumer.accept(entry.getKey(), nw.getValue());
            }
        }
    }
}
