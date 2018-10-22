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

import com.espertech.esper.common.client.variable.EPVariableService;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;

import java.util.Map;

public interface EPVariableServiceSPI extends EPVariableService {
    Map<DeploymentIdNamePair, Class> getVariableTypeAll();

    Class getVariableType(String deploymentId, String variableName);
}
