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
 * Context object for filter for use with the EPStatementSource operator.
 */
public class EPDataFlowEPStatementFilterContext {
    private final String deploymentId;
    private final String statementName;
    private final Object epStatement;

    /**
     * Ctor.
     *
     * @param deploymentId  deployment id
     * @param statementName statement name
     * @param epStatement   statement
     */
    public EPDataFlowEPStatementFilterContext(String deploymentId, String statementName, Object epStatement) {
        this.deploymentId = deploymentId;
        this.statementName = statementName;
        this.epStatement = epStatement;
    }

    /**
     * Returns the deployment id
     *
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Returns the statement name
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the statement, can safely be cast to EPStatement
     *
     * @return statement
     */
    public Object getEpStatement() {
        return epStatement;
    }
}
