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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompiledManifest;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.UndeployPreconditionException;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.DependencyGraph;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.util.RuntimeVersion;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;
import com.espertech.esper.runtime.internal.deploymentlifesvc.StatementIdRecoveryService;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.espertech.esper.common.client.util.UndeployRethrowPolicy.RETHROW_FIRST;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperDependencies.getDependenciesConsumed;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperDependencies.getDependenciesProvided;

public class EPDeploymentServiceImpl implements EPDeploymentServiceSPI {
    private static final Logger log = LoggerFactory.getLogger(EPDeploymentServiceImpl.class);

    private final EPServicesContext services;
    private final EPRuntimeSPI runtime;

    EPDeploymentServiceImpl(EPServicesContext services, EPRuntimeSPI runtime) {
        this.services = services;
        this.runtime = runtime;
    }

    public EPDeploymentRollout rollout(Collection<EPDeploymentRolloutCompiled> items) throws EPDeployException {
        return rollout(items, new RolloutOptions());
    }

    public EPDeploymentRollout rollout(Collection<EPDeploymentRolloutCompiled> items, RolloutOptions options) throws EPDeployException {
        if (options == null) {
            options = new RolloutOptions();
        }
        validateRuntimeAlive();
        int rollItemNum = 0;
        for (EPDeploymentRolloutCompiled item : items) {
            checkManifest(rollItemNum++, item.getCompiled().getManifest());
        }

        try {
            options.getRolloutLockStrategy().acquire(services.getEventProcessingRWLock());
        } catch (Exception e) {
            throw new EPDeployLockException(e.getMessage(), e);
        }

        DeployerRolloutDeploymentResult rolloutResult;
        try {
            StatementIdRecoveryService statementIdRecovery = services.getEpServicesHA().getStatementIdRecoveryService();
            Integer currentStatementId = statementIdRecovery.getCurrentStatementId();
            if (currentStatementId == null) {
                currentStatementId = 1;
            }

            rolloutResult = DeployerRollout.rollout(currentStatementId, items, runtime);
            statementIdRecovery.setCurrentStatementId(currentStatementId + rolloutResult.getNumStatements());

            // dispatch event
            for (int i = 0; i < rolloutResult.getDeployments().length; i++) {
                dispatchOnDeploymentEvent(rolloutResult.getDeployments()[i], i);
            }
        } finally {
            options.getRolloutLockStrategy().release(services.getEventProcessingRWLock());
        }

        EPDeploymentRolloutItem[] deployments = new EPDeploymentRolloutItem[items.size()];
        for (int i = 0; i < rolloutResult.getDeployments().length; i++) {
            EPDeployment deployment = makeDeployment(rolloutResult.getDeployments()[i]);
            deployments[i] = new EPDeploymentRolloutItem(deployment);
        }
        return new EPDeploymentRollout(deployments);
    }

    public EPDeployment deploy(EPCompiled compiled) throws EPDeployException {
        return deploy(compiled, new DeploymentOptions());
    }

    public EPDeployment deploy(EPCompiled compiled, DeploymentOptions options) throws EPDeployException {
        if (options == null) {
            options = new DeploymentOptions();
        }
        validateRuntimeAlive();
        checkManifest(-1, compiled.getManifest());

        try {
            options.getDeploymentLockStrategy().acquire(services.getEventProcessingRWLock());
        } catch (Exception e) {
            throw new EPDeployLockException(e.getMessage(), e);
        }

        DeploymentInternal deployerResult;
        try {
            StatementIdRecoveryService statementIdRecovery = services.getEpServicesHA().getStatementIdRecoveryService();
            Integer currentStatementId = statementIdRecovery.getCurrentStatementId();
            if (currentStatementId == null) {
                currentStatementId = 1;
            }

            String deploymentId = DeployerHelperResolver.determineDeploymentIdCheckExists(-1, options, runtime.getServicesContext().getDeploymentLifecycleService());
            deployerResult = Deployer.deployFresh(deploymentId, currentStatementId, compiled, options.getStatementNameRuntime(), options.getStatementUserObjectRuntime(), options.getStatementSubstitutionParameter(), options.getDeploymentClassLoaderOption(), runtime);
            statementIdRecovery.setCurrentStatementId(currentStatementId + deployerResult.getStatements().length);

            // dispatch event
            dispatchOnDeploymentEvent(deployerResult, -1);
        } finally {
            options.getDeploymentLockStrategy().release(services.getEventProcessingRWLock());
        }

        return makeDeployment(deployerResult);
    }

    public EPStatement getStatement(String deploymentId, String statementName) {
        if (deploymentId == null) {
            throw new IllegalArgumentException("Missing deployment-id parameter");
        }
        if (statementName == null) {
            throw new IllegalArgumentException("Missing statement-name parameter");
        }
        return services.getDeploymentLifecycleService().getStatementByName(deploymentId, statementName);
    }

    public String[] getDeployments() {
        return services.getDeploymentLifecycleService().getDeploymentIds();
    }

    public Map<String, DeploymentInternal> getDeploymentMap() {
        return services.getDeploymentLifecycleService().getDeploymentMap();
    }

    public EPDeployment getDeployment(String deploymentId) {
        return EPDeploymentServiceUtil.toDeployment(services.getDeploymentLifecycleService(), deploymentId);
    }

    public boolean isDeployed(String deploymentId) {
        return services.getDeploymentLifecycleService().getDeploymentById(deploymentId) != null;
    }

    public void undeployAll() throws EPUndeployException {
        undeployAllInternal(null);
    }

    public void undeployAll(UndeploymentOptions options) throws EPUndeployException {
        undeployAllInternal(options);
    }

    private void undeployAllInternal(UndeploymentOptions options) throws EPUndeployException {
        if (options == null) {
            options = new UndeploymentOptions();
        }
        DeploymentLifecycleService deploymentSvc = services.getDeploymentLifecycleService();
        String[] deployments = services.getDeploymentLifecycleService().getDeploymentIds();
        if (deployments.length == 0) {
            return;
        }
        if (deployments.length == 1) {
            undeploy(deployments[0]);
            return;
        }
        if (deployments.length == 2) {
            DeploymentInternal zero = deploymentSvc.getDeploymentById(deployments[0]);
            String[] zeroDependsOn = zero.getDeploymentIdDependencies();
            if (zeroDependsOn != null && zeroDependsOn.length > 0) {
                undeploy(deployments[0]);
                undeploy(deployments[1]);
            } else {
                undeploy(deployments[1]);
                undeploy(deployments[0]);
            }
            return;
        }

        // build map of deployment-to-index
        Map<String, Integer> deploymentIndexes = new HashMap<>();
        int count = 0;
        for (String deployment : deployments) {
            deploymentIndexes.put(deployment, count++);
        }

        DependencyGraph graph = new DependencyGraph(deployments.length, false);
        for (String deploymentId : deployments) {
            DeploymentInternal deployment = deploymentSvc.getDeploymentById(deploymentId);
            String[] dependentOn = deployment.getDeploymentIdDependencies();
            if (dependentOn == null || dependentOn.length == 0) {
                continue;
            }
            for (String target : dependentOn) {
                int fromIndex = deploymentIndexes.get(deploymentId);
                int targetIndex = deploymentIndexes.get(target);
                graph.addDependency(targetIndex, fromIndex);
            }
        }
        Set<String> undeployed = new HashSet<>();
        for (int rootIndex : graph.getRootNodes()) {
            recursiveUndeploy(rootIndex, deployments, graph, undeployed, options);
        }
    }

    private void recursiveUndeploy(int index, String[] deployments, DependencyGraph graph, Set<String> undeployed, UndeploymentOptions options) throws EPUndeployException {
        Collection<Integer> dependencies = graph.getDependenciesForStream(index);
        for (int dependency : dependencies) {
            recursiveUndeploy(dependency, deployments, graph, undeployed, options);
        }

        String next = deployments[index];
        if (!undeployed.add(next)) {
            return;
        }

        undeploy(next, options);
    }

    public void undeploy(String deploymentId) throws EPUndeployException {
        undeployRemoveInternal(deploymentId, null);
    }

    public void undeploy(String deploymentId, UndeploymentOptions options) throws EPUndeployException {
        undeployRemoveInternal(deploymentId, options);
    }

    private void undeployRemoveInternal(String deploymentId, UndeploymentOptions options) throws EPUndeployException {
        DeploymentInternal deployment = services.getDeploymentLifecycleService().getDeploymentById(deploymentId);
        if (deployment == null) {
            String stageUri = services.getStageRecoveryService().deploymentGetStage(deploymentId);
            if (stageUri != null) {
                throw new EPUndeployPreconditionException("Deployment id '" + deploymentId + "' is staged and cannot be undeployed");
            }
            throw new EPUndeployNotFoundException("Deployment id '" + deploymentId + "' cannot be found");
        }
        EPStatement[] statements = deployment.getStatements();

        if (options == null) {
            options = new UndeploymentOptions();
        }

        try {
            options.getUndeploymentLockStrategy().acquire(services.getEventProcessingRWLock());
        } catch (Exception e) {
            throw new EPUndeployLockException(e.getMessage(), e);
        }

        try {
            // build list of statements in reverse order
            StatementContext[] reverted = new StatementContext[statements.length];
            int count = reverted.length - 1;
            for (EPStatement stmt : statements) {
                reverted[count--] = ((EPStatementSPI) stmt).getStatementContext();
            }

            // check module preconditions
            String moduleName = deployment.getModuleProvider().getModuleName();
            Undeployer.checkModulePreconditions(deploymentId, moduleName, deployment, services);

            // check preconditions
            try {
                for (StatementContext statement : reverted) {
                    statement.getStatementAIFactoryProvider().getFactory().statementDestroyPreconditions(statement);
                }
            } catch (UndeployPreconditionException t) {
                throw new EPUndeployException("Precondition not satisfied for undeploy: " + t.getMessage(), t);
            }

            // disassociate statements
            Undeployer.disassociate(statements);

            // undeploy statements
            Throwable undeployException = null;
            try {
                Undeployer.undeploy(deploymentId, deployment.getDeploymentTypes(), reverted, deployment.getModuleProvider(), services);
            } catch (Throwable t) {
                log.error("Exception encountered during undeploy: " + t.getMessage(), t);
                undeployException = t;
            }

            // remove deployment
            services.getEpServicesHA().getDeploymentRecoveryService().remove(deploymentId);
            services.getDeploymentLifecycleService().removeDeployment(deploymentId);

            dispatchOnUndeploymentEvent(deployment, -1);

            // rethrow exception if configured
            if (undeployException != null && services.getConfigSnapshot().getRuntime().getExceptionHandling().getUndeployRethrowPolicy() == RETHROW_FIRST) {
                throw new EPUndeployException("Undeploy completed with an exception: " + undeployException.getMessage(), undeployException);
            }

            ((EPEventServiceSPI) runtime.getEventService()).clearCaches();
        } finally {
            options.getUndeploymentLockStrategy().release(services.getEventProcessingRWLock());
        }
    }

    public void destroy() {
    }

    public void addDeploymentStateListener(DeploymentStateListener listener) {
        services.getDeploymentLifecycleService().getListeners().add(listener);
    }

    public void removeDeploymentStateListener(DeploymentStateListener listener) {
        services.getDeploymentLifecycleService().getListeners().remove(listener);
    }

    public Iterator<DeploymentStateListener> getDeploymentStateListeners() {
        return services.getDeploymentLifecycleService().getListeners().iterator();
    }

    public void removeAllDeploymentStateListeners() {
        services.getDeploymentLifecycleService().getListeners().clear();
    }

    public EPDeploymentDependencyProvided getDeploymentDependenciesProvided(String selfDeploymentId) {
        if (selfDeploymentId == null) {
            throw new IllegalArgumentException("deployment-id is null");
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            return getDependenciesProvided(selfDeploymentId, services, services.getDeploymentLifecycleService());
        } finally {
            services.getEventProcessingRWLock().releaseReadLock();
        }
    }

    public EPDeploymentDependencyConsumed getDeploymentDependenciesConsumed(String selfDeploymentId) {
        if (selfDeploymentId == null) {
            throw new IllegalArgumentException("deployment-id is null");
        }
        services.getEventProcessingRWLock().acquireReadLock();
        try {
            return getDependenciesConsumed(selfDeploymentId, services, services.getDeploymentLifecycleService());
        } finally {
            services.getEventProcessingRWLock().releaseReadLock();
        }
    }

    private void dispatchOnDeploymentEvent(DeploymentInternal deployed, int rolloutItemNumber) {
        CopyOnWriteArrayList<DeploymentStateListener> listeners = services.getDeploymentLifecycleService().getListeners();
        if (listeners.isEmpty()) {
            return;
        }
        EPStatement[] stmts = deployed.getStatements();
        DeploymentStateEventDeployed event = new DeploymentStateEventDeployed(services.getRuntimeURI(),
            deployed.getDeploymentId(), deployed.getModuleProvider().getModuleName(), stmts, rolloutItemNumber);
        for (DeploymentStateListener listener : listeners) {
            try {
                listener.onDeployment(event);
            } catch (Throwable t) {
                handleDeploymentEventListenerException("on-deployment", t);
            }
        }
    }

    private void dispatchOnUndeploymentEvent(DeploymentInternal result, int rolloutItemNumber) {
        CopyOnWriteArrayList<DeploymentStateListener> listeners = services.getDeploymentLifecycleService().getListeners();
        if (listeners.isEmpty()) {
            return;
        }
        EPStatement[] statements = result.getStatements();
        DeploymentStateEventUndeployed event = new DeploymentStateEventUndeployed(services.getRuntimeURI(),
            result.getDeploymentId(), result.getModuleProvider().getModuleName(), statements, rolloutItemNumber);
        for (DeploymentStateListener listener : listeners) {
            try {
                listener.onUndeployment(event);
            } catch (Throwable t) {
                handleDeploymentEventListenerException("on-undeployment", t);
            }
        }
    }

    private void handleDeploymentEventListenerException(String typeOfOperation, Throwable t) {
        log.error("Application-provided deployment state listener reported an exception upon receiving the " + typeOfOperation + " event, logging and ignoring the exception, detail: " + t.getMessage(), t);
    }

    private void validateRuntimeAlive() {
        if (runtime.isDestroyed()) {
            throw new EPRuntimeDestroyedException(runtime.getURI());
        }
    }

    private void checkManifest(int rolloutItemNumber, EPCompiledManifest manifest) throws EPDeployException {
        try {
            RuntimeVersion.checkVersion(manifest.getCompilerVersion());
        } catch (RuntimeVersion.VersionException ex) {
            throw new EPDeployDeploymentVersionException(ex.getMessage(), ex, rolloutItemNumber);
        }

        if (manifest.getModuleProviderClassName() == null) {
            if (manifest.getQueryProviderClassName() != null) {
                throw new EPDeployException("Cannot deploy EPL that was compiled as a fire-and-forget query, make sure to use the 'compile' method of the compiler", rolloutItemNumber);
            }
            throw new EPDeployException("Failed to find module provider class name in manifest (is this a compiled module?)", rolloutItemNumber);
        }

        try {
            services.getEventSerdeFactory().verifyHADeployment(manifest.isTargetHA());
        } catch (ExprValidationException ex) {
            throw new EPDeployException(ex.getMessage(), ex, rolloutItemNumber);
        }
    }

    private EPDeployment makeDeployment(DeploymentInternal deployerResult) {
        EPStatement[] copy = new EPStatement[deployerResult.getStatements().length];
        System.arraycopy(deployerResult.getStatements(), 0, copy, 0, deployerResult.getStatements().length);
        return new EPDeployment(deployerResult.getDeploymentId(), deployerResult.getModuleProvider().getModuleName(), deployerResult.getModulePropertiesCached(), copy, CollectionUtil.copyArray(deployerResult.getDeploymentIdDependencies()), new Date(System.currentTimeMillis()));
    }
}
