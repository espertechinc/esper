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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

public class Variable {
    private final int variableNumber;
    private final String deploymentId;
    private final VariableMetaData metaData;
    private final String optionalContextDeploymentId;

    public Variable(int variableNumber, String deploymentId, VariableMetaData metaData, String optionalContextDeploymentId) {
        this.variableNumber = variableNumber;
        this.deploymentId = deploymentId;
        this.metaData = metaData;
        this.optionalContextDeploymentId = optionalContextDeploymentId;
    }

    public int getVariableNumber() {
        return variableNumber;
    }

    public VariableMetaData getMetaData() {
        return metaData;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getOptionalContextDeploymentId() {
        return optionalContextDeploymentId;
    }
}
