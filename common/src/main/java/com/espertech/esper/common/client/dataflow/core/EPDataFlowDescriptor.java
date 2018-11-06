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
package com.espertech.esper.common.client.dataflow.core;

/**
 * Data flow descriptor.
 */
public class EPDataFlowDescriptor {
    private final String deploymentId;
    private final String dataFlowName;
    private final String statementName;

    /**
     * Ctor.
     *
     * @param deploymentId deployment id
     * @param dataFlowName  data flow name
     * @param statementName statement name
     */
    public EPDataFlowDescriptor(String deploymentId, String dataFlowName, String statementName) {
        this.deploymentId = deploymentId;
        this.dataFlowName = dataFlowName;
        this.statementName = statementName;
    }

    /**
     * Returns the data flow name.
     *
     * @return name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }

    /**
     * Returns the statement name.
     *
     * @return statement name.
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the deployment id
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }
}
