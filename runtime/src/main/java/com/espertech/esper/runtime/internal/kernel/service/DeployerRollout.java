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

import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesRuntime;
import com.espertech.esper.common.internal.context.module.ModuleProviderCLPair;
import com.espertech.esper.common.internal.context.module.ModuleProviderUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.runtime.client.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.espertech.esper.runtime.internal.kernel.service.Deployer.addPathDependencies;
import static com.espertech.esper.runtime.internal.kernel.service.Deployer.getRecoveryInformation;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperInitStatement.initializeStatements;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperInitializeEPLObjects.initializeEPLObjects;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperInitializeEPLObjects.validateStagedEPLObjects;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperResolver.resolveDependencies;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperUpdatePath.updatePath;

public class DeployerRollout {
    public static DeployerRolloutDeploymentResult rollout(int currentStatementId, Collection<EPDeploymentRolloutCompiled> itemsProvided, EPRuntimeSPI runtime) throws EPDeployException {
        EPDeploymentRolloutCompiled[] items = itemsProvided.toArray(new EPDeploymentRolloutCompiled[0]);

        // per-deployment: determine deployment id
        String[] deploymentIds = new String[items.length];
        Set<String> deploymentIdSet = new HashSet<>(CollectionUtil.capacityHashMap(deploymentIds.length));
        for (int i = 0; i < items.length; i++) {
            deploymentIds[i] = DeployerHelperResolver.determineDeploymentIdCheckExists(i, items[i].getOptions(), runtime.getServicesContext().getDeploymentLifecycleService());
            if (!deploymentIdSet.add(deploymentIds[i])) {
                throw new EPDeployException("Deployment id '" + deploymentIds[i] + "' occurs multiple times in the rollout", i);
            }
        }

        // per-deployment: obtain module providers
        ModuleProviderCLPair[] moduleProviders = new ModuleProviderCLPair[items.length];
        for (int i = 0; i < items.length; i++) {
            ClassLoader classLoader = DeployerHelperResolver.getClassLoader(i, items[i].getOptions().getDeploymentClassLoaderOption(), runtime.getServicesContext());
            try {
                moduleProviders[i] = ModuleProviderUtil.analyze(items[i].getCompiled(), classLoader);
            } catch (Throwable t) {
                rolloutCleanClassloader(deploymentIds, runtime.getServicesContext());
            }
        }

        // per-deployment: check dependencies and initialize EPL objects
        DeployerRolloutInitResult[] inits = new DeployerRolloutInitResult[items.length];
        for (int i = 0; i < items.length; i++) {
            try {
                inits[i] = resolveDependenciesInitEPLObjects(i, deploymentIds[i], moduleProviders[i], runtime.getServicesContext(), runtime.getStageService());
            } catch (EPDeployException ex) {
                rolloutCleanPathAndTypes(inits, deploymentIds, runtime.getServicesContext());
                throw ex;
            } catch (Throwable t) {
                rolloutCleanPathAndTypes(inits, deploymentIds, runtime.getServicesContext());
                throw new EPDeployException(t.getMessage(), t, i);
            }
        }

        // per-deployment - obtain statement lightweights
        DeployerModuleStatementLightweights[] stmtLightweights = new DeployerModuleStatementLightweights[items.length];
        int numStatements = 0;
        for (int i = 0; i < items.length; i++) {
            int statementIdStart = currentStatementId + numStatements;
            try {
                stmtLightweights[i] = initializeStatements(i, false, inits[i].getModuleEPLObjects(), inits[i].getModulePaths(), inits[i].getModuleName(), moduleProviders[i], deploymentIds[i], statementIdStart,
                    items[i].getOptions().getStatementUserObjectRuntime(), items[i].getOptions().getStatementNameRuntime(), items[i].getOptions().getStatementSubstitutionParameter(), runtime.getServicesContext());
            } catch (EPDeployException ex) {
                rolloutCleanLightweights(stmtLightweights, inits, deploymentIds, moduleProviders, runtime.getServicesContext());
                throw ex;
            } catch (Throwable t) {
                rolloutCleanLightweights(stmtLightweights, inits, deploymentIds, moduleProviders, runtime.getServicesContext());
                throw new EPDeployException(t.getMessage(), t, i);
            }
            numStatements += stmtLightweights[i].getLightweights().size();
        }

        // per-deployment: start statements depending on context association
        EPStatement[][] statements = new EPStatement[items.length][];
        for (int i = 0; i < items.length; i++) {
            try {
                statements[i] = DeployerHelperStatement.deployStatements(i, stmtLightweights[i].getLightweights(), false, inits[i].getModulePaths(), moduleProviders[i], deploymentIds[i], runtime);
            } catch (EPDeployException ex) {
                rolloutCleanStatements(statements, stmtLightweights, inits, deploymentIds, moduleProviders, runtime.getServicesContext());
                throw ex;
            } catch (Throwable t) {
                rolloutCleanStatements(statements, stmtLightweights, inits, deploymentIds, moduleProviders, runtime.getServicesContext());
                throw new EPDeployException(t.getMessage(), t, i);
            }
        }

        // per-deployment: add paths dependency information and add deployment
        DeploymentInternal[] deployments = new DeploymentInternal[items.length];
        for (int i = 0; i < items.length; i++) {
            try {
                // add dependencies
                addPathDependencies(deploymentIds[i], moduleProviders[i].getModuleProvider().getModuleDependencies(), runtime.getServicesContext());

                // keep statement and deployment
                deployments[i] = DeploymentInternal.from(deploymentIds[i], statements[i], inits[i].getDeploymentIdDependencies(), inits[i].getModulePaths(), inits[i].getModuleEPLObjects(), moduleProviders[i]);
                runtime.getServicesContext().getDeploymentLifecycleService().addDeployment(deploymentIds[i], deployments[i]);

                // register for recovery
                DeploymentRecoveryInformation recoveryInformation = getRecoveryInformation(deployments[i]);
                runtime.getServicesContext().getDeploymentRecoveryService().add(deploymentIds[i], stmtLightweights[i].getStatementIdFirstStatement(), items[i].getCompiled(), recoveryInformation.getStatementUserObjectsRuntime(), recoveryInformation.getStatementNamesWhenProvidedByAPI(), stmtLightweights[i].getSubstitutionParameters());
            } catch (Throwable t) {
                rolloutCleanStatements(statements, stmtLightweights, inits, deploymentIds, moduleProviders, runtime.getServicesContext());
                throw new EPDeployException(t.getMessage(), t, i);
            }
        }

        return new DeployerRolloutDeploymentResult(numStatements, deployments);
    }

    private static void rolloutCleanClassloader(String[] deploymentIds, EPServicesContext services) {
        for (int i = 0; i < deploymentIds.length; i++) {
            services.getClassLoaderParent().remove(deploymentIds[i]);
        }
    }

    private static void rolloutCleanPathAndTypes(DeployerRolloutInitResult[] inits, String[] deploymentIds, EPServicesContext services) {
        rolloutCleanClassloader(deploymentIds, services);

        for (int i = 0; i < inits.length; i++) {
            Undeployer.deleteFromPathRegistries(services, deploymentIds[i]);
            if (inits[i] != null) {
                Undeployer.deleteFromEventTypeBus(services, inits[i].getModulePaths().getDeploymentTypes());
            }
        }
    }

    private static void rolloutCleanLightweights(DeployerModuleStatementLightweights[] stmtLightweights, DeployerRolloutInitResult[] inits, String[] deploymentIds, ModuleProviderCLPair[] moduleProviders, EPServicesContext services) {
        for (int i = stmtLightweights.length - 1; i >= 0; i--) {
            if (stmtLightweights[i] != null) {
                DeployerHelperResolver.reverseDeployment(deploymentIds[i], inits[i].getModulePaths().getDeploymentTypes(), stmtLightweights[i].getLightweights(), new EPStatement[0], moduleProviders[i], services);
                inits[i] = null;
            }
        }
        rolloutCleanPathAndTypes(inits, deploymentIds, services);
    }

    private static void rolloutCleanStatements(EPStatement[][] statements, DeployerModuleStatementLightweights[] stmtLightweights, DeployerRolloutInitResult[] inits, String[] deploymentIds, ModuleProviderCLPair[] moduleProviders, EPServicesContext services) {
        for (int i = statements.length - 1; i >= 0; i--) {
            if (statements[i] != null) {
                DeployerHelperResolver.reverseDeployment(deploymentIds[i], inits[i].getModulePaths().getDeploymentTypes(), stmtLightweights[i].getLightweights(), statements[i], moduleProviders[i], services);
                stmtLightweights[i] = null;
                inits[i] = null;
            }
        }
        rolloutCleanLightweights(stmtLightweights, inits, deploymentIds, moduleProviders, services);
    }

    private static DeployerRolloutInitResult resolveDependenciesInitEPLObjects(int rolloutItemNumber, String deploymentId, ModuleProviderCLPair moduleProvider, EPServicesContext services, EPStageService stageService) throws EPDeployPreconditionException, PathException {
        ModuleDependenciesRuntime moduleDependencies = moduleProvider.getModuleProvider().getModuleDependencies();
        Set<String> deploymentIdDependencies = resolveDependencies(rolloutItemNumber, moduleDependencies, services);

        // initialize EPL objects defined by module
        DeployerModuleEPLObjects moduleEPLObjects = initializeEPLObjects(moduleProvider, deploymentId, services);

        // determine staged EPL object overlap
        validateStagedEPLObjects(moduleEPLObjects, moduleProvider.getModuleProvider().getModuleName(), rolloutItemNumber, stageService);

        // add EPL objects defined by module to path
        String moduleName = moduleProvider.getModuleProvider().getModuleName();
        DeployerModulePaths modulePaths = updatePath(rolloutItemNumber, moduleEPLObjects, moduleName, deploymentId, services);

        return new DeployerRolloutInitResult(deploymentIdDependencies, moduleEPLObjects, modulePaths, moduleName);
    }
}
