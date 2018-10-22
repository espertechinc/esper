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
package com.espertech.esper.runtime.client.option;

import java.lang.annotation.Annotation;

/**
 * Provides the environment to {@link StatementNameRuntimeOption}.
 */
public class StatementNameRuntimeContext {
    private final String deploymentId;
    private final String statementName;
    private final int statementId;
    private final String epl;
    private final Annotation[] annotations;

    /**
     * Ctor.
     *
     * @param deploymentId  deployment id
     * @param statementName statement name
     * @param statementId   statement number
     * @param epl           epl when attached or null
     * @param annotations   annotations
     */
    public StatementNameRuntimeContext(String deploymentId, String statementName, int statementId, String epl, Annotation[] annotations) {
        this.deploymentId = deploymentId;
        this.statementName = statementName;
        this.statementId = statementId;
        this.epl = epl;
        this.annotations = annotations;
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
     * Returns the statement number
     *
     * @return statement number
     */
    public int getStatementId() {
        return statementId;
    }

    /**
     * Returns the EPL when attached or null when not available
     *
     * @return epl
     */
    public String getEpl() {
        return epl;
    }

    /**
     * Returns the annotations.
     *
     * @return annotations
     */
    public Annotation[] getAnnotations() {
        return annotations;
    }
}
