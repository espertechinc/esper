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

import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;

import java.util.Date;

public class EPDeploymentServiceUtil {
    public static EPDeployment toDeployment(DeploymentLifecycleService deploymentLifecycleService, String deploymentId) {
        DeploymentInternal deployed = deploymentLifecycleService.getDeploymentById(deploymentId);
        if (deployed == null) {
            return null;
        }
        EPStatement[] stmts = deployed.getStatements();
        EPStatement[] copy = new EPStatement[stmts.length];
        System.arraycopy(stmts, 0, copy, 0, stmts.length);
        return new EPDeployment(deploymentId, deployed.getModuleProvider().getModuleName(), deployed.getModulePropertiesCached(), copy, CollectionUtil.copyArray(deployed.getDeploymentIdDependencies()), new Date(deployed.getLastUpdateDate()));
    }
}
