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

import com.espertech.esper.common.client.EPCompiled;

import java.util.Iterator;

/**
 * Service for deploying and undeploying modules and obtaining information about current deployments and statements.
 */
public interface EPDeploymentService {
    /**
     * Deploy a compiled module and with the default options.
     *
     * @param compiled byte code
     * @return deployment
     * @throws EPDeployException when the deployment failed
     */
    EPDeployment deploy(EPCompiled compiled) throws EPDeployException;

    /**
     * Deploy a compiled module and with the provided options.
     *
     * @param compiled byte code
     * @param options  deployment options
     * @return deployment
     * @throws EPDeployException when the deployment failed
     */
    EPDeployment deploy(EPCompiled compiled, DeploymentOptions options) throws EPDeployException;

    /**
     * Returns the deployment ids of all deployments.
     *
     * @return deployment ids
     */
    String[] getDeployments();

    /**
     * Undeploy a deployment and with the default options.
     *
     * @param deploymentId of the deployment to undeploy
     * @throws EPUndeployException when the deployment does not exist or the undeployment failed and the deployment remains deployed
     */
    void undeploy(String deploymentId) throws EPUndeployException;

    /**
     * Undeploy a deployment and with the provided options
     *
     * @param deploymentId of the deployment to undeploy
     * @param options      undeployment options
     * @throws EPUndeployException when the deployment does not exist or the undeployment failed and the deployment remains deployed
     */
    void undeploy(String deploymentId, UndeploymentOptions options) throws EPUndeployException;

    /**
     * Undeploy all deployments and with the default options.
     *
     * @throws EPUndeployException when the undeployment failed, of the deployments may remain deployed
     */
    void undeployAll() throws EPUndeployException;

    /**
     * Undeploy all deployments and with the provided options.
     *
     * @param options undeployment options or null if none provided
     * @throws EPUndeployException when the undeployment failed, of the deployments may remain deployed
     */
    void undeployAll(UndeploymentOptions options) throws EPUndeployException;

    /**
     * Returns the statement of a given deployment.
     * <p>
     * A statement is uniquely identified by the deployment id that deployed the statement
     * and by the statement name.
     * </p>
     *
     * @param deploymentId  deployment id of the statement
     * @param statementName statement name
     * @return statement or null if the statement could not be found
     */
    EPStatement getStatement(String deploymentId, String statementName);

    /**
     * Returns the deployment.
     * <p>
     * A deployment is uniquely identified by its deployment id.
     * </p>
     *
     * @param deploymentId the deployment id of the deployment
     * @return deployment or null if the deployment could not be found
     */
    EPDeployment getDeployment(String deploymentId);

    /**
     * Add a deployment state listener
     *
     * @param listener to add
     */
    void addDeploymentStateListener(DeploymentStateListener listener);

    /**
     * Remove a deployment state listener
     *
     * @param listener to remove
     */
    void removeDeploymentStateListener(DeploymentStateListener listener);

    /**
     * Returns an iterator of deployment state listeners (read-only)
     *
     * @return listeners
     */
    Iterator<DeploymentStateListener> getDeploymentStateListeners();

    /**
     * Removes all deployment state listener
     */
    void removeAllDeploymentStateListeners();

    /**
     * Returns indicator whether a deployment for this deployment id exists
     * @param deploymentId deployment id
     * @return true for deployed, false for not deployed
     */
    boolean isDeployed(String deploymentId);
}
