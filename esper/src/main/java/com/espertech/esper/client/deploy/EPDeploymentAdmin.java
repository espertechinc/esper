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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 * Service to package and deploy EPL statements organized into an EPL module.
 */
public interface EPDeploymentAdmin {
    /**
     * Read the input stream and return the module. It is up to the calling method to close the stream when done.
     *
     * @param stream    to read
     * @param moduleUri uri of the module
     * @return module
     * @throws IOException    when the io operation failed
     * @throws ParseException when parsing of the module failed
     */
    public Module read(InputStream stream, String moduleUri) throws IOException, ParseException;

    /**
     * Read the resource by opening from classpath and return the module.
     *
     * @param resource name of the classpath resource
     * @return module
     * @throws IOException    when the resource could not be read
     * @throws ParseException when parsing of the module failed
     */
    public Module read(String resource) throws IOException, ParseException;

    /**
     * Read the module by reading the text file and return the module.
     *
     * @param file the file to read
     * @return module
     * @throws IOException    when the file could not be read
     * @throws ParseException when parsing of the module failed
     */
    public Module read(File file) throws IOException, ParseException;

    /**
     * Read the module by reading from the URL provided and return the module.
     *
     * @param url the URL to read
     * @return module
     * @throws IOException    when the url input stream could not be read
     * @throws ParseException when parsing of the module failed
     */
    public Module read(URL url) throws IOException, ParseException;

    /**
     * Parse the module text passed in, returning the module.
     *
     * @param eplModuleText to parse
     * @return module
     * @throws IOException    when the parser failed to read the string buffer
     * @throws ParseException when parsing of the module failed
     */
    public Module parse(String eplModuleText) throws IOException, ParseException;

    /**
     * Compute a deployment order among the modules passed in considering their uses-dependency declarations
     * and considering the already-deployed modules.
     * <p>
     * The operation also checks and reports circular dependencies.
     * <p>
     * Pass in @{link DeploymentOrderOptions} to customize the behavior if this method. When passing no options
     * or passing default options, the default behavior checks uses-dependencies and circular dependencies.
     *
     * @param modules to determine ordering for
     * @param options operation options or null for default options
     * @return ordered modules
     * @throws DeploymentOrderException when any module dependencies are not satisfied
     */
    public DeploymentOrder getDeploymentOrder(Collection<Module> modules, DeploymentOrderOptions options) throws DeploymentException;

    /**
     * Deploy a single module returning a generated deployment id to use when undeploying statements as well as
     * additional statement-level information.
     * <p>
     * Pass in @{link DeploymentOptions} to customize the behavior. When passing no options or passing default options,
     * the operation first compiles all EPL statements before starting each statement, fails-fast on the first statement that fails to start
     * and rolls back (destroys) any started statement on a failure.
     * <p>
     * When setting validate-only in the deployment options, the method returns a null-value
     * on success.
     *
     * @param module  to deploy
     * @param options operation options or null for default options
     * @return result object with statement detail, or null for pass on validate-only
     * @throws DeploymentActionException when the deployment fails, contains a list of deployment failures
     * @throws DeploymentLockException   to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult deploy(Module module, DeploymentOptions options) throws DeploymentException, InterruptedException;

    /**
     * Deploy a single module using the deployment id provided as a parameter.
     * <p>
     * Pass in @{link DeploymentOptions} to customize the behavior. When passing no options or passing default options,
     * the operation first compiles all EPL statements before starting each statement, fails-fast on the first statement that fails to start
     * and rolls back (destroys) any started statement on a failure.
     * <p>
     * When setting validate-only in the deployment options, the method returns a null-value
     * on success.
     *
     * @param module               to deploy
     * @param options              operation options or null for default options
     * @param assignedDeploymentId the deployment id to assign
     * @return result object with statement detail, or null for pass on validate-only
     * @throws DeploymentActionException when the deployment fails, contains a list of deployment failures
     * @throws DeploymentLockException   to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult deploy(Module module, DeploymentOptions options, String assignedDeploymentId) throws DeploymentActionException, DeploymentLockException, InterruptedException;

    /**
     * Undeploy a single module, if its in deployed state, and removes it from the known modules.
     * <p>
     * This operation destroys all statements previously associated to the deployed module
     * and also removes this module from the list deployments list.
     *
     * @param deploymentId of the deployment to undeploy.
     * @return result object with statement-level detail
     * @throws DeploymentNotFoundException when the deployment id could not be resolved to a deployment
     */
    public UndeploymentResult undeployRemove(String deploymentId) throws DeploymentNotFoundException;

    /**
     * Undeploy a single module, if its in deployed state, and removes it from the known modules.
     * <p>
     * This operation, by default, destroys all statements previously associated to the deployed module
     * and also removes this module from the list deployments list. Use the options object to control
     * whether statements get destroyed.
     *
     * @param deploymentId        of the deployment to undeploy.
     * @param undeploymentOptions for controlling undeployment, can be a null value
     * @return result object with statement-level detail
     * @throws DeploymentNotFoundException when the deployment id could not be resolved to a deployment
     */
    public UndeploymentResult undeployRemove(String deploymentId, UndeploymentOptions undeploymentOptions) throws DeploymentNotFoundException;

    /**
     * Return deployment ids of all currently known modules.
     *
     * @return array of deployment ids
     */
    public String[] getDeployments();

    /**
     * Returns the deployment information for a given deployment.
     *
     * @param deploymentId to return the deployment information for.
     * @return deployment info
     */
    public DeploymentInformation getDeployment(String deploymentId);

    /**
     * Returns deployment information for all known modules.
     *
     * @return deployment information.
     */
    public DeploymentInformation[] getDeploymentInformation();

    /**
     * Determine if a named module is already deployed (in deployed state), returns true if one or more modules of the same
     * name are deployed or false when no module of that name is deployed.
     *
     * @param moduleName to look up
     * @return indicator
     */
    public boolean isDeployed(String moduleName);

    /**
     * Shortcut method to read and deploy a single module from a classpath resource.
     * <p>
     * Uses default options for performing deployment dependency checking and deployment.
     *
     * @param resource      to read
     * @param moduleURI     uri of module to assign or null if not applicable
     * @param moduleArchive archive name of module to assign or null if not applicable
     * @param userObject    user object to assign to module, passed along unused as part of deployment information, or null if not applicable
     * @return deployment result object
     * @throws IOException               when the file could not be read
     * @throws ParseException            when parsing of the module failed
     * @throws DeploymentOrderException  when any module dependencies are not satisfied
     * @throws DeploymentActionException when the deployment fails, contains a list of deployment failures
     * @throws DeploymentLockException   to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult readDeploy(String resource, String moduleURI, String moduleArchive, Object userObject)
            throws IOException, ParseException, DeploymentException, InterruptedException;

    /**
     * Shortcut method to read and deploy a single module from an input stream.
     * <p>
     * Uses default options for performing deployment dependency checking and deployment.
     * <p>
     * Leaves the stream unclosed.
     *
     * @param stream        to read
     * @param moduleURI     uri of module to assign or null if not applicable
     * @param moduleArchive archive name of module to assign or null if not applicable
     * @param userObject    user object to assign to module, passed along unused as part of deployment information, or null if not applicable
     * @return deployment result object
     * @throws IOException               when the file could not be read
     * @throws ParseException            when parsing of the module failed
     * @throws DeploymentOrderException  when any module dependencies are not satisfied
     * @throws DeploymentActionException when the deployment fails, contains a list of deployment failures
     * @throws DeploymentLockException   to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult readDeploy(InputStream stream, String moduleURI, String moduleArchive, Object userObject)
            throws IOException, ParseException, DeploymentException, InterruptedException;

    /**
     * Shortcut method to parse and deploy a single module from a string text buffer.
     * <p>
     * Uses default options for performing deployment dependency checking and deployment.
     *
     * @param eplModuleText to parse
     * @param moduleURI     uri of module to assign or null if not applicable
     * @param moduleArchive archive name of module to assign or null if not applicable
     * @param userObject    user object to assign to module, passed along unused as part of deployment information, or null if not applicable
     * @return deployment result object
     * @throws IOException               when the file could not be read
     * @throws ParseException            when parsing of the module failed
     * @throws DeploymentOrderException  when any module dependencies are not satisfied
     * @throws DeploymentActionException when the deployment fails, contains a list of deployment failures
     * @throws DeploymentLockException   to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult parseDeploy(String eplModuleText, String moduleURI, String moduleArchive, Object userObject)
            throws IOException, ParseException, DeploymentException, InterruptedException;

    /**
     * Shortcut method to parse and deploy a single module from a string text buffer, without providing a module URI name or
     * archive name or user object. The module URI, archive name and user object are defaulted to null.
     * <p>
     * Uses default options for performing deployment dependency checking and deployment.
     *
     * @param eplModuleText to parse
     * @return deployment result object
     * @throws IOException               when the file could not be read
     * @throws ParseException            when parsing of the module failed
     * @throws DeploymentOrderException  when any module dependencies are not satisfied
     * @throws DeploymentActionException when the deployment fails, contains a list of deployment failures
     * @throws DeploymentLockException   to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult parseDeploy(String eplModuleText)
            throws IOException, ParseException, DeploymentException, InterruptedException;

    /**
     * Adds a module in undeployed state, generating a deployment id and returning the generated deployment id of the module.
     *
     * @param module to add
     * @return The deployment id assigned to the module
     */
    public String add(Module module);

    /**
     * Adds a module in undeployed state, using the provided deployment id as a unique identifier for the module.
     *
     * @param module               to add
     * @param assignedDeploymentId deployment id to assign
     */
    public void add(Module module, String assignedDeploymentId);

    /**
     * Remove a module that is currently in undeployed state.
     * <p>
     * This call may only be used on undeployed modules.
     *
     * @param deploymentId of the module to remove
     * @throws DeploymentStateException    when attempting to remove a module that does not exist or a module that is not in undeployed state
     * @throws DeploymentNotFoundException if no such deployment id is known
     */
    public void remove(String deploymentId) throws DeploymentException;

    /**
     * Deploy a previously undeployed module.
     *
     * @param deploymentId of the module to deploy
     * @param options      deployment options
     * @return deployment result
     * @throws DeploymentStateException    when attempting to deploy a module that does not exist is already deployed
     * @throws DeploymentOrderException    when deployment dependencies are not satisfied
     * @throws DeploymentActionException   when the deployment (or validation when setting validate-only) failed
     * @throws DeploymentNotFoundException if no such deployment id is known
     * @throws DeploymentLockException     to indicate a problem obtaining the necessary lock
     * @throws InterruptedException when lock-taking was interrupted
     */
    public DeploymentResult deploy(String deploymentId, DeploymentOptions options) throws DeploymentException, InterruptedException;

    /**
     * Undeploy a previously deployed module.
     *
     * @param deploymentId of the module to undeploy
     * @return undeployment result
     * @throws DeploymentStateException    when attempting to undeploy a module that does not exist is already undeployed
     * @throws DeploymentNotFoundException when the deployment id could not be resolved
     * @throws InterruptedException when lock-taking was interrupted
     */
    public UndeploymentResult undeploy(String deploymentId) throws DeploymentException, InterruptedException;

    /**
     * Undeploy a previously deployed module.
     *
     * @param deploymentId        of the module to undeploy
     * @param undeploymentOptions undeployment options, or null for default behavior
     * @return undeployment result
     * @throws DeploymentStateException    when attempting to undeploy a module that does not exist is already undeployed
     * @throws DeploymentNotFoundException when the deployment id could not be resolved
     * @throws InterruptedException when lock-taking was interrupted
     */
    public UndeploymentResult undeploy(String deploymentId, UndeploymentOptions undeploymentOptions) throws DeploymentException, InterruptedException;
}
