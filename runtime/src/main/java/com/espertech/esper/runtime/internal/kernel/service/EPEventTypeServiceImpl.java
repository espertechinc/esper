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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.runtime.client.EPEventTypeService;

public class EPEventTypeServiceImpl implements EPEventTypeService {
    private final EPServicesContext services;

    public EPEventTypeServiceImpl(EPServicesContext services) {
        this.services = services;
    }

    public EventType getEventTypePreconfigured(String eventTypeName) {
        return services.getEventTypeRepositoryBus().getTypeByName(eventTypeName);
    }

    public EventType getEventType(String deploymentId, String eventTypeName) {
        DeploymentInternal deployment = services.getDeploymentLifecycleService().getDeploymentById(deploymentId);
        if (deployment == null) {
            return null;
        }
        String moduleName = deployment.getModuleProvider().getModuleName();
        return services.getEventTypePathRegistry().getWithModule(eventTypeName, moduleName);
    }
}
