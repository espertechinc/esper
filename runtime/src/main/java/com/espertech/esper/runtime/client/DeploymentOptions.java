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
package com.espertech.esper.runtime.client;

import com.espertech.esper.runtime.client.option.StatementNameRuntimeOption;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;
import com.espertech.esper.runtime.client.option.StatementUserObjectRuntimeOption;
import com.espertech.esper.runtime.client.util.LockStrategy;
import com.espertech.esper.runtime.client.util.LockStrategyDefault;

/**
 * Option holder for use with {@link EPDeploymentService#deploy}.
 */
public class DeploymentOptions {
    private String deploymentId;
    private StatementUserObjectRuntimeOption statementUserObjectRuntime;
    private StatementNameRuntimeOption statementNameRuntime;
    private StatementSubstitutionParameterOption statementSubstitutionParameter;
    private LockStrategy deploymentLockStrategy = LockStrategyDefault.INSTANCE;

    /**
     * Returns the deployment id if one should be assigned; A null value causes the runtime to generate and assign a deployment id.
     *
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Sets the deployment id if one should be assigned; A null value causes the runtime to generate and assign a deployment id.
     *
     * @param deploymentId deployment id
     * @return itself
     */
    public DeploymentOptions setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
        return this;
    }

    /**
     * Returns the callback providing a runtime statement user object that can be obtained using {@link EPStatement#getUserObjectRuntime()}
     *
     * @return callback
     */
    public StatementUserObjectRuntimeOption getStatementUserObjectRuntime() {
        return statementUserObjectRuntime;
    }

    /**
     * Sets the callback providing a runtime statement user object that can be obtained using {@link EPStatement#getUserObjectRuntime()}
     *
     * @param statementUserObjectRuntime callback
     * @return itself
     */
    public DeploymentOptions setStatementUserObjectRuntime(StatementUserObjectRuntimeOption statementUserObjectRuntime) {
        this.statementUserObjectRuntime = statementUserObjectRuntime;
        return this;
    }

    /**
     * Returns the callback overriding the statement name that identifies the statement within the deployment and that
     * can be obtained using {@link EPStatement#getName()}
     *
     * @return callback
     */
    public StatementNameRuntimeOption getStatementNameRuntime() {
        return statementNameRuntime;
    }

    /**
     * Sets the callback overriding the statement name that identifies the statement within the deployment and that
     * can be obtained using {@link EPStatement#getName()}
     *
     * @param statementNameRuntime callback
     * @return itself
     */
    public DeploymentOptions setStatementNameRuntime(StatementNameRuntimeOption statementNameRuntime) {
        this.statementNameRuntime = statementNameRuntime;
        return this;
    }

    /**
     * Return the deployment lock strategy, the default is {@link LockStrategyDefault}
     *
     * @return lock strategy
     */
    public LockStrategy getDeploymentLockStrategy() {
        return deploymentLockStrategy;
    }

    /**
     * Sets the deployment lock strategy, the default is {@link LockStrategyDefault}
     *
     * @param deploymentLockStrategy lock strategy
     * @return itself
     */
    public DeploymentOptions setDeploymentLockStrategy(LockStrategy deploymentLockStrategy) {
        this.deploymentLockStrategy = deploymentLockStrategy;
        return this;
    }

    /**
     * Returns the callback providing values for substitution parameters.
     *
     * @return callback
     */
    public StatementSubstitutionParameterOption getStatementSubstitutionParameter() {
        return statementSubstitutionParameter;
    }

    /**
     * Sets the callback providing values for substitution parameters.
     *
     * @param statementSubstitutionParameter callback
     * @return itself
     */
    public DeploymentOptions setStatementSubstitutionParameter(StatementSubstitutionParameterOption statementSubstitutionParameter) {
        this.statementSubstitutionParameter = statementSubstitutionParameter;
        return this;
    }
}
