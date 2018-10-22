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

import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentRecoveryService;
import com.espertech.esper.runtime.internal.deploymentlifesvc.ListenerRecoveryService;
import com.espertech.esper.runtime.internal.deploymentlifesvc.StatementIdRecoveryService;

public class EPServicesHA {
    private final RuntimeExtensionServices runtimeExtensionServices;
    private final DeploymentRecoveryService deploymentRecoveryService;
    private final ListenerRecoveryService listenerRecoveryService;
    private final StatementIdRecoveryService statementIdRecoveryService;
    private final Long currentTimeAsRecovered;

    public EPServicesHA(RuntimeExtensionServices runtimeExtensionServices, DeploymentRecoveryService deploymentRecoveryService, ListenerRecoveryService listenerRecoveryService, StatementIdRecoveryService statementIdRecoveryService, Long currentTimeAsRecovered) {
        this.runtimeExtensionServices = runtimeExtensionServices;
        this.deploymentRecoveryService = deploymentRecoveryService;
        this.listenerRecoveryService = listenerRecoveryService;
        this.statementIdRecoveryService = statementIdRecoveryService;
        this.currentTimeAsRecovered = currentTimeAsRecovered;
    }

    public RuntimeExtensionServices getRuntimeExtensionServices() {
        return runtimeExtensionServices;
    }

    public DeploymentRecoveryService getDeploymentRecoveryService() {
        return deploymentRecoveryService;
    }

    public ListenerRecoveryService getListenerRecoveryService() {
        return listenerRecoveryService;
    }

    public StatementIdRecoveryService getStatementIdRecoveryService() {
        return statementIdRecoveryService;
    }

    public Long getCurrentTimeAsRecovered() {
        return currentTimeAsRecovered;
    }

    public void destroy() {
        ((RuntimeExtensionServicesSPI) runtimeExtensionServices).destroy();
    }
}
