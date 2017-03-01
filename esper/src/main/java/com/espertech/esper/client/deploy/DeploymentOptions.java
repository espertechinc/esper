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
package com.espertech.esper.client.deploy;

import java.io.Serializable;

/**
 * Options for use in deployment of a module to control the behavior of the deploy operation.
 */
public class DeploymentOptions implements Serializable {

    private static final long serialVersionUID = 119383576416141182L;

    private boolean compile = true;
    private boolean compileOnly = false;
    private boolean rollbackOnFail = true;
    private boolean failFast = true;
    private String isolatedServiceProvider = null;
    private boolean validateOnly = false;
    private StatementNameResolver statementNameResolver;
    private StatementUserObjectResolver statementUserObjectResolver;
    private DeploymentLockStrategy deploymentLockStrategy = DeploymentLockStrategyDefault.INSTANCE;

    /**
     * Returns true (the default) to indicate that the deploy operation first performs a compile step for
     * each statement before attempting to start a statement.
     *
     * @return true for compile before start, false for start-only
     */
    public boolean isCompile() {
        return compile;
    }

    /**
     * Set this indicator to true (the default) to indicate that the deploy operation first performs a compile step for
     * each statement before attempting to start a statement.
     *
     * @param compile true for compile before start, false for start-only
     */
    public void setCompile(boolean compile) {
        this.compile = compile;
    }

    /**
     * Returns true (the default) to indicate that the first statement to fail starting will
     * fail the complete module deployment, or set to false to indicate that the operation should attempt
     * to start all statements regardless of any failures.
     *
     * @return indicator
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Set to true (the default) to indicate that the first statement to fail starting will
     * fail the complete module deployment, or set to false to indicate that the operation should attempt
     * to start all statements regardless of any failures.
     *
     * @param failFast indicator
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Returns true (the default) to indicate that the engine destroys any started statement when
     * a subsequent statement fails to start, or false if the engine should leave any started statement
     * as-is even when exceptions occur for one or more statements.
     *
     * @return indicator
     */
    public boolean isRollbackOnFail() {
        return rollbackOnFail;
    }

    /**
     * Set this indicator to true (the default) to indicate that the engine destroys any started statement when
     * a subsequent statement fails to start, or false if the engine should leave any started statement
     * as-is even when exceptions occur for one or more statements.
     *
     * @param rollbackOnFail indicator
     */
    public void setRollbackOnFail(boolean rollbackOnFail) {
        this.rollbackOnFail = rollbackOnFail;
    }

    /**
     * Returns true to indicate to compile only and not start any statements, or false (the default) to
     * indicate that statements are started as part of the deploy.
     *
     * @return indicator
     */
    public boolean isCompileOnly() {
        return compileOnly;
    }

    /**
     * Set this indicator to true to indicate to compile only and not start any statements, or false (the default) to
     * indicate that statements are started as part of the deploy.
     *
     * @param compileOnly indicator
     */
    public void setCompileOnly(boolean compileOnly) {
        this.compileOnly = compileOnly;
    }

    /**
     * Returns the isolated service provider to deploy to, if specified.
     *
     * @return isolated service provider name
     */
    public String getIsolatedServiceProvider() {
        return isolatedServiceProvider;
    }

    /**
     * Sets the isolated service provider to deploy to, if specified the deployment occurs to the provider indicated.
     *
     * @param name isolated service provider name
     */
    public void setIsolatedServiceProvider(String name) {
        this.isolatedServiceProvider = name;
    }

    /**
     * Returns true to validate the module syntax and EPL syntax only. Use this option to
     * not deploy any EPL statement, performing only syntax checking.
     *
     * @return validate flag
     */
    public boolean isValidateOnly() {
        return validateOnly;
    }

    /**
     * Set to true to validate the module syntax and EPL syntax only. Use this option to
     * not deploy any EPL statement, performing only syntax checking.
     *
     * @param validateOnly validate flag
     */
    public void setValidateOnly(boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    /**
     * Returns the statement name resolver.
     *
     * @return statement name resolver
     */
    public StatementNameResolver getStatementNameResolver() {
        return statementNameResolver;
    }

    /**
     * Sets the statement name resolver.
     *
     * @param statementNameResolver name resolver
     */
    public void setStatementNameResolver(StatementNameResolver statementNameResolver) {
        this.statementNameResolver = statementNameResolver;
    }

    /**
     * Returns the statement user object resolver.
     *
     * @return statement user object resolver
     */
    public StatementUserObjectResolver getStatementUserObjectResolver() {
        return statementUserObjectResolver;
    }

    /**
     * Sets the statement user object resolver.
     *
     * @param statementUserObjectResolver statement user object resolver
     */
    public void setStatementUserObjectResolver(StatementUserObjectResolver statementUserObjectResolver) {
        this.statementUserObjectResolver = statementUserObjectResolver;
    }

    /**
     * Return the deployment lock strategy, the default is {@link DeploymentLockStrategyDefault}
     * @return lock strategy
     */
    public DeploymentLockStrategy getDeploymentLockStrategy() {
        return deploymentLockStrategy;
    }

    /**
     * Sets the deployment lock strategy, the default is {@link DeploymentLockStrategyDefault}
     * @param deploymentLockStrategy lock strategy
     */
    public void setDeploymentLockStrategy(DeploymentLockStrategy deploymentLockStrategy) {
        this.deploymentLockStrategy = deploymentLockStrategy;
    }
}
