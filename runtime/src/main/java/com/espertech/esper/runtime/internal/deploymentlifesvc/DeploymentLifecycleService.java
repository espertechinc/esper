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

import com.espertech.esper.runtime.client.DeploymentStateListener;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public interface DeploymentLifecycleService {
    void addDeployment(String deploymentId, DeploymentInternal deployment);

    String[] getDeploymentIds();

    DeploymentInternal undeploy(String deploymentId);

    DeploymentInternal getDeploymentByCRC(long deploymentId);

    DeploymentInternal getDeploymentById(String deploymentId);

    EPStatement getStatementByName(String deploymentId, String statementName);

    Map<String, DeploymentInternal> getDeploymentMap();

    CopyOnWriteArrayList<DeploymentStateListener> getListeners();

    void addStatementLifecycleListener(StatementListenerEventObserver observer);
    void removeStatementLifecycleListener(StatementListenerEventObserver observer);
    void dispatchStatementListenerEvent(StatementListenerEvent event);
}
