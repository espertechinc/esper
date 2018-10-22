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
package com.espertech.esper.common.client.hook.condition;

/**
 * Context provided to {@link ConditionHandler} implementations providing
 * runtime-condition-contextual information.
 * <p>
 * Statement information pertains to the statement currently being processed when the condition occured.
 */
public class ConditionHandlerContext {
    private final String runtimeURI;
    private final String statementName;
    private final String deploymentId;
    private final BaseCondition condition;

    /**
     * Ctor.
     *
     * @param runtimeURI    runtime URI
     * @param statementName statement name
     * @param deploymentId  statement deployment-id
     * @param condition     condition reported
     */
    public ConditionHandlerContext(String runtimeURI, String statementName, String deploymentId, BaseCondition condition) {
        this.runtimeURI = runtimeURI;
        this.statementName = statementName;
        this.deploymentId = deploymentId;
        this.condition = condition;
    }

    /**
     * Returns the runtime URI.
     *
     * @return runtime URI
     */
    public String getRuntimeURI() {
        return runtimeURI;
    }

    /**
     * Returns the statement name, if provided, or the statement id assigned to the statement if no name was provided.
     *
     * @return statement name or id
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the deployment id of the statement.
     *
     * @return statement.
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Returns the condition reported.
     *
     * @return condition reported
     */
    public BaseCondition getCondition() {
        return condition;
    }
}
