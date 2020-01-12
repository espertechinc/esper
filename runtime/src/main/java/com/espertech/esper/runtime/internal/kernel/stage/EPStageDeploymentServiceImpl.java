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

import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;
import com.espertech.esper.runtime.internal.kernel.service.EPDeploymentServiceUtil;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;

import java.util.Map;

public class EPStageDeploymentServiceImpl implements EPStageDeploymentServiceSPI {
    private final String stageUri;
    private final EPServicesContext servicesContext;
    private final StageSpecificServices stageSpecificServices;

    public EPStageDeploymentServiceImpl(String stageUri, EPServicesContext servicesContext, StageSpecificServices stageSpecificServices) {
        this.stageUri = stageUri;
        this.servicesContext = servicesContext;
        this.stageSpecificServices = stageSpecificServices;
    }

    public EPDeployment getDeployment(String deploymentId) {
        return EPDeploymentServiceUtil.toDeployment(stageSpecificServices.getDeploymentLifecycleService(), deploymentId);
    }

    public Map<String, DeploymentInternal> getDeploymentMap() {
        return stageSpecificServices.getDeploymentLifecycleService().getDeploymentMap();
    }

    public String[] getDeployments() {
        return stageSpecificServices.getDeploymentLifecycleService().getDeploymentIds();
    }
}
