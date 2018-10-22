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

import com.espertech.esper.common.client.context.EPContextPartitionService;
import com.espertech.esper.common.client.metric.EPMetricsService;
import com.espertech.esper.common.client.variable.EPVariableService;
import com.espertech.esper.runtime.client.EPEventTypeService;
import com.espertech.esper.runtime.client.EPFireAndForgetService;

class EPRuntimeEnv {
    private final EPServicesContext services;
    private final EPEventServiceSPI eventService;
    private final EPDeploymentServiceSPI deploymentService;
    private final EPEventTypeService eventTypeService;
    private final EPContextPartitionService contextPartitionService;
    private final EPVariableService variableService;
    private final EPMetricsService metricsService;
    private final EPFireAndForgetService fireAndForgetService;

    public EPRuntimeEnv(EPServicesContext services, EPEventServiceSPI eventService, EPDeploymentServiceSPI deploymentService, EPEventTypeService eventTypeService, EPContextPartitionService contextPartitionService, EPVariableService variableService, EPMetricsService metricsService, EPFireAndForgetService fireAndForgetService) {
        this.services = services;
        this.eventService = eventService;
        this.deploymentService = deploymentService;
        this.eventTypeService = eventTypeService;
        this.contextPartitionService = contextPartitionService;
        this.variableService = variableService;
        this.metricsService = metricsService;
        this.fireAndForgetService = fireAndForgetService;
    }

    public EPServicesContext getServices() {
        return services;
    }

    public EPEventServiceSPI getRuntime() {
        return eventService;
    }

    public EPDeploymentServiceSPI getDeploymentService() {
        return deploymentService;
    }

    public EPEventTypeService getEventTypeService() {
        return eventTypeService;
    }

    public EPEventServiceSPI getEventService() {
        return eventService;
    }

    public EPContextPartitionService getContextPartitionService() {
        return contextPartitionService;
    }

    public EPVariableService getVariableService() {
        return variableService;
    }

    public EPMetricsService getMetricsService() {
        return metricsService;
    }

    public EPFireAndForgetService getFireAndForgetService() {
        return fireAndForgetService;
    }
}
